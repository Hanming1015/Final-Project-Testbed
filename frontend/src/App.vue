<template>
    <div>
        <NavBar />
        <router-view />
    </div>
</template>

<script>
import NavBar from './components/NavBar.vue';
import "bootstrap/dist/css/bootstrap.min.css";


export default {
    name: 'App',
    components: {
        NavBar
    }
}
</script>

<style>
body {
    background-color: #EAF2D7;
    background-image: repeating-linear-gradient(
        -45deg,
        #E3ECD0,
        #E3ECD0 20px,
        #EAF2D7 20px,
        #EAF2D7 40px
    );
    background-attachment: fixed; /* 固定背景，防止滚动或自适应高度导致纹理错位截断 */
    min-height: 100vh;
    margin: 0;
    color: #2C3E2D;
}
</style>
