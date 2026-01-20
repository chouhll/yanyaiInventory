<template>
  <div class="inventory-alerts">
    <div class="page-header">
      <h2>📊 库存预警</h2>
      <el-button type="primary" @click="refreshAlerts" :icon="Refresh">刷新</el-button>
    </div>

    <!-- 预警统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="4">
        <el-card class="stat-card high-severity">
          <div class="stat-content">
            <div class="stat-icon">⚠️</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.outOfStock + statistics.expired }}</div>
              <div class="stat-label">高危预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card medium-severity">
          <div class="stat-content">
            <div class="stat-icon">⚡</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.lowStock + statistics.expiringSoon }}</div>
              <div class="stat-label">中等预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card low-severity">
          <div class="stat-content">
            <div class="stat-icon">ℹ️</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.overStock + statistics.slowMoving }}</div>
              <div class="stat-label">低级预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">📦</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.lowStock }}</div>
              <div class="stat-label">低库存</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">🚫</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.outOfStock }}</div>
              <div class="stat-label">缺货</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">⏰</div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.expiringSoon }}</div>
              <div class="stat-label">即将过期</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filterType" placeholder="预警类型" clearable @change="filterAlerts">
            <el-option label="全部" value=""></el-option>
            <el-option label="低库存" value="LOW_STOCK"></el-option>
            <el-option label="缺货" value="OUT_OF_STOCK"></el-option>
            <el-option label="超库存" value="OVER_STOCK"></el-option>
            <el-option label="即将过期" value="EXPIRING_SOON"></el-option>
            <el-option label="已过期" value="EXPIRED"></el-option>
            <el-option label="滞销品" value="SLOW_MOVING"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterSeverity" placeholder="严重程度" clearable @change="filterAlerts">
            <el-option label="全部" value=""></el-option>
            <el-option label="高危" value="HIGH"></el-option>
            <el-option label="中等" value="MEDIUM"></el-option>
            <el-option label="低级" value="LOW"></el-option>
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- 预警列表 -->
    <el-card class="alerts-table">
      <el-table :data="filteredAlerts" style="width: 100%" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="alert-details">
              <p><strong>产品编码：</strong>{{ row.product.code }}</p>
              <p><strong>当前库存：</strong>{{ row.currentStock }}</p>
              <p v-if="row.safetyStock"><strong>安全库存：</strong>{{ row.safetyStock }}</p>
              <p v-if="row.maxStock"><strong>最大库存：</strong>{{ row.maxStock }}</p>
              <p v-if="row.batch"><strong>批次号：</strong>{{ row.batch.batchNumber }}</p>
              <p v-if="row.expirationDate"><strong>到期日期：</strong>{{ row.expirationDate }}</p>
              <p v-if="row.daysUntilExpiration"><strong>剩余天数：</strong>{{ row.daysUntilExpiration }}天</p>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityType(row.severity)" size="small">
              {{ getSeverityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="预警类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getAlertType(row.alertType)" size="small">
              {{ getAlertTypeLabel(row.alertType) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="product.name" label="产品名称" width="200"></el-table-column>
        
        <el-table-column label="预警信息" min-width="300">
          <template #default="{ row }">
            <div class="alert-message">{{ row.message }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button 
              v-if="row.alertType === 'LOW_STOCK' || row.alertType === 'OUT_OF_STOCK'" 
              type="primary" 
              size="small"
              @click="handleReplenish(row)">
              补货
            </el-button>
            <el-button 
              v-if="row.alertType === 'EXPIRED'" 
              type="danger" 
              size="small"
              @click="handleExpired(row)">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { alertApi } from '../api'

const alerts = ref([])
const statistics = ref({
  lowStock: 0,
  outOfStock: 0,
  overStock: 0,
  expiringSoon: 0,
  expired: 0,
  slowMoving: 0,
  total: 0
})

const filterType = ref('')
const filterSeverity = ref('')

// 过滤后的预警列表
const filteredAlerts = computed(() => {
  let result = alerts.value
  
  if (filterType.value) {
    result = result.filter(alert => alert.alertType === filterType.value)
  }
  
  if (filterSeverity.value) {
    result = result.filter(alert => alert.severity === filterSeverity.value)
  }
  
  return result
})

// 加载预警数据
const loadAlerts = async () => {
  try {
    const response = await alertApi.getAll()
    if (response.success) {
      alerts.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载预警数据失败')
    console.error(error)
  }
}

// 加载预警统计
const loadStatistics = async () => {
  try {
    const response = await alertApi.getStatistics()
    if (response.success) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 刷新预警
const refreshAlerts = async () => {
  await Promise.all([loadAlerts(), loadStatistics()])
  ElMessage.success('预警数据已刷新')
}

// 筛选预警
const filterAlerts = () => {
  // 筛选由computed自动处理
}

// 获取严重程度类型
const getSeverityType = (severity) => {
  const types = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'info'
  }
  return types[severity] || 'info'
}

// 获取严重程度标签
const getSeverityLabel = (severity) => {
  const labels = {
    'HIGH': '高危',
    'MEDIUM': '中等',
    'LOW': '低级'
  }
  return labels[severity] || severity
}

// 获取预警类型标签
const getAlertTypeLabel = (alertType) => {
  const labels = {
    'LOW_STOCK': '低库存',
    'OUT_OF_STOCK': '缺货',
    'OVER_STOCK': '超库存',
    'EXPIRING_SOON': '即将过期',
    'EXPIRED': '已过期',
    'SLOW_MOVING': '滞销品'
  }
  return labels[alertType] || alertType
}

// 获取预警类型样式
const getAlertType = (alertType) => {
  const types = {
    'OUT_OF_STOCK': 'danger',
    'LOW_STOCK': 'warning',
    'EXPIRED': 'danger',
    'EXPIRING_SOON': 'warning',
    'OVER_STOCK': 'info',
    'SLOW_MOVING': 'info'
  }
  return types[alertType] || 'info'
}

// 处理补货
const handleReplenish = (alert) => {
  ElMessage.info('跳转到采购页面进行补货')
  // 可以跳转到采购页面，并预填产品信息
}

// 处理过期
const handleExpired = (alert) => {
  ElMessage.info('处理过期批次')
  // 可以跳转到批次管理页面处理过期批次
}

onMounted(() => {
  loadAlerts()
  loadStatistics()
})
</script>

<style scoped>
.inventory-alerts {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card.high-severity {
  border-left: 4px solid #F56C6C;
}

.stat-card.medium-severity {
  border-left: 4px solid #E6A23C;
}

.stat-card.low-severity {
  border-left: 4px solid #409EFF;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-icon {
  font-size: 32px;
}

.stat-info {
  text-align: right;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.filter-card {
  margin-bottom: 20px;
}

.alerts-table {
  margin-bottom: 20px;
}

.alert-details {
  padding: 10px 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.alert-details p {
  margin: 8px 0;
  color: #606266;
}

.alert-message {
  color: #606266;
  line-height: 1.5;
}
</style>