<template>
    <div class="panel spike-injector">
        <div class="panel-title">Latency Spike</div>

        <div class="field">
            <span class="label">Magnitude</span>
            <div class="control">
                <input type="number" min="0" max="1000" step="50" v-model.number="ms" />
                <span class="unit">ms</span>
            </div>
        </div>
        <div class="field">
            <span class="label">Duration</span>
            <div class="control">
                <input type="number" min="1" max="20" step="1" v-model.number="ticks" />
                <span class="unit">ticks</span>
            </div>
        </div>

        <button class="tb-btn primary" @click="fire">Fire Spike</button>
        <div class="hint">+{{ ms }} ms on the next {{ ticks }} move{{ ticks === 1 ? '' : 's' }}</div>
    </div>
</template>

<script>
import { ref } from 'vue';

export default {
    name: 'SpikeInjector',
    emits: ['fire'],
    setup(_, { emit }) {
        const ms    = ref(200);
        const ticks = ref(1);

        const fire = () => {
            const m = Math.max(0, ms.value || 0);
            const t = Math.max(1, ticks.value || 1);
            emit('fire', { ms: m, ticks: t });
        };

        return { ms, ticks, fire };
    }
};
</script>

<style scoped>
.panel {
    background: var(--tb-card);
    border: 1px solid var(--tb-border);
    border-radius: var(--tb-radius);
    box-shadow: var(--tb-shadow);
    padding: 14px 16px;
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
    font-size: 13px;
    user-select: none;
}
.panel-title {
    font-size: 11px;
    color: var(--tb-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 600;
    margin-bottom: 12px;
}
.field {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}
.label { color: var(--tb-muted); }
.control {
    display: flex;
    align-items: center;
    gap: 6px;
}
.control input {
    width: 64px;
    padding: 4px 8px;
    border: 1px solid var(--tb-border);
    border-radius: 8px;
    background: var(--tb-card);
    color: var(--tb-fg);
    font-family: var(--tb-font-mono);
    font-size: 13px;
    text-align: right;
}
.control input:focus {
    outline: none;
    border-color: var(--tb-accent);
    box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}
.unit { color: var(--tb-muted-2); font-size: 12px; }
.tb-btn {
    width: 100%;
    padding: 9px 0;
    border-radius: var(--tb-radius);
    border: 1px solid var(--tb-border);
    background: var(--tb-card);
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: filter 0.15s, transform 0.05s;
}
.tb-btn.primary {
    background: var(--tb-accent);
    border-color: var(--tb-accent);
    color: #fff;
}
.tb-btn.primary:hover { filter: brightness(1.07); }
.tb-btn.primary:active { transform: translateY(1px); }
.hint {
    margin-top: 8px;
    font-size: 11px;
    color: var(--tb-muted-2);
    font-family: var(--tb-font-mono);
    text-align: center;
}
</style>
