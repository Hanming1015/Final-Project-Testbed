import NotFound from '@/views/notfound/NotFound.vue'
import PlaygroundIndexView from '@/views/playground/PlaygroundIndexView.vue'
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
