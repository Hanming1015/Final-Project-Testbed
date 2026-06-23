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
    background: rgba(20, 20, 24, 0.92);
    border: 1px solid #2c2c34;
    border-radius: 10px;
    padding: 12px 14px;
    color: #e8e8ea;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 13px;
    user-select: none;
}
.panel-title {
    font-size: 10px;
    color: #8a8a92;
    text-transform: uppercase;
    letter-spacing: 1.2px;
    margin-bottom: 10px;
}
.log-body {
    max-height: 200px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #444 transparent;
}
.empty {
    color: #555;
    font-style: italic;
    font-size: 11px;
}
.log-entry {
    display: flex;
    gap: 5px;
    align-items: center;
    padding: 3px 0;
    border-bottom: 1px solid #1e1e22;
}
.log-entry:last-child { border-bottom: none; }
.time   { color: #6f6f78; font-size: 10px; min-width: 70px; }
.player     { color: #8a8a92; font-size: 10px; min-width: 18px; }
.step-badge { color: #b9b9c0; font-size: 9px; min-width: 28px; }
.dir    { color: #fff; font-weight: bold; min-width: 14px; }
.sep    { color: #555; }
.log-entry.pending  .result { color: #8a8a92; }
.log-entry.correct  .result { color: #4caf50; }
.log-entry.rollback .result { color: #f44336; }
.log-entry.expired  .result { color: #555; }
</style>
