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
    background-color: #2b2b2b;
    background-image: 
        linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
    background-size: 30px 30px;
    margin: 0;
    color: white;
}
</style>
