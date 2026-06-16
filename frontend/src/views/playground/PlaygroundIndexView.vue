<template>
    <div>
        <PlayGround v-if="$store.state.playground.status === 'playing'" />
        <MatchGround v-else />
        <ResultBoard v-if="$store.state.playground.loser !== 'none'" />
        <LatencyDisplay  :rtt="rttMs"      :injected="injectedMs" />
        <LatencyInjector v-model="injectedMs" />
        <PredictionLog   :entries="logEntries" />
    </div>
</template>

<script>
import PlayGround     from '@/components/PlayGround.vue';
import MatchGround    from '@/components/MatchGround.vue';
import ResultBoard    from '@/components/ResultBoard.vue';
import LatencyDisplay  from '@/components/LatencyDisplay.vue';
import LatencyInjector from '@/components/LatencyInjector.vue';
import PredictionLog   from '@/components/PredictionLog.vue';
import { SnakePredictor } from '@/assets/scripts/SnakePredictor';
import { onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex';

const MAX_LOG = 30;

function timestamp() {
    const d = new Date();
    const pad = (n, w = 2) => String(n).padStart(w, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3).slice(0, 2)}`;
}

export default {
    name: 'PlaygroundIndexView',
    components: {
        PlayGround, MatchGround, ResultBoard,
        LatencyDisplay, LatencyInjector, PredictionLog
    },
    setup() {
        const store = useStore();
        const socUrl = `ws://localhost:3000/websocket/${store.state.user.token}/`;

        let socket          = null;
        let predictor       = null;
        let localSnakeIdx   = 0;
        let pingInterval    = null;
        let pingSentAt      = null;
        let pendingPrediction    = null;   // { id, dir } — waiting for next move to confirm
        let skipNextPrediction  = false;  // true for one tick after a rollback

        const rttMs      = ref(0);
        const injectedMs = ref(0);
        const logEntries = ref([]);

        onMounted(() => {
            store.commit("updateOpponent", { username: "Opponent" });

            predictor = new SnakePredictor();
            predictor.init().catch(err => console.error('[SnakePredictor] init failed:', err));

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
                        // Effective latency = real RTT + frontend-injected one-way delay
                        predictor.updateRTT(rtt + injectedMs.value);
                        console.log(`[RTT] ${rtt.toFixed(2)} ms`);
                        pingSentAt = null;
                    }

                } else if (data.event === "matching-success") {
                    store.commit("updateOpponent", { username: data.opponent_username });
                    setTimeout(() => { store.commit("updateStatus", "playing"); }, 1000);
                    store.commit("updateGamemap", data.game);
                    localSnakeIdx = (String(store.state.user.id) === String(data.game.a_id)) ? 0 : 1;

                } else if (data.event === "move") {
                    // Front-end injection: delay processing to simulate one-way network latency
                    setTimeout(() => {
                        const game = store.state.playground.gameObject;
                        if (!game) return;
                        const [snake0, snake1] = game.snakes;

                        const localSnake = game.snakes[localSnakeIdx];
                        const oppSnake   = game.snakes[1 - localSnakeIdx];

                        // Confirm or rollback the previous prediction against the opponent's actual direction
                        if (pendingPrediction !== null) {
                            const actualOppDir = localSnakeIdx === 0 ? data.b_direction : data.a_direction;
                            const entry = logEntries.value.find(e => e.id === pendingPrediction.id);
                            if (entry) {
                                const isCorrect = actualOppDir === pendingPrediction.dir;
                                entry.status = isCorrect ? 'correct' : 'rollback';
                                entry.actual = actualOppDir;
                                // On rollback: skip next prediction so the server-corrected
                                // direction is visible for a full tick before prediction resumes
                                if (!isCorrect) skipNextPrediction = true;
                            }
                            pendingPrediction = null;
                        }

                        snake0.set_direction(data.a_direction);
                        snake1.set_direction(data.b_direction);
                        snake0.eat_apple = data.a_ate_apple;
                        snake1.eat_apple = data.b_ate_apple;
                        game.apple.r = data.apple_r;
                        game.apple.c = data.apple_c;

                        if (game.apple) {
                            const gamemap   = store.state.playground.gamemap;
                            // Feed the predictor from the opponent's perspective
                            const oppDir    = localSnakeIdx === 0 ? data.b_direction : data.a_direction;
                            const oppJustAte = localSnakeIdx === 0 ? data.b_ate_apple : data.a_ate_apple;

                            predictor.addFrame(oppDir, oppSnake, localSnake, gamemap,
                                               game.apple.r, game.apple.c, oppJustAte);

                            const canPredict = !skipNextPrediction;
                            skipNextPrediction = false;

                            if (canPredict && predictor.shouldTrigger(oppDir, oppSnake, localSnake,
                                                        gamemap, game.rows, game.cols)) {
                                predictor.predict().then(predictedDir => {
                                    if (predictedDir === null) return;

                                    const oppPlayerLabel = localSnakeIdx === 0 ? 'B' : 'A';
                                    const entry = {
                                        id: Date.now(),
                                        time: timestamp(),
                                        player: oppPlayerLabel,
                                        predicted: predictedDir,
                                        status: 'pending',
                                        actual: null
                                    };
                                    logEntries.value.push(entry);
                                    if (logEntries.value.length > MAX_LOG) logEntries.value.shift();

                                    pendingPrediction = { id: entry.id, dir: predictedDir };
                                    console.log(`[SnakePredictor] trigger fired → predicted opp: ${predictedDir}`);
                                    oppSnake.set_direction(predictedDir);
                                });
                            }
                        }
                    }, injectedMs.value);

                } else if (data.event === "result") {
                    const game = store.state.playground.gameObject;
                    const [snake0, snake1] = game.snakes;
                    if (data.loser === "all" || data.loser === "A") snake0.status = "die";
                    if (data.loser === "all" || data.loser === "B") snake1.status = "die";
                    store.commit("updateLoser", data.loser);
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

        return { rttMs, injectedMs, logEntries };
    }
}
</script>

<style scoped></style>
