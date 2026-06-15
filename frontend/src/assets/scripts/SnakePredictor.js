import * as ort from 'onnxruntime-web';

const N = 10;
const FEATURES = 29;
const LATENCY_THRESHOLD_MS = 100;

const DR = [-1, 0, 1, 0];
const DC = [0, 1, 0, -1];

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

    // Called every tick after receiving a "move" event from the server.
    // direction  : direction the local snake moved this tick (0=up,1=right,2=down,3=left)
    // snake      : local player's Snake object
    // oppSnake   : opponent's Snake object
    // gamemap    : 2-D grid from store (1=wall, 2=apple, 0=empty)
    // appleR/C   : current apple grid position
    // justAte    : whether the local snake ate the apple this tick
    addFrame(direction, snake, oppSnake, gamemap, appleR, appleC, justAte) {
        const frame = this._buildFrame(direction, snake, oppSnake, gamemap, appleR, appleC, justAte);
        this.window.push(frame);
        if (this.window.length > N) this.window.shift();
    }

    // Returns true when both trigger conditions are met:
    //   1. estimated RTT exceeds threshold
    //   2. snake is in a constrained position after the current move
    // direction, snake, oppSnake, gamemap, rows, cols describe the post-move state.
    shouldTrigger(direction, snake, oppSnake, gamemap, rows, cols) {
        if (!this.session || this.window.length < N) return false;
        if (this.rttMs < LATENCY_THRESHOLD_MS) return false;
        return this._isConstrained(direction, snake, oppSnake, gamemap, rows, cols);
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
        // output shape: [1, 2, 4] flattened → 8 values; t+1 logits are at indices 0-3
        const logits = output.logits.data;
        let best = 0;
        for (let i = 1; i < 4; i++) {
            if (logits[i] > logits[best]) best = i;
        }
        return best;
    }

    // Update the current RTT estimate (milliseconds).
    updateRTT(ms) {
        this.rttMs = ms;  // direct assignment; no EMA needed for injected latency
    }

    // ── private ──────────────────────────────────────────────────────────────

    // Build a 29-element feature vector for the current tick.
    // Head position is computed as cells[0] + direction offset to match training data
    // (where features are recorded after the move has been applied).
    _buildFrame(direction, snake, oppSnake, gamemap, appleR, appleC, justAte) {
        const rows = gamemap.length;
        const cols = gamemap[0].length;

        const headR = snake.cells[0].r + DR[direction];
        const headC = snake.cells[0].c + DC[direction];

        const occupied = new Set();
        for (const cell of snake.cells)    occupied.add(cell.r * cols + cell.c);
        for (const cell of oppSnake.cells) occupied.add(cell.r * cols + cell.c);

        // 5×5 obstacle grid (row-major, g00 = top-left corner)
        const grid = [];
        for (let dr = -2; dr <= 2; dr++) {
            for (let dc = -2; dc <= 2; dc++) {
                const r = headR + dr, c = headC + dc;
                if (r < 0 || r >= rows || c < 0 || c >= cols || gamemap[r][c] === 1) {
                    grid.push(1);
                } else {
                    grid.push(occupied.has(r * cols + c) ? 1 : 0);
                }
            }
        }

        return [
            direction,      // feat 0
            ...grid,        // feat 1-25
            appleR - headR, // feat 26
            appleC - headC, // feat 27
            justAte ? 1 : 0 // feat 28
        ];
    }

    // Count non-reverse blocked directions from the new head position.
    // Returns true if at least one non-reverse direction is blocked (constrained).
    _isConstrained(direction, snake, oppSnake, gamemap, rows, cols) {
        const headR = snake.cells[0].r + DR[direction];
        const headC = snake.cells[0].c + DC[direction];
        const reverse = (direction + 2) % 4;

        const occupied = new Set();
        for (const cell of snake.cells)    occupied.add(cell.r * cols + cell.c);
        for (const cell of oppSnake.cells) occupied.add(cell.r * cols + cell.c);

        for (let d = 0; d < 4; d++) {
            if (d === reverse) continue;
            const nr = headR + DR[d], nc = headC + DC[d];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return true;
            if (gamemap[nr][nc] === 1) return true;
            if (occupied.has(nr * cols + nc)) return true;
        }
        return false;
    }
}
