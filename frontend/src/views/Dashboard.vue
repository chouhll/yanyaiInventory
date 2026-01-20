<template>
  <div class="dashboard-page">
    <!-- 筛选器 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="24" :md="10">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <el-button type="primary" @click="loadStatistics">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
          <el-button @click="setToday">今天</el-button>
          <el-button @click="setThisWeek">本周</el-button>
          <el-button @click="setThisMonth">本月</el-button>
        </el-col>
        <el-col :xs="24" :sm="24" :md="6" style="text-align: right; color: #909399; font-size: 13px;">
          <span v-if="stats.startDate">
            {{ stats.startDate }} ~ {{ stats.endDate }}
          </span>
        </el-col>
      </el-row>
    </el-card>

    <!-- 财务指标卡片 -->
    <el-row :gutter="20" style="margin-top: 20px;" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card revenue-card">
          <div class="stat-icon">💰</div>
          <div class="stat-content">
            <div class="stat-label">销售收入</div>
            <div class="stat-value">¥{{ formatMoney(stats.salesRevenue) }}</div>
            <div class="stat-detail">期间订单: {{ stats.periodOrders || 0 }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card profit-card">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <div class="stat-label">毛利润</div>
            <div class="stat-value">¥{{ formatMoney(stats.grossProfit) }}</div>
            <div class="stat-detail">毛利率: {{ formatPercent(stats.grossMargin) }}%</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card pending-card">
          <div class="stat-icon">⏰</div>
          <div class="stat-content">
            <div class="stat-label">待收款</div>
            <div class="stat-value">¥{{ formatMoney(stats.pendingPayment) }}</div>
            <div class="stat-detail">需跟进收款</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card invoice-card">
          <div class="stat-icon">🧾</div>
          <div class="stat-content">
            <div class="stat-label">开票金额</div>
            <div class="stat-value">¥{{ formatMoney(stats.invoicedAmount) }}</div>
            <div class="stat-detail">税额: ¥{{ formatMoney(stats.taxAmount) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细统计 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600;">📊 业务统计</span>
            </div>
          </template>
          <div class="detail-stats">
            <div class="stat-item">
              <span class="label">期间订单数</span>
              <span class="value">{{ stats.periodOrders || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">期间采购数</span>
              <span class="value">{{ stats.periodPurchases || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">期间发票数</span>
              <span class="value">{{ stats.periodInvoices || 0 }}</span>
            </div>
            <el-divider />
            <div class="stat-item">
              <span class="label">产品总数</span>
              <span class="value">{{ stats.totalProducts || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="label">客户总数</span>
              <span class="value">{{ stats.totalCustomers || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600;">💵 成本与利润</span>
            </div>
          </template>
          <div class="detail-stats">
            <div class="stat-item highlight">
              <span class="label">销售收入</span>
              <span class="value success">¥{{ formatMoney(stats.salesRevenue) }}</span>
            </div>
            <div class="stat-item">
              <span class="label">采购成本</span>
              <span class="value">¥{{ formatMoney(stats.purchaseCost) }}</span>
            </div>
            <div class="stat-item highlight">
              <span class="label">毛利润</span>
              <span class="value" :class="stats.grossProfit >= 0 ? 'success' : 'danger'">
                ¥{{ formatMoney(stats.grossProfit) }}
              </span>
            </div>
            <div class="stat-item">
              <span class="label">毛利率</span>
              <span class="value">{{ formatPercent(stats.grossMargin) }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订单状态分布 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600;">📋 订单状态分布</span>
            </div>
          </template>
          <div class="status-stats">
            <div v-for="(count, status) in stats.ordersByStatus" :key="status" class="status-item">
              <el-tag :type="getStatusType(status)" size="large">
                {{ getStatusText(status) }}
              </el-tag>
              <span class="status-count">{{ count }}</span>
            </div>
            <div v-if="!stats.ordersByStatus || Object.keys(stats.ordersByStatus).length === 0" class="empty-hint">
              期间内无订单
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span style="font-weight: 600;">📦 库存快览</span>
              <el-button type="primary" link @click="$router.push('/products')">
                查看详情
              </el-button>
            </div>
          </template>
          <el-table :data="recentProducts" style="width: 100%" size="small">
            <el-table-column prop="name" label="产品名称" min-width="120" />
            <el-table-column prop="stock" label="库存" width="80" align="right">
              <template #default="{ row }">
                <span :class="row.stock < 10 ? 'low-stock' : ''">
                  {{ row.stock || 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">
                ¥{{ row.price?.toFixed(2) || '0.00' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近订单 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span style="font-weight: 600;">📝 最近订单</span>
          <el-button type="primary" link @click="$router.push('/orders')">
            查看全部
          </el-button>
        </div>
      </template>
      <el-table :data="stats.recentOrders" style="width: 100%">
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column label="客户" width="150">
          <template #default="{ row }">
            {{ row.customer?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="订单日期" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.orderDate) }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            ¥{{ calculateOrderTotal(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/orders/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { statisticsApi, productApi } from '../api'

const loading = ref(false)
const dateRange = ref([])
const recentProducts = ref([])

const stats = ref({
  totalProducts: 0,
  totalCustomers: 0,
  totalOrders: 0,
  totalPurchases: 0,
  periodOrders: 0,
  periodPurchases: 0,
  periodInvoices: 0,
  salesRevenue: 0,
  purchaseCost: 0,
  grossProfit: 0,
  grossMargin: 0,
  invoicedAmount: 0,
  taxAmount: 0,
  pendingPayment: 0,
  ordersByStatus: {},
  recentOrders: [],
  startDate: '',
  endDate: ''
})

const loadStatistics = async () => {
  loading.value = true
  try {
    let startDate, endDate
    if (dateRange.value && dateRange.value.length === 2) {
      startDate = dateRange.value[0]
      endDate = dateRange.value[1]
    }
    
    const data = await statisticsApi.getDashboard(startDate, endDate)
    stats.value = data
  } catch (error) {
    ElMessage.error('加载统计数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadProducts = async () => {
  try {
    const products = await productApi.getAll()
    recentProducts.value = products.slice(0, 5)
  } catch (error) {
    console.error('加载产品失败')
  }
}

const formatMoney = (value) => {
  if (!value) return '0.00'
  return parseFloat(value).toFixed(2)
}

const formatPercent = (value) => {
  if (!value) return '0.00'
  return parseFloat(value).toFixed(2)
}

const formatDateTime = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const calculateOrderTotal = (order) => {
  if (!order || !order.items) return '0.00'
  const total = order.items.reduce((sum, item) => sum + (item.subtotal || 0), 0)
  return total.toFixed(2)
}

const getStatusType = (status) => {
  const types = {
    'CREATED': 'info',
    'UNPAID': 'warning',
    'PAID': 'success',
    'INVOICED': 'primary',
    'SHIPPED': '',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'CREATED': '已创建',
    'UNPAID': '未付款',
    'PAID': '已付款',
    'INVOICED': '已开票',
    'SHIPPED': '已发货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const setToday = () => {
  const today = new Date()
  const dateStr = today.toISOString().split('T')[0]
  dateRange.value = [dateStr, dateStr]
  loadStatistics()
}

const setThisWeek = () => {
  const today = new Date()
  const firstDay = new Date(today)
  firstDay.setDate(today.getDate() - today.getDay())
  dateRange.value = [
    firstDay.toISOString().split('T')[0],
    today.toISOString().split('T')[0]
  ]
  loadStatistics()
}

const setThisMonth = () => {
  const today = new Date()
  const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
  dateRange.value = [
    firstDay.toISOString().split('T')[0],
    today.toISOString().split('T')[0]
  ]
  loadStatistics()
}

onMounted(() => {
  setThisMonth() // 默认显示本月数据
  loadProducts()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.filter-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
}

.filter-card :deep(.el-card__body) {
  padding: 16px;
}

.filter-card :deep(.el-range-editor) {
  width: 100%;
}

/* 财务指标卡片 */
.stat-card {
  height: 160px;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.stat-card.revenue-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-card.profit-card {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-card.pending-card {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stat-card.invoice-card {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.9;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin: 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-detail {
  font-size: 13px;
  opacity: 0.85;
}

/* 详细统计 */
.detail-stats {
  padding: 10px 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 15px;
}

.stat-item.highlight {
  background: #f5f7fa;
  padding: 15px;
  margin: 5px 0;
  border-radius: 4px;
}

.stat-item .label {
  color: #606266;
  font-weight: 500;
}

.stat-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.stat-item .value.success {
  color: #67c23a;
  font-size: 20px;
}

.stat-item .value.danger {
  color: #f56c6c;
}

/* 状态统计 */
.status-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 10px 0;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 4px;
  flex: 1;
  min-width: 45%;
}

.status-count {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-left: auto;
}

.empty-hint {
  color: #909399;
  text-align: center;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.low-stock {
  color: #f56c6c;
  font-weight: 600;
}

/* 移动端优化 - iOS风格 */
@media (max-width: 768px) {
  .dashboard-page {
    padding: 0;
  }

  /* 筛选器移动端优化 */
  .filter-card {
    margin-bottom: 16px;
    border-radius: 16px;
  }

  .filter-card :deep(.el-card__body) {
    padding: 12px;
  }

  .filter-card :deep(.el-col) {
    margin-bottom: 12px;
  }

  .filter-card :deep(.el-button) {
    padding: 8px 12px;
    font-size: 13px;
  }

  .filter-card :deep(.el-date-editor) {
    font-size: 13px;
  }

  /* 财务卡片改为单列 */
  .stat-card {
    height: 140px;
    margin-bottom: 12px;
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .stat-card :deep(.el-card__body) {
    padding: 16px;
  }

  .stat-icon {
    font-size: 36px;
  }

  .stat-value {
    font-size: 26px;
  }

  .stat-label {
    font-size: 13px;
  }

  .stat-detail {
    font-size: 12px;
  }

  /* 详细统计卡片 */
  .detail-stats {
    padding: 8px 0;
  }

  .stat-item {
    padding: 10px 0;
    font-size: 14px;
  }

  .stat-item .value {
    font-size: 16px;
  }

  .stat-item.highlight {
    padding: 12px;
    margin: 8px 0;
    border-radius: 8px;
  }

  .stat-item .value.success {
    font-size: 18px;
  }

  /* 状态分布 */
  .status-stats {
    gap: 10px;
  }

  .status-item {
    min-width: 100%;
    padding: 12px;
    border-radius: 10px;
  }

  .status-count {
    font-size: 18px;
  }

  /* 表格优化 */
  :deep(.el-table) {
    font-size: 13px;
  }

  :deep(.el-table th) {
    padding: 8px 0;
    font-size: 13px;
  }

  :deep(.el-table td) {
    padding: 10px 0;
  }

  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  /* 卡片头部 */
  .card-header {
    font-size: 15px;
  }

  .card-header :deep(.el-button) {
    font-size: 13px;
  }

  /* 最近订单表格 - 隐藏部分列 */
  :deep(.el-table__header-wrapper),
  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  /* 卡片圆角 */
  :deep(.el-card) {
    border-radius: 16px;
    margin-bottom: 16px;
  }

  /* 标签大小 */
  :deep(.el-tag) {
    padding: 4px 8px;
    font-size: 12px;
  }
}

/* 小屏手机 (< 375px) */
@media (max-width: 375px) {
  .stat-value {
    font-size: 22px !important;
  }

  .stat-icon {
    font-size: 32px !important;
  }

  .stat-card {
    height: 120px;
  }

  .filter-card :deep(.el-button) {
    padding: 6px 10px;
    font-size: 12px;
  }
}
</style>
