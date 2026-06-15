<template>
    <div>
        <PlayGround v-if="$store.state.playground.status === 'playing'" />
        <MatchGround v-else />
        <ResultBoard v-if="$store.state.playground.loser !== 'none'" />
    </div>
</template>

<script>
import PlayGround from '@/components/PlayGround.vue';
import MatchGround from '@/components/MatchGround.vue';
import ResultBoard from '@/components/ResultBoard.vue';
import { SnakePredictor } from '@/assets/scripts/SnakePredictor';
import { onMounted, onUnmounted } from 'vue';
import { useStore } from 'vuex';

export default {
    name: 'PlaygroundIndexView',
    components: {
        PlayGround,
        MatchGround,
        ResultBoard
    },
    setup() {
        const store = useStore();
        const socUrl = `ws://localhost:3000/websocket/${store.state.user.token}/`;
        let socket = null;
        let predictor = null;
        let localSnakeIdx = 0;  // 0 = local player is A, 1 = local player is B
        let pingInterval = null;
        let pingSentAt = null;

        onMounted(() => {
            store.commit("updateOpponent", {
                username: "Opponent"
            });

            predictor = new SnakePredictor();
            predictor.init().catch(err => console.error('[SnakePredictor] init failed:', err));

            socket = new WebSocket(socUrl);

            socket.onopen = () => {
                console.log("WebSocket connection established.");
                store.commit("updateSocket", socket);

                // Ping every 100ms to measure RTT
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
                        predictor.updateRTT(performance.now() - pingSentAt);
                        pingSentAt = null;
                    }

                } else if (data.event === "matching-success") {
                    store.commit("updateOpponent", { username: data.opponent_username });
                    setTimeout(() => { store.commit("updateStatus", "playing"); }, 1000);
                    store.commit("updateGamemap", data.game);
                    localSnakeIdx = (String(store.state.user.id) === String(data.game.a_id)) ? 0 : 1;

                } else if (data.event === "move") {
                    const game = store.state.playground.gameObject;
                    if (!game) return;
                    const [snake0, snake1] = game.snakes;

                    // Apply server-authoritative directions
                    snake0.set_direction(data.a_direction);
                    snake1.set_direction(data.b_direction);
                    snake0.eat_apple = data.a_ate_apple;
                    snake1.eat_apple = data.b_ate_apple;
                    game.apple.r = data.apple_r;
                    game.apple.c = data.apple_c;

                    // Feed predictor and check trigger
                    if (game.apple) {
                        const gamemap    = store.state.playground.gamemap;
                        const localSnake = game.snakes[localSnakeIdx];
                        const oppSnake   = game.snakes[1 - localSnakeIdx];
                        const localDir   = localSnakeIdx === 0 ? data.a_direction : data.b_direction;
                        const justAte    = localSnakeIdx === 0 ? data.a_ate_apple : data.b_ate_apple;

                        predictor.addFrame(localDir, localSnake, oppSnake, gamemap,
                                           game.apple.r, game.apple.c, justAte);

                        if (predictor.shouldTrigger(localDir, localSnake, oppSnake,
                                                    gamemap, game.rows, game.cols)) {
                            predictor.predict().then(predictedDir => {
                                if (predictedDir === null) return;
                                console.log(`[SnakePredictor] trigger fired → predicted: ${predictedDir}`);
                                // Pre-apply prediction; server response will correct if wrong
                                localSnake.set_direction(predictedDir);
                            });
                        }
                    }

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
    }
}
</script>

<style scoped></style>