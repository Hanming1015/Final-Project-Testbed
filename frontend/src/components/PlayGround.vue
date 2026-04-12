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
    width: 60vw;
    height: 65vh;
    margin: 80px auto;
    /* border-radius: 20px;
    box-shadow: 
        0 15px 35px rgba(0, 0, 0, 0.15), 
        0 0 0 10px rgba(255, 255, 255, 0.4);
    overflow: hidden; 
    background-color: #554A3F;  */
}
</style>