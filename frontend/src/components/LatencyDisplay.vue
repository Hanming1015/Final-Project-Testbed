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
            if (ms < 100) return '#4caf50';
            if (ms < 200) return '#ff9800';
            return '#f44336';
        };

        const rttColor       = computed(() => colorFor(props.rtt));
        const effectiveColor = computed(() => colorFor(effective.value));

        return { effective, rttColor, effectiveColor };
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
.stat {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
}
.label { color: #8a8a92; }
.value { font-weight: bold; }
.divider {
    border-top: 1px solid #2c2c34;
    margin: 6px 0;
}
</style>
