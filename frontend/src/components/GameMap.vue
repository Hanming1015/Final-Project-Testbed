<template>
    <div ref="parent" class="gamemap">
        <canvas ref="canvas" tabindex="0"></canvas>
    </div>
</template>

<script>
import { GameMap } from '@/assets/scripts/GameMap';
import { ref, onMounted, onUnmounted } from 'vue';
import { useStore } from 'vuex';

export default {
    name: 'GameMap',
    setup() {
        const store = useStore();
        let parent = ref(null);
        let canvas = ref(null);
        let game = null;

        onMounted(() => {
            game = new GameMap(canvas.value.getContext('2d'), parent.value, store);
            store.commit('updateGameObject', game);
        });

        onUnmounted(() => {
            if (game) { game.destroy(); game = null; }
        });

        return { parent, canvas };
    }
}
</script>

<style scoped>
.gamemap {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}
</style>