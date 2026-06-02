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
        onMounted(() => {
            store.commit("updateOpponent", {
                username: "Opponent"
            });
            socket = new WebSocket(socUrl);
            socket.onopen = () => {
                console.log("WebSocket connection established.");
                store.commit("updateSocket", socket);
            };

            socket.onmessage = msg => {
                const data = JSON.parse(msg.data);
                if (data.event === "matching-success") {
                    store.commit("updateOpponent", {
                        username: data.opponent_username
                    });
                    setTimeout(() => {
                        store.commit("updateStatus", "playing");
                    }, 1000);
                    store.commit("updateGamemap", data.game);
                } else if (data.event === "move") {
                    const game = store.state.playground.gameObject;
                    if (!game) return;
                    const [snake0, snake1] = game.snakes;
                    snake0.set_direction(data.a_direction);
                    snake1.set_direction(data.b_direction);
                    snake0.eat_apple = data.a_ate_apple;
                    snake1.eat_apple = data.b_ate_apple;
                    game.apple.r = data.apple_r;
                    game.apple.c = data.apple_c;
                } else if (data.event === "result") {
                    console.log("Game result: ", data);
                    const game = store.state.playground.gameObject;
                    const [snake0, snake1] = game.snakes;
                    if (data.loser === "all" || data.loser === "A") {
                        snake0.status = "die";
                    }
                    if (data.loser === "all" || data.loser === "B") {
                        snake1.status = "die";
                    }
                    store.commit("updateLoser", data.loser);
                }
            };

            socket.onclose = () => {
                console.log("WebSocket connection closed.");
                store.commit("updateStatus", "matching");
            };
        });

        onUnmounted(() => {
            if (socket) {
                socket.close();
            }
        });
    }
}
</script>

<style scoped></style>