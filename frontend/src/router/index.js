import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/practice', name: 'practice', component: () => import('../views/PracticeView.vue') },
  { path: '/exam', name: 'exam', component: () => import('../views/ExamView.vue') },
  { path: '/wrong', name: 'wrong', component: () => import('../views/WrongView.vue') },
  { path: '/favorites', name: 'favorites', component: () => import('../views/FavoriteView.vue') },
  { path: '/statistics', name: 'statistics', component: () => import('../views/StatisticsView.vue') },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminView.vue'),
    meta: { admin: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：需要登录的页面对未登录用户跳转到登录页
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.guest && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    return { name: 'home' }
  }
  // 管理页要求 ADMIN 角色：前端拦截只是为了体验，
  // 真正的权限校验在后端每个接口里都做了一次
  if (to.meta.admin && auth.user?.role !== 'ADMIN') {
    return { name: 'home' }
  }
  return true
})

export default router