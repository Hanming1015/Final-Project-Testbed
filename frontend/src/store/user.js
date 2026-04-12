import axios from 'axios';

export default {
    state: {
        id: "",
        username: "",
        token: "",
        is_login: false,
    },
    getters: {
    },
    mutations: {
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.is_login = user.is_login;
        },
        updateToken(state, token) {
            state.token = token;
        },
        logout(state) {
            state.id = "";
            state.username = "";
            state.token = "";
            state.is_login = false;
        }
    },
    actions: {
        login(context, data) {
            return axios.post('http://127.0.0.1:3000/api/user/account/login', {
                username: data.username,
                password: data.password
            }).then(resp => {
                const responseData = resp.data;
                if (responseData.error_message === "success") {
                    localStorage.setItem("jwt_token", responseData.token);
                    context.commit("updateToken", responseData.token);
                    // context.commit("updateUser", {
                    //     username: data.username,
                    //     is_login: true,
                    // });
                    data.success(responseData);
                } else {
                    data.error(responseData);
                }
            }).catch(() => {
                data.error("Network error: Connection to the server failed.");
            });
        },
        getinfo (context, data) {
            return axios.get('http://127.0.0.1:3000/api/user/account/info', {
                headers: {
                    'Authorization': `Bearer ${context.state.token}`
                }
            }).then(resp => {
                const responseData = resp.data;
                if (responseData) {
                    context.commit("updateUser", {
                        ...responseData,
                        is_login: true
                    });
                    if (data && data.success) data.success();
                } else {
                    if (data && data.error) data.error("Failed to get info.");
                }
            }).catch(() => {
                if (data && data.error) data.error("Network error: Connection to the server failed.");
            });
        },
        register(context, data) {
            return axios.post('http://127.0.0.1:3000/api/user/account/register', {
                username: data.username,
                password: data.password,
                confirmedPassword: data.confirmedPassword
            }).then(resp => {
                const responseData = resp.data;
                if (responseData && responseData.id !== undefined) {
                    data.success();
                } else if (responseData.error_message === "success" || typeof responseData === 'string') {
                     data.success();
                } else if (responseData.error_message) {
                    data.error(responseData.error_message);
                } else {
                    data.success();
                }
            }).catch(() => {
                data.error("Network error: Connection to the server failed.");
            });
        },
        logout(context) {
            console.log("Logging out...");
            localStorage.removeItem("jwt_token");
            context.commit("logout");
        }
    },
    modules: {
    }
}
