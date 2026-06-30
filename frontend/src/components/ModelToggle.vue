<template>
    <div class="panel model-toggle">
        <div class="header">
            <span class="panel-title">Prediction Model</span>
            <button class="toggle-btn" :class="{ active: modelValue }" @click="toggle">
                {{ modelValue ? 'ON' : 'OFF' }}
            </button>
        </div>
        <div class="status">
            <span class="dim">Status</span>
            <span :class="modelValue ? 'active-text' : 'silent-text'">
                {{ modelValue ? 'Active' : 'Silent' }}
            </span>
        </div>
    </div>
</template>

<script>
export default {
    name: 'ModelToggle',
    props: {
        modelValue: { type: Boolean, default: true }
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
        const toggle = () => emit('update:modelValue', !props.modelValue);
        return { toggle };
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
    cursor: pointer;
    font-family: var(--tb-font-ui);
    font-size: 12px;
    font-weight: 600;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.toggle-btn.active {
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
