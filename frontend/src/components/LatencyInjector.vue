<template>
    <div class="panel latency-injector">
        <div class="header">
            <span class="panel-title">Latency Injection</span>
            <button class="toggle-btn" :class="{ active: enabled }" @click="toggle">
                {{ enabled ? 'ON' : 'OFF' }}
            </button>
        </div>

        <div class="slider-wrap" :class="{ disabled: !enabled }">
            <input
                type="range"
                min="0" max="200" step="10"
                :value="sliderVal"
                :disabled="!enabled"
                @input="onInput(Number($event.target.value))"
            />
        </div>

        <div class="value-row">
            <span class="dim">Client Injected</span>
            <span>{{ activeVal }} ms</span>
        </div>
    </div>
</template>

<script>
import { ref, computed } from 'vue';

export default {
    name: 'LatencyInjector',
    props: {
        modelValue: { type: Number, default: 0 }
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
        const enabled   = ref(false);
        const sliderVal = ref(100);

        const activeVal = computed(() => enabled.value ? sliderVal.value : 0);

        const toggle = () => {
            enabled.value = !enabled.value;
            emit('update:modelValue', activeVal.value);
        };

        const onInput = val => {
            sliderVal.value = val;
            if (enabled.value) emit('update:modelValue', val);
        };

        return { enabled, sliderVal, activeVal, toggle, onInput };
    }
}
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
    margin-bottom: 12px;
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
    cursor: pointer;
    font-family: var(--tb-font-ui);
    font-size: 12px;
    font-weight: 600;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.toggle-btn.active {
    background: var(--tb-success);
    border-color: var(--tb-success);
    color: #fff;
}
.slider-wrap {
    margin-bottom: 8px;
    transition: opacity 0.2s;
}
.slider-wrap.disabled { opacity: 0.4; }
.slider-wrap input[type="range"] {
    width: 100%;
    accent-color: var(--tb-accent);
}
.value-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
}
.value-row span:last-child {
    font-weight: 600;
    font-family: var(--tb-font-mono);
}
.dim { color: var(--tb-muted); }
</style>
