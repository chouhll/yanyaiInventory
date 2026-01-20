import { createRouter, createWebHistory } from 'vue-router'
import authApi from '../api/auth'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue')
  },
  {
    path: '/purchases',
    name: 'Purchases',
    component: () => import('../views/Purchases.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue')
  },
  {
    path: '/customers',
    name: 'Customers',
    component: () => import('../views/Customers.vue')
  },
  {
    path: '/suppliers',
    name: 'Suppliers',
    component: () => import('../views/Suppliers.vue')
  },
  {
    path: '/inventory-report',
    name: 'InventoryReport',
    component: () => import('../views/InventoryReport.vue')
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue')
  },
  {
    path: '/inventory-alerts',
    name: 'InventoryAlerts',
    component: () => import('../views/InventoryAlerts.vue')
  },
  {
    path: '/warehouses',
    name: 'Warehouses',
    component: () => import('../views/Warehouses.vue')
  },
  {
    path: '/financial-reports',
    name: 'FinancialReports',
    component: () => import('../views/FinancialReports.vue')
  },
  {
    path: '/tax-management',
    name: 'TaxManagement',
    component: () => import('../views/TaxManagement.vue')
  },
  {
    path: '/cost-analysis',
    name: 'CostAnalysis',
    component: () => import('../views/CostAnalysis.vue')
  },
  {
    path: '/reconciliation',
    name: 'Reconciliation',
    component: () => import('../views/Reconciliation.vue')
  },
  {
    path: '/data-analytics',
    name: 'DataAnalytics',
    component: () => import('../views/DataAnalytics.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 检查路由是否需要认证（默认所有路由都需要认证，除非明确设置为false）
  const requiresAuth = to.meta.requiresAuth !== false
  
  // 检查是否已登录
  const isAuthenticated = authApi.isAuthenticated()
  
  if (requiresAuth && !isAuthenticated) {
    // 需要认证但未登录，跳转到登录页
    next({
      path: '/login',
      query: { redirect: to.fullPath } // 保存原始路径，登录后可以跳回
    })
  } else if (to.path === '/login' && isAuthenticated) {
    // 已登录用户访问登录页，重定向到首页
    next('/')
  } else {
    // 放行
    next()
  }
})

export default router
