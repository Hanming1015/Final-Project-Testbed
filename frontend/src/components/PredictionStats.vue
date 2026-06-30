<template>
    <div class="panel prediction-stats">
        <div class="panel-title">Prediction Stats</div>
        <div class="stats-body">
            <div class="row">
                <span class="label">Glides</span>
                <span class="val">{{ stats.glides }}</span>
                <span class="hint">times prediction kicked in</span>
            </div>
            <div class="row">
                <span class="label">Verified steps</span>
                <span class="val">{{ stats.verified }}</span>
                <span class="hint">predicted steps the server confirmed</span>
            </div>

            <div class="divider"></div>

            <div class="row strong">
                <span class="label">Accuracy</span>
                <span class="val">{{ pct(stats.correct, stats.verified) }}</span>
                <span class="hint">{{ stats.correct }}/{{ stats.verified }} steps both right</span>
            </div>
            <div class="row sub">
                <span class="label">Snake A</span>
                <span class="val">{{ pct(stats.aOk, stats.verified) }}</span>
                <span class="hint">{{ stats.aOk }}/{{ stats.verified }}</span>
            </div>
            <div class="row sub">
                <span class="label">Snake B</span>
                <span class="val">{{ pct(stats.bOk, stats.verified) }}</span>
                <span class="hint">{{ stats.bOk }}/{{ stats.verified }}</span>
            </div>

            <div class="divider"></div>

            <div class="row">
                <span class="label good">Masked</span>
                <span class="val good">{{ pct(stats.masked, stats.verified) }}</span>
                <span class="hint">correct &amp; on-screen → latency hidden</span>
            </div>
            <div class="row">
                <span class="label bad">Visible rollback</span>
                <span class="val bad">{{ pct(stats.visibleRb, stats.verified) }}</span>
                <span class="hint">wrong &amp; on-screen → visible correction</span>
            </div>
            <div class="row">
                <span class="label">Expired</span>
                <span class="val">{{ stats.expired }}</span>
                <span class="hint">dropped by an earlier rollback</span>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'PredictionStats',
    props: {
        stats: {
            type: Object,
            default: () => ({
                glides: 0, verified: 0, correct: 0, rollback: 0,
                onScreen: 0, masked: 0, visibleRb: 0, expired: 0, aOk: 0, bOk: 0
            })
        }
    },
    setup() {
        const pct = (n, d) => (d > 0 ? Math.round((100 * n) / d) + '%' : '—');
        return { pct };
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
    user-select: text;
}
.panel-title {
    font-size: 11px;
    color: var(--tb-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 600;
    margin-bottom: 12px;
}
.row {
    display: flex;
    align-items: baseline;
    gap: 8px;
    padding: 3px 0;
}
.row.sub .label { padding-left: 12px; color: var(--tb-muted-2); }
.row.strong .label,
.row.strong .val { font-weight: 700; }
.label { min-width: 110px; color: var(--tb-muted); }
.val   { min-width: 46px; text-align: right; font-family: var(--tb-font-mono); font-weight: 600; }
.hint  { font-size: 10px; color: var(--tb-muted-2); }
.label.good, .val.good { color: var(--tb-success); }
.label.bad,  .val.bad  { color: var(--tb-danger); }
.divider { height: 1px; background: var(--tb-border-2); margin: 6px 0; }
</style>
