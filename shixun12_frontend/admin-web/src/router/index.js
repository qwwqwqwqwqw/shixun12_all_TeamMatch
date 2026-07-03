import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/board',
    children: [
      {
        path: 'board',
        name: 'Board',
        component: () => import('@/views/board/index.vue'),
        meta: { title: '板块管理' }
      },
      {
        path: 'board/:id/projects',
        name: 'BoardProjects',
        component: () => import('@/views/board/projects.vue'),
        meta: { title: '板块项目' }
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '举报处理' }
      },
      {
        path: 'appeals',
        name: 'Appeals',
        component: () => import('@/views/appeal/index.vue'),
        meta: { title: '申诉处理' }
      },
      {
        path: 'evaluations',
        name: 'Evaluations',
        component: () => import('@/views/evaluation-review/index.vue'),
        meta: { title: '评价复核' }
      },
      {
        path: 'penalties',
        name: 'Penalties',
        component: () => import('@/views/penalty/index.vue'),
        meta: { title: '处罚管理' }
      },
      {
        path: 'credit-audit',
        name: 'CreditAudit',
        component: () => import('@/views/placeholder/index.vue'),
        meta: { title: '信誉分审计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = (to.meta && to.meta.title) || 'TeamMatch 管理后台'

  // 非登录页，检查 token
  if (to.path !== '/login') {
    const token = localStorage.getItem('adminToken')
    if (!token) {
      next('/login')
      return
    }
  }
  next()
})

export default router
