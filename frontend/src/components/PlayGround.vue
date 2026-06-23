<template>
    <div class="playground">
        <GameMap />
    </div>
</template>

<script>
import GameMap from '@/components/GameMap.vue';

export default {
    components: {
        GameMap
    }
}

</script>

<style scoped>
.playground {
    width: 100%;
    height: 100%;
}
</style>