<template>
    <div class="matchground">
        <div class="lobby-title">Match Lobby</div>

        <div class="versus">
            <div class="player">
                <div class="avatar">{{ initial($store.state.user.username) }}</div>
                <div class="name">{{ $store.state.user.username || 'You' }}</div>
            </div>
            <div class="vs">VS</div>
            <div class="player">
                <div class="avatar opponent">{{ initial($store.state.playground.opponent_username) }}</div>
                <div class="name">{{ $store.state.playground.opponent_username || 'Waiting…' }}</div>
            </div>
        </div>

        <div class="actions">
            <button @click="click_match_btn" type="button" class="tb-btn primary">
                {{ match_btn_info }}
            </button>
            <button @click="click_bot_btn" type="button" class="tb-btn">
                Player vs Bot
            </button>
            <button @click="click_watch_btn" type="button" class="tb-btn">
                Bot vs Bot
            </button>
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

        const initial = name => (name && name.length ? name[0].toUpperCase() : '?');

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

        const click_bot_btn = () => {
            store.state.playground.socket.send(JSON.stringify({
                event: "start-bot-game",
            }));
        }

        const click_watch_btn = () => {
            store.state.playground.socket.send(JSON.stringify({
                event: "start-watch-game",
            }));
        }

        return {
            match_btn_info,
            initial,
            click_match_btn,
            click_bot_btn,
            click_watch_btn,
        }
    }
};
</script>

<style scoped>
.matchground {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 48px;
    padding: 40px;
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
    user-select: none;
}
.lobby-title {
    font-size: 13px;
    color: var(--tb-muted);
    text-transform: uppercase;
    letter-spacing: 0.14em;
    font-weight: 600;
}
.versus {
    display: flex;
    align-items: center;
    gap: 40px;
}
.player {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    min-width: 120px;
}
.avatar {
    width: 84px;
    height: 84px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 34px;
    font-weight: 700;
    color: #fff;
    background: var(--tb-accent);
    box-shadow: var(--tb-shadow);
}
.avatar.opponent {
    background: var(--tb-muted-2);
}
.name {
    font-size: 16px;
    font-weight: 600;
    color: var(--tb-fg);
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
}
.vs {
    font-size: 18px;
    font-weight: 700;
    color: var(--tb-muted-2);
    letter-spacing: 0.06em;
}
.actions {
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
    justify-content: center;
}
.tb-btn {
    padding: 10px 22px;
    border-radius: var(--tb-radius);
    border: 1px solid var(--tb-border);
    background: var(--tb-card);
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, transform 0.05s;
}
.tb-btn:hover {
    background: var(--tb-border-2);
    border-color: var(--tb-muted-2);
}
.tb-btn:active {
    transform: translateY(1px);
}
.tb-btn.primary {
    background: var(--tb-accent);
    border-color: var(--tb-accent);
    color: #fff;
}
.tb-btn.primary:hover {
    filter: brightness(1.07);
    background: var(--tb-accent);
}
</style>