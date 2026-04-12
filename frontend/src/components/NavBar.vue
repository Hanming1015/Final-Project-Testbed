<template>
    <nav class="navbar navbar-expand-lg game-navbar shadow-sm">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                Testbed
            </a>
            <img src="@/assets/images/logo.svg" alt="logo" width="40" height="40" class="d-inline-block align-text-top me-2 logo-glow">
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarText"
                aria-controls="navbarText" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarText">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <router-link class="nav-link fw-semibold" active-class="active" :to="{ name: 'playground' }">Playground</router-link>
                    </li>
                </ul>
                <ul class="navbar-nav" v-if="!$store.state.user.is_login">
                    <li class="nav-item">
                        <router-link class="nav-link fw-semibold" active-class="active" :to="{ name: 'user_account_login' }">Login</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link fw-semibold" active-class="active" :to="{ name: 'user_account_register' }">Register</router-link>
                    </li>
                </ul>
                <ul class="navbar-nav" v-else>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle fw-semibold" href="#" role="button" data-bs-toggle="dropdown"
                            aria-expanded="false">
                            {{ $store.state.user.username }}
                        </a>
                        <ul class="dropdown-menu game-dropdown">
                            <li><a class="dropdown-item" href="#" @click="logout">Logout</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
</template>

<script>
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';

export default {
    name: 'NavBar',
    setup() {
        const store = useStore();
        const router = useRouter();

        const logout = () => {
            store.dispatch("logout");
            router.push({ name: 'user_account_login' });
        }

        return {
            logout
        }
    }
}
</script>

<style scoped>
.game-navbar {
    background-color: #FFFFFF;
    border-bottom: 3px solid #AAD751;
    box-shadow: 0 4px 15px rgba(170, 215, 81, 0.15);
}

.game-navbar .navbar-brand {
    color: #2C3E2D;
    font-size: 1.5rem;
    letter-spacing: 1px;
}

.game-navbar .nav-link {
    color: #6B7D6C;
    transition: all 0.3s ease;
    font-weight: 500;
}

.game-navbar .nav-link:hover {
    color: #89B92B;
}

.game-navbar .nav-link.active {
    color: #89B92B;
    font-weight: 700;
}

.logo-glow {
    filter: drop-shadow(0 0 2px rgba(170, 215, 81, 0.6));
}

.game-dropdown {
    background-color: #FFFFFF;
    border: 1px solid #AAD751;
    box-shadow: 0 4px 10px rgba(170, 215, 81, 0.15);
}

.game-dropdown .dropdown-item {
    color: #2C3E2D;
    transition: all 0.2s;
}

.game-dropdown .dropdown-item:hover {
    background-color: #AAD751;
    color: #ffffff;
    font-weight: bold;
}
</style>