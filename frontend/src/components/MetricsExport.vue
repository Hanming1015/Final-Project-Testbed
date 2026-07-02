<template>
    <div class="panel metrics-export">
        <div class="header">
            <span class="panel-title">Metrics Log</span>
            <button class="reset-btn" @click="$emit('reset')">Reset</button>
        </div>
        <div class="counts">
            <div class="count"><span class="dim">Games</span><span class="val">{{ summary.games }}</span></div>
            <div class="count"><span class="dim">Ticks</span><span class="val">{{ summary.ticks }}</span></div>
            <div class="count"><span class="dim">Renders</span><span class="val">{{ summary.renders }}</span></div>
            <div class="count"><span class="dim">Verifies</span><span class="val">{{ summary.verifies }}</span></div>
            <div class="count"><span class="dim">Overdue</span><span class="val">{{ summary.overdues }}</span></div>
            <div class="count"><span class="dim">Infers</span><span class="val">{{ summary.infers }}</span></div>
        </div>
        <div class="actions">
            <button @click="$emit('export', 'json')">JSON (all)</button>
            <button @click="$emit('export', 'verifies')">Verifies CSV</button>
            <button @click="$emit('export', 'renders')">Renders CSV</button>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MetricsExport',
    props: {
        summary: {
            type: Object,
            default: () => ({ games: 0, ticks: 0, renders: 0, verifies: 0, overdues: 0, infers: 0 })
        }
    },
    emits: ['export', 'reset']
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
.reset-btn {
    background: var(--tb-border-2);
    color: var(--tb-muted);
    border: 1px solid var(--tb-border);
    border-radius: 6px;
    padding: 2px 10px;
    cursor: pointer;
    font-family: var(--tb-font-ui);
    font-size: 11px;
    font-weight: 600;
}
.reset-btn:hover { color: var(--tb-fg); }
.counts {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 6px;
    margin-bottom: 12px;
}
.count {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
}
.count .dim { color: var(--tb-muted); font-size: 10px; text-transform: uppercase; letter-spacing: 0.04em; }
.count .val { color: var(--tb-fg); font-weight: 600; font-family: var(--tb-font-mono); font-size: 14px; }
.actions {
    display: flex;
    gap: 8px;
}
.actions button {
    flex: 1;
    background: var(--tb-accent);
    color: #fff;
    border: 1px solid var(--tb-accent);
    border-radius: 6px;
    padding: 5px 0;
    cursor: pointer;
    font-family: var(--tb-font-ui);
    font-size: 11px;
    font-weight: 600;
    transition: opacity 0.15s;
}
.actions button:hover { opacity: 0.85; }
</style>
