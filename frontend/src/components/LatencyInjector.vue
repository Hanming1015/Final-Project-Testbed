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
    background: rgba(20, 20, 24, 0.92);
    border: 1px solid #2c2c34;
    border-radius: 10px;
    padding: 12px 14px;
    color: #e8e8ea;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
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
    font-size: 10px;
    color: #8a8a92;
    text-transform: uppercase;
    letter-spacing: 1.2px;
}
.toggle-btn {
    background: #33333a;
    color: #8a8a92;
    border: none;
    border-radius: 4px;
    padding: 2px 10px;
    cursor: pointer;
    font-family: inherit;
    font-size: 12px;
    transition: background 0.15s, color 0.15s;
}
.toggle-btn.active {
    background: #4caf50;
    color: #fff;
}
.slider-wrap {
    margin-bottom: 6px;
    transition: opacity 0.2s;
}
.slider-wrap.disabled { opacity: 0.35; }
.slider-wrap input[type="range"] { width: 100%; }
.value-row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
}
.dim { color: #8a8a92; }
</style>
