<template>
  <div class="inventory-report-page">
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-row :gutter="20" align="middle">
        <el-col :span="8">
          <el-select v-model="selectedPeriod" placeholder="选择期间" style="width: 100%">
            <el-option
              v-for="period in periods"
              :key="period"
              :label="period"
              :value="period"
            />
          </el-select>
        </el-col>
        <el-col :span="16">
          <el-button type="primary" @click="generateReport" :loading="generating">
            <el-icon><DocumentAdd /></el-icon>
            生成报表
          </el-button>
          <el-button @click="loadReport" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>库存月报表 - {{ selectedPeriod }}</span>
          <el-button type="success" @click="exportReport">
            <el-icon><Download /></el-icon>
            导出报表
          </el-button>
        </div>
      </template>

      <el-table 
        :data="reportData" 
        style="width: 100%" 
        v-loading="loading"
        border
        :header-cell-style="{ background: '#f5f7fa', color: '#333' }"
      >
        <el-table-column prop="product.inventoryCode" label="库存编码" width="120" fixed />
        <el-table-column prop="product.name" label="存货名称" width="150" fixed />
        <el-table-column prop="product.specification" label="规格型号" width="120" />
        <el-table-column prop="product.category" label="存货分类" width="100" />
        <el-table-column prop="product.unit" label="计量单位" width="90" />
        
        <el-table-column label="期初" align="center">
          <el-table-column prop="beginningQuantity" label="数量" width="100" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.beginningQuantity) }}
            </template>
          </el-table-column>
          <el-table-column prop="beginningUnitPrice" label="单价" width="100" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.beginningUnitPrice) }}
            </template>
          </el-table-column>
          <el-table-column prop="beginningAmount" label="金额" width="120" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.beginningAmount) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="入库" align="center">
          <el-table-column prop="inboundQuantity" label="数量" width="100" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.inboundQuantity) }}
            </template>
          </el-table-column>
          <el-table-column prop="inboundUnitPrice" label="单价" width="100" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.inboundUnitPrice) }}
            </template>
          </el-table-column>
          <el-table-column prop="inboundAmount" label="金额" width="120" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.inboundAmount) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="出库" align="center">
          <el-table-column prop="outboundQuantity" label="数量" width="100" align="right">
            <template #default="{ row }">
              {{ formatNumber(row.outboundQuantity) }}
            </template>
          </el-table-column>
          <el-table-column prop="outboundCostUnitPrice" label="成本单价" width="110" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.outboundCostUnitPrice) }}
            </template>
          </el-table-column>
          <el-table-column prop="outboundCostAmount" label="成本金额" width="120" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.outboundCostAmount) }}
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="结存" align="center">
          <el-table-column prop="endingQuantity" label="数量" width="100" align="right">
            <template #default="{ row }">
              <el-tag :type="row.endingQuantity > 0 ? 'success' : 'info'">
                {{ formatNumber(row.endingQuantity) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="endingUnitPrice" label="单价" width="100" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.endingUnitPrice) }}
            </template>
          </el-table-column>
          <el-table-column prop="endingAmount" label="金额" width="120" align="right">
            <template #default="{ row }">
              {{ formatCurrency(row.endingAmount) }}
            </template>
          </el-table-column>
        </el-table-column>
      </el-table>

      <div v-if="reportData.length > 0" class="summary-section">
        <el-divider content-position="left">汇总统计</el-divider>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="产品种类" :value="reportData.length" suffix="种" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="期初总额" :value="totalBeginning" :precision="2" prefix="¥" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="入库总额" :value="totalInbound" :precision="2" prefix="¥" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="出库总额" :value="totalOutbound" :precision="2" prefix="¥" />
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { inventoryApi } from '../api'

const selectedPeriod = ref('')
const periods = ref([])
const reportData = ref([])
const loading = ref(false)
const generating = ref(false)

const totalBeginning = computed(() => {
  return reportData.value.reduce((sum, item) => sum + (item.beginningAmount || 0), 0)
})

const totalInbound = computed(() => {
  return reportData.value.reduce((sum, item) => sum + (item.inboundAmount || 0), 0)
})

const totalOutbound = computed(() => {
  return reportData.value.reduce((sum, item) => sum + (item.outboundCostAmount || 0), 0)
})

const loadPeriods = async () => {
  try {
    periods.value = await inventoryApi.getPeriods()
    if (periods.value.length > 0) {
      selectedPeriod.value = periods.value[0]
      await loadReport()
    }
  } catch (error) {
    ElMessage.error('加载期间列表失败')
  }
}

const loadReport = async () => {
  if (!selectedPeriod.value) {
    ElMessage.warning('请选择期间')
    return
  }

  loading.value = true
  try {
    reportData.value = await inventoryApi.getReport(selectedPeriod.value)
    if (reportData.value.length === 0) {
      ElMessage.info('该期间暂无报表数据，请先生成报表')
    }
  } catch (error) {
    ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
  }
}

const generateReport = async () => {
  if (!selectedPeriod.value) {
    ElMessage.warning('请选择期间')
    return
  }

  generating.value = true
  try {
    reportData.value = await inventoryApi.generateReport(selectedPeriod.value)
    ElMessage.success('报表生成成功')
  } catch (error) {
    ElMessage.error('生成报表失败')
  } finally {
    generating.value = false
  }
}

const exportReport = () => {
  // 导出功能可以后续实现
  ElMessage.info('导出功能开发中...')
}

const formatNumber = (value) => {
  if (value == null) return '0.00'
  return Number(value).toFixed(2)
}

const formatCurrency = (value) => {
  if (value == null) return '0.00'
  return Number(value).toFixed(2)
}

onMounted(() => {
  loadPeriods()
})
</script>

<style scoped>
.inventory-report-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.summary-section {
  margin-top: 30px;
  padding: 20px;
  background: #fafafa;
  border-radius: 4px;
}

:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-table th) {
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 8px 0;
}
</style>