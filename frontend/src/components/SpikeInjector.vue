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

        <div class="btn-row">
            <button class="tb-btn primary" @click="fire">Fire Spike</button>
            <button class="tb-btn auto" :class="{ active: auto }" @click="toggleAuto">Auto {{ auto ? 'ON' : 'OFF' }}</button>
        </div>
        <div class="hint">
            +{{ ms }} ms on the next {{ ticks }} move{{ ticks === 1 ? '' : 's' }}
            <template v-if="auto"><br>auto-fires when both snakes are constrained (gate-open samples)</template>
        </div>
    </div>
</template>

<script>
import { ref, watch } from 'vue';

export default {
    name: 'SpikeInjector',
    emits: ['fire', 'auto'],
    setup(_, { emit }) {
        const ms    = ref(200);
        const ticks = ref(1);
        const auto  = ref(false);

        const clamp = () => ({ ms: Math.max(0, ms.value || 0), ticks: Math.max(1, ticks.value || 1) });
        const fire = () => emit('fire', clamp());
        const toggleAuto = () => { auto.value = !auto.value; };

        // Keep the parent's auto-spike config in sync with the toggle and inputs.
        watch([auto, ms, ticks], () => emit('auto', { enabled: auto.value, ...clamp() }));

        return { ms, ticks, auto, fire, toggleAuto };
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
.btn-row { display: flex; gap: 8px; }
.btn-row .tb-btn { flex: 1; width: auto; }
.tb-btn.auto.active {
    background: #d97706;
    border-color: #d97706;
    color: #fff;
}
.hint {
    margin-top: 8px;
    font-size: 11px;
    color: var(--tb-muted-2);
    font-family: var(--tb-font-mono);
    text-align: center;
}
</style>
