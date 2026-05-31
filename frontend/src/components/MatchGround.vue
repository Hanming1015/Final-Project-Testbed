<template>
    <div class="matchground">
        <div class="row">
            <div class="col-6">
                <div class="username">
                    {{ $store.state.user.username }}
                </div>
            </div>
            <div class="col-6">
                <div class="username">
                    {{ $store.state.playground.opponent_username }}
                </div>
            </div>
            <div class="col-12" style="text-align: center; padding-top: 15vh;">
                <button @click="click_match_btn" type="button" class="btn btn-warning btn-lg">{{ match_btn_info }}</button>
            </div>
        </div>
    </div>

</template>

<script>
import { ref } from 'vue';
import { useStore } from 'vuex';

export default {
    name: 'MatchGround',

    setup() {
        const store = useStore();
        let match_btn_info = ref("Start Matching");

        const click_match_btn = () => {
            if (match_btn_info.value === "Start Matching") {
                store.state.playground.socket.send(JSON.stringify({
                    event: "start-matching",
                }));
                match_btn_info.value = "Cancel Matching";
            } else {
                match_btn_info.value = "Start Matching";
                store.state.playground.socket.send(JSON.stringify({
                    event: "stop-matching",
                }));
            }
        }

        return {
            match_btn_info,
            click_match_btn
        }
    }
};
</script>

<style scoped>
div.matchground {
    width: 60vw;
    height: 70vh;
    margin: 5vh auto;
    background-color: rgba(50, 50, 50, 0.5);
}

div.user-photo>img {
    width: 20vh;
    border-radius: 50%;
    object-fit: cover;
}

div.username {
    text-align: center;
    margin-top: 200px;
    font-size: 1.5em;
    font-weight: bold;
    color: white;
    padding-top: 2vh;
}

div.user-select-bot {
    padding-top: 15vh;
}

div.user-select-bot > select {
    width: 70%;
    margin: 0 auto;
}
</style>