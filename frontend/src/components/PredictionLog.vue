<template>
    <div class="panel prediction-log">
        <div class="panel-title">Prediction Log</div>
        <div class="log-body" ref="logBody">
            <div v-if="entries.length === 0" class="empty">Waiting for predictions…</div>
            <div
                v-for="entry in entries"
                :key="entry.id"
                class="log-entry"
                :class="entry.status"
            >
                <span class="time">{{ entry.time }}</span>
                <span class="player">{{ entry.player }}</span>
                <span class="step-badge">{{ entry.step }}</span>
                <span class="dir">{{ DIR[entry.predicted] }}</span>
                <span class="sep">→</span>
                <span class="result">
                    <template v-if="entry.status === 'pending'">…</template>
                    <template v-else-if="entry.status === 'correct'">✓ correct</template>
                    <template v-else-if="entry.status === 'expired'">— expired</template>
                    <template v-else>✗ rollback (server: {{ DIR[entry.actual] }})</template>
                </span>
            </div>
        </div>
    </div>
</template>

<script>
import { ref, watch, nextTick } from 'vue';

export default {
    name: 'PredictionLog',
    props: {
        entries: { type: Array, default: () => [] }
    },
    setup(props) {
        const DIR = ['↑', '→', '↓', '←'];
        const logBody = ref(null);

        watch(
            () => props.entries.length,
            async () => {
                await nextTick();
                if (logBody.value) logBody.value.scrollTop = logBody.value.scrollHeight;
            }
        );

        return { DIR, logBody };
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
.panel-title {
    font-size: 11px;
    color: var(--tb-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 600;
    margin-bottom: 12px;
}
.log-body {
    max-height: 200px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
    user-select: text;
}
.empty {
    color: var(--tb-muted-2);
    font-style: italic;
    font-size: 12px;
}
.log-entry {
    display: flex;
    gap: 6px;
    align-items: center;
    padding: 4px 0;
    border-bottom: 1px solid var(--tb-border-2);
    font-family: var(--tb-font-mono);
}
.log-entry:last-child { border-bottom: none; }
.time   { color: var(--tb-muted-2); font-size: 10px; min-width: 70px; }
.player     { color: var(--tb-muted); font-size: 10px; min-width: 18px; }
.step-badge { color: var(--tb-muted); font-size: 9px; min-width: 28px; }
.dir    { color: var(--tb-fg); font-weight: 700; min-width: 14px; }
.sep    { color: var(--tb-muted-2); }
.log-entry.pending  .result { color: var(--tb-muted); }
.log-entry.correct  .result { color: var(--tb-success); }
.log-entry.rollback .result { color: var(--tb-danger); }
.log-entry.expired  .result { color: var(--tb-muted-2); }
</style>
