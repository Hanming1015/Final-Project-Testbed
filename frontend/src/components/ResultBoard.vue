<template>
    <div class="result-board">
        <div class="result-board-text" v-if="$store.state.playground.loser === 'all'">
            Draw
        </div>
        <div class="result-board-text" v-else-if="$store.state.playground.loser === 'A' && $store.state.playground.a_id === $store.state.user.id">
            You Lose
        </div>
        <div class="result-board-text" v-else-if="$store.state.playground.loser === 'B' && $store.state.playground.b_id === $store.state.user.id">
            You Lose
        </div>
        <div class="result-board-text" v-else>
            You Win
        </div>
        <div class="result-board-btn">
            <button type="button" class="btn btn-warning btn-lg" @click="restart">
                Restart
            </button>
        </div>
    </div>
</template>

<script>
import { useStore } from "vuex";

export default {
    name: 'ResultBoard',
    setup() {
        const store = useStore();

        const restart = () => {
            store.commit("updateStatus", "matching");
            store.commit("updateLoser", "none");
            store.commit("updateOpponent", {
                username: "Opponent"
            });
        }

        return {
            store,
            restart
        }

    },
};
</script>

<style scoped>
div.result-board {
    height: 30vh;
    width: 30vw;
    background-color: rgba(50, 50, 50, 0.5);
    position: absolute;
    top: 30vh;
    left: 35vw;
}
div.result-board-text {
    font-size: 4vh;
    color: white;
    text-align: center;
    margin-top: 4vh;
    font-weight: 600;
    font-style: italic;
    padding-top: 5vh;
}
div.result-board-btn {
    padding-top: 5vh;
    text-align: center;

}
</style>