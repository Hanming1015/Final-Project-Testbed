import NotFound from '@/views/notfound/NotFound.vue'
import PlaygroundIndexView from '@/views/playground/PlaygroundIndexView.vue'
import UserAccountLoginView from '@/views/user/account/UserAccountLoginView.vue'
import UserAccountRegisterView from '@/views/user/account/UserAccountRegisterView.vue'
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    name: 'home',
    path: '/',
    redirect: '/playground'
  },
  {
    name: 'playground',
    path: '/playground',
    component: PlaygroundIndexView
  },
  {
    name: 'user_account_login',
    path: '/user/account/login',
    component: UserAccountLoginView
  },
  {
    name: 'user_account_register',
    path: '/user/account/register',
    component: UserAccountRegisterView
  },
  {
    name: 'notfound',
    path: '/404',
    component: NotFound
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
