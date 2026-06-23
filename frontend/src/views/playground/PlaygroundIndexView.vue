<template>
    <div class="dashboard">
        <div class="game-area">
            <PlayGround v-if="$store.state.playground.status === 'playing'" />
            <MatchGround v-else />
            <ResultBoard v-if="$store.state.playground.loser !== 'none'" />
        </div>
        <aside class="sidebar">
            <LatencyDisplay  :rtt="rttMs" :injected="injectedMs" />
            <LatencyInjector v-model="injectedMs" />
            <PredictionLog   :entries="logEntries" />
            <SyncMonitor     :entries="syncEntries" />
        </aside>
    </div>
</template>

<script>
import PlayGround     from '@/components/PlayGround.vue';
import MatchGround    from '@/components/MatchGround.vue';
import ResultBoard    from '@/components/ResultBoard.vue';
import LatencyDisplay  from '@/components/LatencyDisplay.vue';
import LatencyInjector from '@/components/LatencyInjector.vue';
import PredictionLog   from '@/components/PredictionLog.vue';
import SyncMonitor     from '@/components/SyncMonitor.vue';
import { SnakePredictor } from '@/assets/scripts/SnakePredictor';
import { onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex';

const MAX_LOG = 30;
const MAX_SYNC = 100;
const PREDICTION_ENABLED = true;
const LOOKAHEAD = 2; // K: predict this many steps ahead (RTT 200ms / tick 100ms)
let _predId = 0;     // monotonic ID for log entries

const DR = [-1, 0, 1, 0];
const DC = [0, 1, 0, -1];

function timestamp() {
    const d = new Date();
    const pad = (n, w = 2) => String(n).padStart(w, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3).slice(0, 2)}`;
}

// Safe iff not a 180° reversal, in bounds, not into a wall, and not into an
// occupied body cell. `occupied` is a Set of r*cols+c for the relevant bodies.
function isSafeStep(dir, prevDir, hr, hc, occupied, gmap, rows, cols) {
    if (dir === (prevDir + 2) % 4) return false;
    const nr = hr + DR[dir], nc = hc + DC[dir];
    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return false;
    if (gmap[nr][nc] === 1) return false;
    if (occupied.has(nr * cols + nc)) return false;
    return true;
}

// "Constrained" = at least one non-reverse neighbour of the (post-move) head is
// blocked by a wall, boundary, or a body cell. This is the regime the model is
// most accurate in, so we only spend predictions here. occupied is the union of
// both snakes' body cells.
function isConstrained(headR, headC, prevDir, occupied, gmap, rows, cols) {
    const reverse = (prevDir + 2) % 4;
    for (let d = 0; d < 4; d++) {
        if (d === reverse) continue;
        const nr = headR + DR[d], nc = headC + DC[d];
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return true;
        if (gmap[nr][nc] === 1) return true;
        if (occupied.has(nr * cols + nc)) return true;
    }
    return false;
}

export default {
    name: 'PlaygroundIndexView',
    components: {
        PlayGround, MatchGround, ResultBoard,
        LatencyDisplay, LatencyInjector, PredictionLog, SyncMonitor
    },
    setup() {
        const store = useStore();
        const socUrl = `ws://localhost:3000/websocket/${store.state.user.token}/`;

        let socket       = null;
        let predictorA   = null;  // subject = snake0 (server side A)
        let predictorB   = null;  // subject = snake1 (server side B)
        let pingInterval = null;
        let pingSentAt   = null;

        // ── Prediction / sync state ──────────────────────────────────────────
        let moveCount = 0;     // confirmed server steps received
        let pending   = [];    // FIFO of predicted ticks awaiting verification
        let predEpoch = 0;     // bumped every move; invalidates stale predict().then
        let gameEnded = false; // true once a result arrives
        let inferring = false; // true while an ONNX run is in flight (prevents re-entry)

        // Confirmed (server-authoritative) shadow of each snake, evolved purely
        // from the move stream so features/predictions never read mid-animation
        // live cells. body is head-first [{r,c}, ...]; dir is the last move.
        let shadowA = null, shadowB = null;
        let confApple = null;             // { r, c }

        const rttMs      = ref(0);
        const injectedMs = ref(0);
        const logEntries = ref([]);
        const syncEntries = ref([]);

        const initShadow = snake => ({
            body: snake.cells.map(c => ({ r: c.r, c: c.c })),
            dir:  snake.eye_direction
        });
        // Evolve the shadow by one confirmed move, mirroring Snake's body rules:
        // grow head; drop tail unless step≤10 or step%3==1; drop one more on eat.
        const evolveShadow = (sh, dir, eat, step) => {
            const h = sh.body[0];
            sh.body.unshift({ r: h.r + DR[dir], c: h.c + DC[dir] });
            if (!(step <= 10 || step % 3 === 1)) sh.body.pop();
            if (eat && sh.body.length > 1) sh.body.pop();
            sh.dir = dir;
        };

        function pushLog(player, stepLabel, predicted) {
            const e = { id: ++_predId, time: timestamp(), player,
                        step: stepLabel, predicted, status: 'pending', actual: null };
            logEntries.value.push(e);
            if (logEntries.value.length > MAX_LOG) logEntries.value.shift();
            return e;
        }

        onMounted(() => {
            store.commit("updateOpponent", { username: "Opponent" });

            predictorA = new SnakePredictor();
            predictorB = new SnakePredictor();
            Promise.all([predictorA.init(), predictorB.init()])
                .catch(err => console.error('[SnakePredictor] init failed:', err));

            socket = new WebSocket(socUrl);

            socket.onopen = () => {
                console.log("WebSocket connection established.");
                store.commit("updateSocket", socket);

                pingInterval = setInterval(() => {
                    if (socket.readyState === WebSocket.OPEN) {
                        pingSentAt = performance.now();
                        socket.send(JSON.stringify({ event: "ping" }));
                    }
                }, 100);
            };

            socket.onmessage = msg => {
                const data = JSON.parse(msg.data);

                if (data.event === "pong") {
                    if (pingSentAt !== null) {
                        const rtt = performance.now() - pingSentAt;
                        rttMs.value = rtt;
                        predictorA.updateRTT(rtt + injectedMs.value);
                        predictorB.updateRTT(rtt + injectedMs.value);
                        pingSentAt = null;
                    }

                } else if (data.event === "matching-success") {
                    // Reset all per-game prediction/sync state
                    pending = [];
                    moveCount = 0;
                    predEpoch++;          // invalidate any in-flight prediction
                    gameEnded = false;
                    inferring = false;
                    shadowA = null; shadowB = null; confApple = null;
                    logEntries.value = [];
                    syncEntries.value = [];
                    predictorA.reset();
                    predictorB.reset();

                    store.commit("updateOpponent", { username: data.opponent_username });
                    setTimeout(() => { store.commit("updateStatus", "playing"); }, 1000);
                    store.commit("updateGamemap", data.game);

                } else if (data.event === "move") {
                    setTimeout(() => {
                        const game = store.state.playground.gameObject;
                        if (!game || gameEnded) return;
                        const [snake0, snake1] = game.snakes;
                        const rows = game.rows, cols = game.cols;
                        const gmap = store.state.playground.gamemap;

                        predEpoch++;                 // any in-flight predict is now stale
                        moveCount++;
                        const M = moveCount;
                        const fps = game.timedelta ? (1000 / game.timedelta).toFixed(0) : '?';
                        syncEntries.value.push({
                            id:    M,
                            tick:  M,
                            step:  snake0.step,
                            q:     snake0.direction_queue.length,
                            fps,
                            rtt:   rttMs.value.toFixed(0),
                            pred:  predictorA.canPredict(),
                            ahead: snake0.step - M
                        });
                        if (syncEntries.value.length > MAX_SYNC) syncEntries.value.shift();

                        // ── Initialise the confirmed shadow on first move ───────
                        if (shadowA === null) { shadowA = initShadow(snake0); shadowB = initShadow(snake1); }

                        // ── Verify the oldest pending prediction for this step ──
                        let suppressed = false;
                        if (pending.length > 0 && pending[0].step === M) {
                            const p = pending.shift();
                            const okA = data.a_direction === p.dirA && data.a_ate_apple === p.eatA;
                            const okB = data.b_direction === p.dirB && data.b_ate_apple === p.eatB;
                            p.logA.status = okA ? 'correct' : 'rollback'; p.logA.actual = data.a_direction;
                            p.logB.status = okB ? 'correct' : 'rollback'; p.logB.actual = data.b_direction;

                            if (okA && okB) {
                                suppressed = true;   // engine already advanced step M correctly
                            } else {
                                // Undo step M and every prediction queued after it.
                                // If the engine hasn't drained step M yet there is no
                                // snapshot to restore — the snake is still at the
                                // confirmed pre-M state, so just drop the queued moves.
                                if (!game.rollback_to(M)) {
                                    snake0.direction_queue = [];
                                    snake1.direction_queue = [];
                                }
                                for (const q of pending) {
                                    if (q.logA.status === 'pending') q.logA.status = 'expired';
                                    if (q.logB.status === 'pending') q.logB.status = 'expired';
                                }
                                pending = [];
                            }
                        }

                        // ── Apply the real move when not already (correctly) predicted ──
                        if (!suppressed) {
                            snake0.enqueue_direction(data.a_direction, data.a_ate_apple);
                            snake1.enqueue_direction(data.b_direction, data.b_ate_apple);
                        }

                        // ── Authoritative apple + shadow update ─────────────────
                        game.apple.r = data.apple_r;
                        game.apple.c = data.apple_c;
                        evolveShadow(shadowA, data.a_direction, data.a_ate_apple, M);
                        evolveShadow(shadowB, data.b_direction, data.b_ate_apple, M);
                        confApple = { r: data.apple_r, c: data.apple_c };

                        // ── Feed both predictors (exact post-move state) ────────
                        const occupied = new Set();
                        for (const c of shadowA.body) occupied.add(c.r * cols + c.c);
                        for (const c of shadowB.body) occupied.add(c.r * cols + c.c);
                        const headA = shadowA.body[0], headB = shadowB.body[0];
                        predictorA.addFrame(data.a_direction, headA.r, headA.c, occupied, gmap,
                                            game.apple.r, game.apple.c, data.a_ate_apple);
                        predictorB.addFrame(data.b_direction, headB.r, headB.c, occupied, gmap,
                                            game.apple.r, game.apple.c, data.b_ate_apple);

                        // ── Refill the lookahead window when fully drained ──────
                        // Gate on at least one snake being in a constrained position
                        // (switch to consA && consB for the high-confidence-only regime).
                        const consA = isConstrained(headA.r, headA.c, shadowA.dir, occupied, gmap, rows, cols);
                        const consB = isConstrained(headB.r, headB.c, shadowB.dir, occupied, gmap, rows, cols);

                        if (PREDICTION_ENABLED && (consA && consB) && !inferring && pending.length === 0 &&
                            predictorA.canPredict() && predictorB.canPredict()) {

                            const myEpoch = predEpoch;
                            const baseStep = M;
                            const baseA = { headR: shadowA.body[0].r, headC: shadowA.body[0].c, dir: shadowA.dir };
                            const baseB = { headR: shadowB.body[0].r, headC: shadowB.body[0].c, dir: shadowB.dir };
                            const baseApple = confApple;

                            inferring = true;
                            (async () => {
                                // Run the two models sequentially: the wasm backend cannot
                                // run overlapping inferences on a session.
                                let pa, pb;
                                try {
                                    pa = await predictorA.predict();
                                    pb = await predictorB.predict();
                                } finally {
                                    inferring = false;
                                }

                                if (myEpoch !== predEpoch || gameEnded) return; // stale / ended
                                if (pa === null || pb === null) return;
                                if (pending.length !== 0) return;               // already refilled

                                // Simulate the lookahead from the confirmed shadow so the
                                // result is independent of live (mid-animation) cell state.
                                // Collision occupancy = both confirmed bodies minus their
                                // tail tips (which vacate as the snake moves), growing as we
                                // accept each predicted head.
                                const occ = new Set();
                                for (let i = 0; i < shadowA.body.length - 1; i++)
                                    occ.add(shadowA.body[i].r * cols + shadowA.body[i].c);
                                for (let i = 0; i < shadowB.body.length - 1; i++)
                                    occ.add(shadowB.body[i].r * cols + shadowB.body[i].c);

                                let hrA = baseA.headR, hcA = baseA.headC, pdA = baseA.dir;
                                let hrB = baseB.headR, hcB = baseB.headC, pdB = baseB.dir;
                                let aR = baseApple.r, aC = baseApple.c;

                                for (let k = 0; k < LOOKAHEAD; k++) {
                                    const dA = pa[k], dB = pb[k];
                                    if (!isSafeStep(dA, pdA, hrA, hcA, occ, gmap, rows, cols)) break;
                                    if (!isSafeStep(dB, pdB, hrB, hcB, occ, gmap, rows, cols)) break;

                                    const nrA = hrA + DR[dA], ncA = hcA + DC[dA];
                                    const nrB = hrB + DR[dB], ncB = hcB + DC[dB];
                                    if (nrA === nrB && ncA === ncB) break; // head-on collision

                                    const eatA = (nrA === aR && ncA === aC);
                                    const eatB = (nrB === aR && ncB === aC);
                                    if (eatA || eatB) { aR = -1; aC = -1; } // respawn unknown

                                    snake0.enqueue_direction(dA, eatA);
                                    snake1.enqueue_direction(dB, eatB);

                                    const logA = pushLog('A', `t+${k + 1}`, dA);
                                    const logB = pushLog('B', `t+${k + 1}`, dB);
                                    pending.push({ step: baseStep + k + 1,
                                        dirA: dA, dirB: dB, eatA, eatB, logA, logB });

                                    occ.add(nrA * cols + ncA);
                                    occ.add(nrB * cols + ncB);
                                    hrA = nrA; hcA = ncA; pdA = dA;
                                    hrB = nrB; hcB = ncB; pdB = dB;
                                }
                            })();
                        }
                    }, injectedMs.value);

                } else if (data.event === "result") {
                    // Stop further advancement immediately so the engine cannot drain
                    // queued predictions past the end of the game.
                    gameEnded = true;
                    pending = [];
                    const g = store.state.playground.gameObject;
                    if (g) {
                        g.snakes[0].direction_queue = [];
                        g.snakes[1].direction_queue = [];
                    }

                    setTimeout(() => {
                        const game = store.state.playground.gameObject;
                        if (!game) return;
                        const [snake0, snake1] = game.snakes;
                        // Let any in-progress animation finish before showing death.
                        const applyResult = () => {
                            if (snake0.status === 'move' || snake1.status === 'move') {
                                requestAnimationFrame(applyResult); return;
                            }
                            // Reconcile to the last server-confirmed state: undo any
                            // predicted steps rendered ahead but never verified (the
                            // fatal move is never sent), so death shows at the real spot.
                            game.rollback_to(moveCount + 1);
                            if (data.loser === "all" || data.loser === "A") snake0.status = "die";
                            if (data.loser === "all" || data.loser === "B") snake1.status = "die";
                            store.commit("updateLoser", data.loser);
                        };
                        requestAnimationFrame(applyResult);
                    }, injectedMs.value);
                }
            };

            socket.onclose = () => {
                console.log("WebSocket connection closed.");
                store.commit("updateStatus", "matching");
            };
        });

        onUnmounted(() => {
            if (pingInterval) clearInterval(pingInterval);
            if (socket) socket.close();
        });

        return { rttMs, injectedMs, logEntries, syncEntries };
    }
}
</script>

<style scoped>
.dashboard {
    display: flex;
    gap: 16px;
    padding: 16px;
    height: 88vh;
    box-sizing: border-box;
    background: var(--tb-bg);
    font-family: var(--tb-font-ui);
}
.game-area {
    flex: 1 1 auto;
    position: relative;          /* anchor for ResultBoard */
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 0;
    background: var(--tb-card);
    border: 1px solid var(--tb-border);
    border-radius: 12px;
    box-shadow: var(--tb-shadow);
    overflow: hidden;
}
.sidebar {
    flex: 0 0 360px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}
</style>
