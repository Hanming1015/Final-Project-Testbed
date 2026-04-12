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
                <ul class="navbar-nav">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle fw-semibold" href="#" role="button" data-bs-toggle="dropdown"
                            aria-expanded="false">
                            Hanming
                        </a>
                        <ul class="dropdown-menu game-dropdown">
                            <li><a class="dropdown-item" href="#">Logout</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
</template>

<script>

export default {
    name: 'NavBar',
    setup() {

    }
}
</script>

<style scoped>
.game-navbar {
    background-color: #1e1e1e; /* 深灰色背景匹配游戏风 */
    border-bottom: 3px solid #AAD751; /* 底部使用游戏地图中浅绿色的亮边 */
}

.game-navbar .navbar-brand {
    color: #ffffff;
    font-size: 1.5rem;
    letter-spacing: 1px;
}

.game-navbar .nav-link {
    color: rgba(255, 255, 255, 0.75);
    transition: color 0.2s;
}

.game-navbar .nav-link:hover {
    color: #AAD751;
}

.game-navbar .nav-link.active {
    color: #A2D149;
    text-shadow: 0 0 5px rgba(162, 209, 73, 0.5); /* 微微的发光效果 */
}

.logo-glow {
    filter: drop-shadow(0 0 3px #AAD751);
}

.game-dropdown {
    background-color: #2b2b2b;
    border: 1px solid #444;
}

.game-dropdown .dropdown-item {
    color: white;
}

.game-dropdown .dropdown-item:hover {
    background-color: #AAD751;
    color: #1e1e1e;
    font-weight: bold;
}
</style>