<template>
    <div class="panel latency-display">
        <div class="panel-title">Network Latency</div>
        <div class="stat">
            <span class="label">RTT</span>
            <span class="value" :style="{ color: rttColor }">{{ rtt.toFixed(1) }} ms</span>
        </div>
        <div class="stat">
            <span class="label">Injected</span>
            <span class="value">{{ injected }} ms</span>
        </div>
        <div class="divider"></div>
        <div class="stat">
            <span class="label">Effective</span>
            <span class="value" :style="{ color: effectiveColor }">{{ effective.toFixed(1) }} ms</span>
        </div>
    </div>
</template>

<script>
import { computed } from 'vue';

export default {
    name: 'LatencyDisplay',
    props: {
        rtt:      { type: Number, default: 0 },
        injected: { type: Number, default: 0 }
    },
    setup(props) {
        const effective = computed(() => props.rtt + props.injected);

        const colorFor = ms => {
            if (ms < 100) return '#16a34a';
            if (ms < 200) return '#d97706';
            return '#dc2626';
        };

        const rttColor       = computed(() => colorFor(props.rtt));
        const effectiveColor = computed(() => colorFor(effective.value));

        return { effective, rttColor, effectiveColor };
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
.stat {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
}
.label { color: var(--tb-muted); }
.value {
    font-weight: 600;
    font-family: var(--tb-font-mono);
}
.divider {
    border-top: 1px solid var(--tb-border-2);
    margin: 8px 0;
}
</style>
