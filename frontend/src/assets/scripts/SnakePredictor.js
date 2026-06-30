import * as ort from 'onnxruntime-web';

const N = 10;
const FEATURES = 29;
const LATENCY_THRESHOLD_MS = 100;

export class SnakePredictor {
    constructor() {
        this.session = null;
        this.window = [];  // sliding window of N feature vectors
        this.rttMs = 0;
    }

    async init() {
        this.session = await ort.InferenceSession.create('/models/model.onnx');
        console.log('[SnakePredictor] model loaded');
    }

    // Called every tick after receiving a "move" event from the server. All
    // positions describe the POST-move (server-confirmed) state, so they match
    // the training data exactly (built by the backend after applying each move).
    // direction : direction the subject snake moved this tick (0=up,1=right,2=down,3=left)
    // headR/C   : subject snake head position after the move
    // occupied  : Set of r*cols+c for every body cell of both snakes after the move
    // gamemap   : 2-D grid from store (1=wall, 2=apple, 0=empty)
    // appleR/C  : current apple grid position
    // justAte   : whether the subject snake ate the apple this tick
    addFrame(direction, headR, headC, occupied, gamemap, appleR, appleC, justAte) {
        const frame = this._buildFrame(direction, headR, headC, occupied, gamemap, appleR, appleC, justAte);
        this.window.push(frame);
        if (this.window.length > N) this.window.shift();
    }

    // Lightweight readiness gate (no positional constraint): model loaded,
    // window full, and estimated latency above threshold. Used when predicting
    // both snakes every refill cycle for continuous latency compensation.
    canPredict() {
        return !!this.session && this.window.length >= N && this.rttMs >= LATENCY_THRESHOLD_MS;
    }

    // Readiness ignoring latency: model loaded and window full. Used by the
    // clock-driven (route-B) path, where the trigger is an overdue/late move
    // rather than a measured RTT threshold, so a spike is masked even when the
    // baseline RTT sits below LATENCY_THRESHOLD_MS.
    canPredictByHistory() {
        return !!this.session && this.window.length >= N;
    }

    // Run inference. Returns the predicted direction (0-3) for t+1, or null if not ready.
    async predict() {
        if (!this.session || this.window.length < N) return null;

        const flat = new Float32Array(N * FEATURES);
        for (let i = 0; i < N; i++) {
            for (let j = 0; j < FEATURES; j++) {
                flat[i * FEATURES + j] = this.window[i][j];
            }
        }

        const tensor = new ort.Tensor('float32', flat, [1, N, FEATURES]);
        const output = await this.session.run({ input: tensor });
        // output shape: [1, 2, 4] flattened → 8 values
        // t+1 logits: indices 0-3   t+2 logits: indices 4-7
        const logits = output.logits.data;
        let best1 = 0;
        for (let i = 1; i < 4; i++) {
            if (logits[i] > logits[best1]) best1 = i;
        }
        let best2 = 0;
        for (let i = 1; i < 4; i++) {
            if (logits[4 + i] > logits[4 + best2]) best2 = i;
        }
        return [best1, best2];
    }

    // Update the current RTT estimate (milliseconds).
    updateRTT(ms) {
        this.rttMs = ms;  // direct assignment; no EMA needed for injected latency
    }

    reset() {
        this.window = [];
        this.rttMs  = 0;
    }

    // ── private ──────────────────────────────────────────────────────────────

    // Build a 29-element feature vector from the post-move state. Mirrors the
    // backend's computeGrid: 5×5 obstacle grid centred on the head (head cell
    // forced to 0), then normalised apple direction and the just-ate flag.
    _buildFrame(direction, headR, headC, occupied, gamemap, appleR, appleC, justAte) {
        const rows = gamemap.length;
        const cols = gamemap[0].length;

        // 5×5 obstacle grid (row-major, g00 = top-left corner)
        const grid = [];
        for (let dr = -2; dr <= 2; dr++) {
            for (let dc = -2; dc <= 2; dc++) {
                if (dr === 0 && dc === 0) { grid.push(0); continue; } // head centre
                const r = headR + dr, c = headC + dc;
                if (r < 0 || r >= rows || c < 0 || c >= cols || gamemap[r][c] === 1) {
                    grid.push(1);
                } else {
                    grid.push(occupied.has(r * cols + c) ? 1 : 0);
                }
            }
        }

        // Apple direction is normalised by max(rows, cols) to match the training
        // data (backend Game.collectTelemetry uses the same norm).
        const norm = Math.max(rows, cols);
        return [
            direction,               // feat 0
            ...grid,                 // feat 1-25
            (appleR - headR) / norm, // feat 26
            (appleC - headC) / norm, // feat 27
            justAte ? 1 : 0          // feat 28
        ];
    }
}
