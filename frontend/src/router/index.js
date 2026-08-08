import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/query/account',
    children: [
      { path: 'basic/community', component: () => import('../views/basic/Community.vue'), meta: { title: '小区管理' } },
      { path: 'basic/building', component: () => import('../views/basic/Building.vue'), meta: { title: '楼栋管理' } },
      { path: 'basic/house', component: () => import('../views/basic/House.vue'), meta: { title: '房屋管理' } },
      { path: 'basic/owner', component: () => import('../views/basic/Owner.vue'), meta: { title: '业主管理' } },
      { path: 'basic/standard', component: () => import('../views/basic/Standard.vue'), meta: { title: '缴存标准' } },
      { path: 'deposit', component: () => import('../views/deposit/Deposit.vue'), meta: { title: '资金缴存' } },
      { path: 'use', component: () => import('../views/use/UseApply.vue'), meta: { title: '资金使用' } },
      { path: 'refund', component: () => import('../views/refund/RefundApply.vue'), meta: { title: '资金退款' } },
      { path: 'query/account', component: () => import('../views/query/AccountQuery.vue'), meta: { title: '账户查询' } },
      { path: 'query/flow', component: () => import('../views/query/FlowQuery.vue'), meta: { title: '流水查询' } },
      { path: 'query/statistics', component: () => import('../views/query/Statistics.vue'), meta: { title: '统计报表' } },
      { path: 'workflow', component: () => import('../views/workflow/FlowConfig.vue'), meta: { title: '流程配置' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
  if (to.path === '/login' && auth.token) {
    return '/'
  }
})

export default router
