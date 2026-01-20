import axios from 'axios'
import authApi from './auth'
import router from '../router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 10000
})

// 请求拦截器 - 自动添加Token
api.interceptors.request.use(
  config => {
    // 从localStorage获取Token
    const token = authApi.getToken()
    
    // 如果Token存在，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理Token过期
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    
    // 处理401未授权错误（Token过期或无效）
    if (error.response && error.response.status === 401) {
      // 清除过期的Token和用户信息
      authApi.removeToken()
      authApi.removeUser()
      
      // 跳转到登录页
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
    }
    
    return Promise.reject(error)
  }
)

// 产品相关API
export const productApi = {
  getAll: () => api.get('/products'),
  create: (data) => api.post('/products', data),
  delete: (id) => api.delete(`/products/${id}`)
}

// 采购相关API
export const purchaseApi = {
  getAll: () => api.get('/purchases'),
  create: (data) => api.post('/purchases', data),
  complete: (id) => api.post(`/purchases/${id}/complete`),
  delete: (id) => api.delete(`/purchases/${id}`)
}

// 订单相关API
export const orderApi = {
  getAll: () => api.get('/orders'),
  getById: (id) => api.get(`/orders/${id}`),
  create: (data) => api.post('/orders', data),
  update: (id, data) => api.put(`/orders/${id}`, data),
  updateStatus: (id, status) => api.patch(`/orders/${id}/status?status=${status}`),
  delete: (id) => api.delete(`/orders/${id}`)
}

// 客户相关API
export const customerApi = {
  getAll: () => api.get('/customers'),
  create: (data) => api.post('/customers', data),
  update: (id, data) => api.put(`/customers/${id}`, data),
  delete: (id) => api.delete(`/customers/${id}`)
}

// 供应商相关API
export const supplierApi = {
  getAll: (activeOnly = false) => api.get('/suppliers', { params: { activeOnly } }),
  getById: (id) => api.get(`/suppliers/${id}`),
  search: (name) => api.get('/suppliers/search', { params: { name } }),
  create: (data) => api.post('/suppliers', data),
  update: (id, data) => api.put(`/suppliers/${id}`, data),
  delete: (id) => api.delete(`/suppliers/${id}`),
  toggleStatus: (id, active) => api.patch(`/suppliers/${id}/status`, { active })
}

// 发票相关API
export const invoiceApi = {
  issue: (data) => api.post('/invoices/issue', data),
  getByOrder: (orderId) => api.get(`/invoices/order/${orderId}`),
  void: (invoiceId, reason) => api.post(`/invoices/${invoiceId}/void`, { reason }),
  getAll: () => api.get('/invoices'),
  getByNumber: (invoiceNumber) => api.get(`/invoices/number/${invoiceNumber}`)
}

// 库存相关API
export const inventoryApi = {
  generateReport: (period) => api.get('/inventory/report/generate', { params: { period } }),
  getReport: (period) => api.get('/inventory/report', { params: { period } }),
  getTransactions: (productId) => api.get(`/inventory/transactions/${productId}`),
  getTransactionsByPeriod: (startDate, endDate) => 
    api.get('/inventory/transactions', { params: { startDate, endDate } }),
  getPeriods: () => api.get('/inventory/report/periods')
}

// 统计相关API
export const statisticsApi = {
  getDashboard: (startDate, endDate) => {
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return api.get('/statistics/dashboard', { params })
  }
}

// 仓库相关API
export const warehouseApi = {
  getAll: () => api.get('/warehouses'),
  getById: (id) => api.get(`/warehouses/${id}`),
  create: (data) => api.post('/warehouses', data),
  update: (id, data) => api.put(`/warehouses/${id}`, data),
  delete: (id) => api.delete(`/warehouses/${id}`),
  toggleStatus: (id) => api.post(`/warehouses/${id}/toggle-status`),
  getLocations: (id) => api.get(`/warehouses/${id}/locations`),
  createLocation: (data) => api.post('/warehouses/locations', data),
  updateLocation: (id, data) => api.put(`/warehouses/locations/${id}`, data),
  deleteLocation: (id) => api.delete(`/warehouses/locations/${id}`)
}

// 库存预警相关API
export const alertApi = {
  getAll: () => api.get('/inventory/alerts'),
  getByType: (type) => api.get('/inventory/alerts/by-type', { params: { type } }),
  getBySeverity: (severity) => api.get('/inventory/alerts/by-severity', { params: { severity } }),
  getStatistics: () => api.get('/inventory/alerts/statistics')
}

export default api
