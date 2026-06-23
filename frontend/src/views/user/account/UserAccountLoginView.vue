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
                            console.log("User info: ", store.state.user);
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
    font-family: var(--tb-font-ui);
}

.auth-card {
    background-color: var(--tb-card);
    padding: 40px 50px;
    border-radius: 14px;
    border: 1px solid var(--tb-border);
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.auth-title {
    text-align: center;
    color: var(--tb-fg);
    font-weight: 700;
    letter-spacing: -0.01em;
    margin-bottom: 30px;
}

.form-label {
    color: var(--tb-muted);
    font-weight: 500;
    font-size: 13px;
}

.game-input {
    border: 1px solid var(--tb-border);
    border-radius: var(--tb-radius);
    color: var(--tb-fg);
    font-weight: 500;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.game-input:focus {
    border-color: var(--tb-accent);
    box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
    outline: none;
}

.error-message {
    color: var(--tb-danger);
    font-size: 0.9em;
    min-height: 20px;
}

.submit-btn {
    background-color: var(--tb-accent);
    color: #fff;
    font-weight: 600;
    border: 1px solid var(--tb-accent);
    border-radius: var(--tb-radius);
    padding: 10px;
    transition: filter 0.15s, transform 0.05s;
}

.submit-btn:hover {
    filter: brightness(1.07);
}

.submit-btn:active {
    transform: translateY(1px);
}

.game-link {
    color: var(--tb-muted);
    text-decoration: none;
    font-weight: 500;
    transition: color 0.15s;
}

.game-link:hover {
    color: var(--tb-accent);
}
</style>