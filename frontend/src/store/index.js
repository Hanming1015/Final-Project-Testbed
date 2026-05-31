import { createStore } from 'vuex'
import ModuleUser from './user'
import ModulePlayground from './playground'

export default createStore({
  state: {
  },
  getters: {
  },
  mutations: {
  },
  actions: {
  },
  modules: {
    user: ModuleUser,
    playground: ModulePlayground,
  }
})
