<template>
    <div class="panel sync-monitor">
        <div class="panel-title">Sync Monitor</div>
        <div class="sync-body" ref="body">
            <div v-if="entries.length === 0" class="empty">Waiting for sync data…</div>
            <div v-for="e in entries" :key="e.id" class="sync-row">
                <span class="col tick">#{{ e.tick }}</span>
                <span class="col">step {{ e.step }}</span>
                <span class="col">q{{ e.q }}</span>
                <span class="col">{{ e.fps }}fps</span>
                <span class="col">rtt {{ e.rtt }}</span>
                <span class="col" :class="e.pred ? 'on' : 'off'">{{ e.pred ? 'pred' : 'idle' }}</span>
                <span class="col ahead" :class="aheadClass(e.ahead)">ahead {{ e.ahead }}</span>
            </div>
        </div>
    </div>
</template>

<script>
import { ref, watch, nextTick } from 'vue';

export default {
    name: 'SyncMonitor',
    props: {
        entries: { type: Array, default: () => [] }
    },
    setup(props) {
        const body = ref(null);

        const aheadClass = a => (a > 0 ? 'pos' : a < 0 ? 'neg' : 'zero');

        watch(
            () => props.entries.length,
            async () => {
                await nextTick();
                if (body.value) body.value.scrollTop = body.value.scrollHeight;
            }
        );

        return { body, aheadClass };
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
.sync-body {
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
.sync-row {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 8px;
    padding: 3px 0;
    border-bottom: 1px solid #1e1e22;
    font-size: 11px;
}
.sync-row:last-child { border-bottom: none; }
.col { color: #b9b9c0; }
.col.tick { color: #6f6f78; min-width: 38px; }
.col.on  { color: #4caf50; }
.col.off { color: #555; }
.ahead.pos  { color: #4caf50; }
.ahead.neg  { color: #f44336; }
.ahead.zero { color: #b9b9c0; }
</style>
