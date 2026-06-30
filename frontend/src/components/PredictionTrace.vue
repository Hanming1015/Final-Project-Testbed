<template>
    <div class="panel prediction-trace">
        <div class="panel-title">Prediction Trace</div>
        <div class="trace-body" ref="body">
            <div v-if="entries.length === 0" class="empty">
                No prediction events yet — fire a spike while both snakes are constrained…
            </div>
            <div
                v-for="e in entries"
                :key="e.id"
                class="row"
                :class="e.type === 'predict' ? 'predict' : (e.ok ? 'ok' : 'rollback')"
            >
                <span class="time">{{ e.time }}</span>

                <template v-if="e.type === 'predict'">
                    <span class="tag predict">PREDICT</span>
                    <span class="msg">
                        overdue @{{ e.frontier }} → glide {{ e.count }}:
                        A[{{ e.a.join(' ') }}] B[{{ e.b.join(' ') }}]
                    </span>
                </template>

                <template v-else>
                    <span class="tag" :class="e.ok ? 'ok' : 'rollback'">
                        {{ e.ok ? '✓' : '✗ ROLLBACK' }}
                    </span>
                    <span class="msg">
                        step {{ e.step }} · A {{ e.aPred }}/{{ e.aSrv }} · B {{ e.bPred }}/{{ e.bSrv }}
                    </span>
                    <span class="glide" :class="{ on: e.glided }">
                        {{ e.glided ? '● on-screen' : '○ queued' }}
                    </span>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
import { ref, watch, nextTick } from 'vue';

export default {
    name: 'PredictionTrace',
    props: {
        entries: { type: Array, default: () => [] }
    },
    setup(props) {
        const body = ref(null);
        watch(
            () => props.entries.length,
            async () => {
                await nextTick();
                if (body.value) body.value.scrollTop = body.value.scrollHeight;
            }
        );
        return { body };
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
.trace-body {
    max-height: 220px;
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
.row {
    display: flex;
    gap: 6px;
    align-items: center;
    padding: 4px 0;
    border-bottom: 1px solid var(--tb-border-2);
    font-family: var(--tb-font-mono);
    font-size: 11px;
}
.row:last-child { border-bottom: none; }
.time { color: var(--tb-muted-2); font-size: 10px; min-width: 56px; }
.tag {
    font-weight: 700;
    font-size: 9px;
    padding: 1px 5px;
    border-radius: 4px;
    white-space: nowrap;
}
.tag.predict  { color: #fff; background: var(--tb-accent); }
.tag.ok       { color: var(--tb-success); }
.tag.rollback { color: #fff; background: var(--tb-danger); }
.msg { color: var(--tb-fg); flex: 1 1 auto; }
.row.predict  .msg { color: var(--tb-muted); }
.glide { margin-left: auto; font-size: 9px; color: var(--tb-muted-2); white-space: nowrap; }
.glide.on { color: var(--tb-accent); font-weight: 700; }
</style>
