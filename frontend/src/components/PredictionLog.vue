<template>
    <div class="prediction-log">
        <div class="panel-title">预测日志</div>
        <div class="log-body" ref="logBody">
            <div v-if="entries.length === 0" class="empty">等待预测触发…</div>
            <div
                v-for="entry in entries"
                :key="entry.id"
                class="log-entry"
                :class="entry.status"
            >
                <span class="time">{{ entry.time }}</span>
                <span class="player">{{ entry.player }}</span>
                <span class="dir">{{ DIR[entry.predicted] }}</span>
                <span class="sep">→</span>
                <span class="result">
                    <template v-if="entry.status === 'pending'">…</template>
                    <template v-else-if="entry.status === 'correct'">✓ 正确</template>
                    <template v-else>✗ 回滚 (服务器: {{ DIR[entry.actual] }})</template>
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
.prediction-log {
    position: fixed;
    bottom: 20px;
    left: 20px;
    background: rgba(0, 0, 0, 0.75);
    color: #fff;
    padding: 10px 14px;
    border-radius: 8px;
    font-family: monospace;
    font-size: 12px;
    z-index: 1000;
    width: 300px;
    user-select: none;
}
.panel-title {
    font-size: 10px;
    color: #aaa;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 8px;
}
.log-body {
    max-height: 180px;
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
    border-bottom: 1px solid #1e1e1e;
}
.log-entry:last-child { border-bottom: none; }
.time   { color: #555; font-size: 10px; min-width: 70px; }
.player { color: #888; font-size: 10px; min-width: 18px; }
.dir    { color: #fff; font-weight: bold; min-width: 14px; }
.sep    { color: #444; }
.log-entry.pending  .result { color: #888; }
.log-entry.correct  .result { color: #4caf50; }
.log-entry.rollback .result { color: #f44336; }
</style>
