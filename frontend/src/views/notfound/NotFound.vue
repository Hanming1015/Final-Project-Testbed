<template>
    <div class="error-container">
        <h1 class="error-code">404</h1>
        <p class="error-msg">Page Not Found</p>
        
        <router-link :to="{ name: 'playground' }" class="back-btn">
            Back to Game
        </router-link>
    </div>
</template>

<script>
export default {
    name: 'NotFound'
}
</script>

<style scoped>
.error-container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 70vh;
    text-align: center;
}

.error-code {
    font-size: 6rem;
    font-weight: bold;
    color: #554A3F;
    margin: 0;
    text-shadow: 2px 2px 0px rgba(0, 0, 0, 0.1);
}

.error-msg {
    font-size: 1.5rem;
    color: #2C3E2D;
    margin-bottom: 30px;
}

.back-btn {
    background-color: #AAD751;
    color: white;
    padding: 12px 30px;
    border-radius: 8px;
    text-decoration: none;
    font-weight: bold;
    font-size: 1.1rem;
    transition: background-color 0.2s;
}

.back-btn:hover {
    background-color: #89B92B;
}
</style>