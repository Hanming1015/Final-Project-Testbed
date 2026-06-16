<template>
    <div class="latency-display">
        <div class="panel-title">网络延迟</div>
        <div class="stat">
            <span class="label">RTT</span>
            <span class="value" :style="{ color: rttColor }">{{ rtt.toFixed(1) }} ms</span>
        </div>
        <div class="stat">
            <span class="label">注入</span>
            <span class="value">{{ injected }} ms</span>
        </div>
        <div class="divider"></div>
        <div class="stat">
            <span class="label">有效</span>
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

        const rttColor      = computed(() => colorFor(props.rtt));
        const effectiveColor = computed(() => colorFor(effective.value));

        return { effective, rttColor, effectiveColor };
    }
}
</script>

<style scoped>
.latency-display {
    position: fixed;
    top: 20px;
    left: 20px;
    background: rgba(0, 0, 0, 0.75);
    color: #fff;
    padding: 10px 14px;
    border-radius: 8px;
    font-family: monospace;
    font-size: 13px;
    z-index: 1000;
    min-width: 160px;
    user-select: none;
}
.panel-title {
    font-size: 10px;
    color: #aaa;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 8px;
}
.stat {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
}
.label { color: #aaa; }
.value { font-weight: bold; }
.divider {
    border-top: 1px solid #444;
    margin: 6px 0;
}
</style>
