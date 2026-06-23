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
    background-color: var(--tb-card);
    border-bottom: 1px solid var(--tb-border);
    box-shadow: var(--tb-shadow);
    font-family: var(--tb-font-ui);
}

.game-navbar .navbar-brand {
    color: var(--tb-fg);
    font-size: 1.25rem;
    font-weight: 700;
    letter-spacing: -0.01em;
}

.game-navbar .nav-link {
    color: var(--tb-muted);
    transition: color 0.15s ease;
    font-weight: 500;
}

.game-navbar .nav-link:hover {
    color: var(--tb-fg);
}

.game-navbar .nav-link.active {
    color: var(--tb-accent);
    font-weight: 600;
}

.logo-glow {
    filter: none;
}

.game-dropdown {
    background-color: var(--tb-card);
    border: 1px solid var(--tb-border);
    border-radius: var(--tb-radius);
    box-shadow: var(--tb-shadow);
}

.game-dropdown .dropdown-item {
    color: var(--tb-fg);
    transition: background 0.15s, color 0.15s;
}

.game-dropdown .dropdown-item:hover {
    background-color: var(--tb-accent);
    color: #ffffff;
    font-weight: 600;
}
</style>