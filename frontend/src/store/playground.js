
export default {
    state: {
        status: "matching", // matching, waiting, playing
        socket: null,
        opponent_username: "",
        gamemap: null
    },
    getters: {
    },
    mutations: {
        updateSocket(state, socket) {
            state.socket = socket;
        },
        updateStatus(state, status) {
            state.status = status;
        },
        updateOpponent(state, opponent) {
            state.opponent_username = opponent.username;
        },
        updateGamemap(state, gamemap) {
            state.gamemap = gamemap;
        }
    },
    actions: {
      
    },
    modules: {
    }
}
