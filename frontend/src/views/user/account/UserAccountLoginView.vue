<template>
    <div class="row justify-content-md-center auth-container">
        <div class="col-md-4 col-sm-8 auth-card">
            <h2 class="auth-title">Login</h2>
            <form @submit.prevent="login">
                <div class="mb-4">
                    <label for="username" class="form-label">Username</label>
                    <input v-model="username" type="text" class="form-control game-input" id="username" placeholder="Your username">
                </div>
                <div class="mb-4">
                    <label for="password" class="form-label">Password</label>
                    <input v-model="password" type="password" class="form-control game-input" id="password" placeholder="Your password">
                </div>
                <div class="error-message">{{ error_message }}</div>
                <button type="submit" class="btn submit-btn w-100 mt-2">Sign In</button>
                <div class="text-center mt-4">
                    <router-link :to="{ name: 'user_account_register' }" class="game-link">Don't have an account? Register</router-link>
                </div>
            </form>
        </div>
    </div>
</template>

<script>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';

export default {
    name: 'UserAccountLoginView',
    setup() {
        let username = ref('');
        let password = ref('');
        let error_message = ref('');
        const router = useRouter();
        const store = useStore();

        const token = localStorage.getItem("jwt_token");
        if (token) {
            store.commit("updateToken", token);
            store.dispatch("getinfo", {
                success() {
                    router.push({ name: 'playground' });
                    console.log(store.state.user);
                },
                error() {
                    localStorage.removeItem("jwt_token");
                    store.commit("updateToken", "");
                    store.commit("updateUser", {
                        id: "",
                        username: "",
                        is_login: false
                    });
                }
            });
        }

        const login = () => {
            error_message.value = "";
            
            store.dispatch("login", {
                username: username.value,
                password: password.value,
                success() {
                    store.dispatch("getinfo", {
                        success() {
                            router.push({ name: 'playground' });
                            console.log(store.state.user);
                        },
                        error() {
                            localStorage.removeItem("jwt_token");
                            store.commit("updateToken", "");
                            store.commit("updateUser", {
                                id: "",
                                username: "",
                                is_login: false
                            });
                        }
                    });
                },
                error() {
                    error_message.value = "Invalid username or password.";
                }
            });
        }

        return {
            username,
            password,
            error_message,
            login,
        }
    }
}
</script>

<style scoped>
.auth-container {
    margin-top: 10vh;
}

.auth-card {
    background-color: #FFFFFF;
    padding: 40px 50px;
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
    border-bottom: 4px solid #AAD751;
}

.auth-title {
    text-align: center;
    color: #554A3F;
    font-weight: bold;
    margin-bottom: 30px;
}

.form-label {
    color: #2C3E2D;
    font-weight: 500;
}

.game-input {
    border: 2px solid #EAF2D7;
    border-radius: 8px;
    font-weight: 500;
    transition: all 0.3s;
}

.game-input:focus {
    border-color: #AAD751;
    box-shadow: 0 0 8px rgba(170, 215, 81, 0.3);
    outline: none;
}

.error-message {
    color: #F50057;
    font-size: 0.9em;
    min-height: 20px;
}

.submit-btn {
    background-color: #AAD751;
    color: white;
    font-weight: bold;
    border: none;
    border-radius: 8px;
    padding: 10px;
    transition: background-color 0.2s, transform 0.1s;
}

.submit-btn:hover {
    background-color: #89B92B;
}

.submit-btn:active {
    transform: translateY(2px);
}

.game-link {
    color: #6B7D6C;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
}

.game-link:hover {
    color: #89B92B;
}
</style>