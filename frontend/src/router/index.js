import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
  { path: '/assessment/:taskId', name: 'assessment', component: () => import('@/views/Assessment.vue') },
  { path: '/report', name: 'report', component: () => import('@/views/Report.vue') },
  { path: '/history', name: 'history', component: () => import('@/views/History.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/stats' },
      { path: 'stats', component: () => import('@/views/admin/AdminStats.vue') },
      { path: 'scales', component: () => import('@/views/admin/AdminScales.vue') },
      { path: 'dialog', component: () => import('@/views/admin/AdminDialog.vue') },
      { path: 'dictionary', component: () => import('@/views/admin/AdminDictionary.vue') },
      { path: 'params', component: () => import('@/views/admin/AdminParams.vue') },
      { path: 'users', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'logs', component: () => import('@/views/admin/AdminLogs.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫
router.beforeEach((to) => {
  const user = useUserStore()
  if (!to.meta.public && !user.isLogin) {
    return { name: 'login' }
  }
  return true
})

export default router
