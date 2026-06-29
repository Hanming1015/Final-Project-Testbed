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
.sync-body {
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
.sync-row {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 8px;
    padding: 4px 0;
    border-bottom: 1px solid var(--tb-border-2);
    font-size: 11px;
    font-family: var(--tb-font-mono);
}
.sync-row:last-child { border-bottom: none; }
.col { color: var(--tb-muted); }
.col.tick { color: var(--tb-muted-2); min-width: 38px; }
.col.on  { color: var(--tb-success); }
.col.off { color: var(--tb-muted-2); }
.ahead.pos  { color: var(--tb-success); }
.ahead.neg  { color: var(--tb-danger); }
.ahead.zero { color: var(--tb-muted); }
</style>
