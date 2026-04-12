<template>
    <div class="row justify-content-md-center auth-container">
        <div class="col-md-4 col-sm-8 auth-card">
            <h2 class="auth-title">Register</h2>
            <form @submit.prevent="register">
                <div class="mb-3">
                    <label for="username" class="form-label">Username</label>
                    <input v-model="username" type="text" class="form-control game-input" id="username" placeholder="Choose a username">
                </div>
                <div class="mb-3">
                    <label for="password" class="form-label">Password</label>
                    <input v-model="password" type="password" class="form-control game-input" id="password" placeholder="Create a password">
                </div>
                <div class="mb-3">
                    <label for="confirmedPassword" class="form-label">Confirm Password</label>
                    <input v-model="confirmedPassword" type="password" class="form-control game-input" id="confirmedPassword" placeholder="Confirm your password">
                </div>
                <div class="error-message">{{ error_message }}</div>
                <button type="submit" class="btn submit-btn w-100 mt-2">Sign Up</button>
                <div class="text-center mt-4">
                    <router-link :to="{ name: 'user_account_login' }" class="game-link">Already have an account? Login</router-link>
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
    name: 'UserAccountRegisterView',
    setup() {
        let username = ref('');
        let password = ref('');
        let confirmedPassword = ref('');
        let error_message = ref('');
        const router = useRouter();
        const store = useStore();

        const register = () => {
            error_message.value = "";
            
            if (password.value !== confirmedPassword.value) {
                error_message.value = "Passwords do not match.";
                return;
            }

            store.dispatch("register", {
                username: username.value,
                password: password.value,
                confirmedPassword: confirmedPassword.value,
                success() {
                    router.push({ name: 'user_account_login' }); 
                },
                error(msg) {
                    error_message.value = msg;
                }
            });
        }

        return {
            username,
            password,
            confirmedPassword,
            error_message,
            register,
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