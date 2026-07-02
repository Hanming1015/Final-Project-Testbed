<template>
    <div class="panel model-toggle">
        <div class="header">
            <span class="panel-title">Predictor Mode</span>
            <button class="toggle-btn" :class="modelValue" @click="cycle">
                {{ label }}
            </button>
        </div>
        <div class="status">
            <span class="dim">Status</span>
            <span :class="modelValue === 'off' ? 'silent-text' : 'active-text'">
                {{ statusText }}
            </span>
        </div>
    </div>
</template>

<script>
import { computed } from 'vue';

// Three predictor modes, cycled by the toggle (the eval independent variable):
//   off   — no prediction (freeze + snap baseline)
//   rule  — dead-reckoning: repeat the last confirmed direction
//   model — the GRU Seq2Seq
const ORDER  = ['off', 'rule', 'model'];
const LABEL  = { off: 'OFF',    rule: 'RULE',            model: 'MODEL' };
const STATUS = { off: 'Silent', rule: 'Dead-reckoning',  model: 'GRU active' };

export default {
    name: 'ModelToggle',
    props: {
        modelValue: { type: String, default: 'model' }
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
        const cycle = () => {
            const i = ORDER.indexOf(props.modelValue);
            emit('update:modelValue', ORDER[(i + 1) % ORDER.length]);
        };
        const label      = computed(() => LABEL[props.modelValue]  ?? 'OFF');
        const statusText = computed(() => STATUS[props.modelValue] ?? 'Silent');
        return { cycle, label, statusText };
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
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}
.panel-title {
    font-size: 11px;
    color: var(--tb-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 600;
}
.toggle-btn {
    background: var(--tb-border-2);
    color: var(--tb-muted);
    border: 1px solid var(--tb-border);
    border-radius: 6px;
    padding: 2px 12px;
    min-width: 64px;
    cursor: pointer;
    font-family: var(--tb-font-ui);
    font-size: 12px;
    font-weight: 600;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}
/* off = muted (default). rule = amber (baseline). model = accent (learned). */
.toggle-btn.rule {
    background: #d97706;
    border-color: #d97706;
    color: #fff;
}
.toggle-btn.model {
    background: var(--tb-accent);
    border-color: var(--tb-accent);
    color: #fff;
}
.status {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
}
.dim { color: var(--tb-muted); }
.active-text { color: var(--tb-success); font-weight: 600; font-family: var(--tb-font-mono); }
.silent-text { color: var(--tb-muted-2); font-weight: 600; font-family: var(--tb-font-mono); }
</style>
