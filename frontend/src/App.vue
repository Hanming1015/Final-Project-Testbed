<template>
    <div>
        <NavBar />
        <router-view />
    </div>
</template>

<script>
import NavBar from './components/NavBar.vue';
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import { useStore } from 'vuex';

export default {
    name: 'App',
    components: {
        NavBar
    },
    setup() {
        const store = useStore();
        const jwt_token = localStorage.getItem("jwt_token");

        if (jwt_token) {
            store.commit("updateToken", jwt_token);
            store.dispatch("getinfo", {
                success() {},
                error() {
                    store.dispatch("logout");
                }
            })
        }
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
    background-attachment: fixed;
    min-height: 100vh;
    margin: 0;
    color: #2C3E2D;
}
</style>
