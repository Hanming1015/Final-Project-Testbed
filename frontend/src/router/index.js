import NotFound from '@/views/notfound/NotFound.vue'
import PlaygroundIndexView from '@/views/playground/PlaygroundIndexView.vue'
import UserAccountLoginView from '@/views/user/account/UserAccountLoginView.vue'
import UserAccountRegisterView from '@/views/user/account/UserAccountRegisterView.vue'
import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

const routes = [
  {
    name: 'home',
    path: '/',
    redirect: '/playground',
    meta: {
      requiresAuth: true
    }
  },
  {
    name: 'playground',
    path: '/playground',
    component: PlaygroundIndexView,
    meta: {
      requiresAuth: true
    }
  },
  {
    name: 'user_account_login',
    path: '/user/account/login',
    component: UserAccountLoginView,
    meta: {
      requiresAuth: false
    }
  },
  {
    name: 'user_account_register',
    path: '/user/account/register',
    component: UserAccountRegisterView,
    meta: {
      requiresAuth: false
    }
  },
  {
    name: 'notfound',
    path: '/404',
    component: NotFound,
    meta: {
      requiresAuth: false
    }
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

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !store.state.user.is_login) {
    next({'name': 'user_account_login'});
  } else {
    next();
  }
})

export default router
