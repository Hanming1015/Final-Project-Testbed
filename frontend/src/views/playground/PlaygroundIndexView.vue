<template>
    <div>
        <PlayGround v-if="$store.state.playground.status === 'playing'" />
        <MatchGround v-else />
    </div>
</template>

<script>
import PlayGround from '@/components/PlayGround.vue';
import MatchGround from '@/components/MatchGround.vue';
import { onMounted, onUnmounted } from 'vue';
import { useStore } from 'vuex';

export default {
    name: 'PlaygroundIndexView',
    components: {
        PlayGround,
        MatchGround
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
                    }, 2000);
                    store.commit("updateGamemap", data.gamemap);
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