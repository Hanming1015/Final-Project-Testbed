<template>
    <div class="result-overlay">
        <div class="result-card">
            <div class="result-text" :class="resultClass">{{ resultText }}</div>
            <button type="button" class="tb-btn primary" @click="restart">
                Restart
            </button>
        </div>
    </div>
</template>

<script>
import { computed } from "vue";
import { useStore } from "vuex";

export default {
    name: 'ResultBoard',
    setup() {
        const store = useStore();

        const outcome = computed(() => {
            const pg = store.state.playground;
            if (pg.loser === 'all') return 'draw';
            if (pg.loser === 'A' && pg.a_id === store.state.user.id) return 'lose';
            if (pg.loser === 'B' && pg.b_id === store.state.user.id) return 'lose';
            return 'win';
        });

        const resultText = computed(() =>
            outcome.value === 'draw' ? 'Draw'
            : outcome.value === 'lose' ? 'You Lose'
            : 'You Win'
        );

        const resultClass = computed(() => outcome.value);

        const restart = () => {
            store.commit("updateStatus", "matching");
            store.commit("updateLoser", "none");
            store.commit("updateOpponent", {
                username: "Opponent"
            });
        }

        return {
            store,
            resultText,
            resultClass,
            restart
        }

    },
};
</script>

<style scoped>
.result-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(248, 250, 252, 0.7);
    backdrop-filter: blur(2px);
    z-index: 10;
}
.result-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
    padding: 40px 56px;
    background: var(--tb-card);
    border: 1px solid var(--tb-border);
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
    font-family: var(--tb-font-ui);
}
.result-text {
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--tb-fg);
}
.result-text.win  { color: var(--tb-success); }
.result-text.lose { color: var(--tb-danger); }
.result-text.draw { color: var(--tb-muted); }

.tb-btn {
    padding: 10px 28px;
    border-radius: var(--tb-radius);
    border: 1px solid var(--tb-border);
    background: var(--tb-card);
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, transform 0.05s, filter 0.15s;
}
.tb-btn.primary {
    background: var(--tb-accent);
    border-color: var(--tb-accent);
    color: #fff;
}
.tb-btn.primary:hover { filter: brightness(1.07); }
.tb-btn.primary:active { transform: translateY(1px); }
</style>