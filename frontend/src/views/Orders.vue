<template>
  <div class="orders-page">
    <!-- Header Section -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">订单管理</h2>
        <p class="page-subtitle">管理和跟踪所有订单</p>
      </div>
      <el-button type="primary" size="large" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        创建订单
      </el-button>
    </div>

    <!-- Main Content -->
    <el-card shadow="hover" class="main-card">
      <el-table 
        :data="orders" 
        v-loading="loading"
        stripe
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
      >
        <!-- 订单号列 -->
        <el-table-column label="订单号" width="160" fixed="left">
          <template #default="{ row }">
            <el-link 
              type="primary" 
              @click="$router.push(`/orders/${row.id}`)"
              :underline="false"
              class="order-link"
            >
              <el-icon><DocumentCopy /></el-icon>
              {{ row.orderNumber || `#${row.id}` }}
            </el-link>
          </template>
        </el-table-column>

        <!-- 客户列 -->
        <el-table-column label="客户" width="150">
          <template #default="{ row }">
            <div class="customer-info">
              <el-icon class="info-icon"><User /></el-icon>
              <span>{{ row.customer?.name || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 订单明细列 -->
        <el-table-column label="订单明细" min-width="280">
          <template #default="{ row }">
            <div class="order-items">
              <div v-for="(item, idx) in row.items?.slice(0, 2)" :key="item.id" class="item-row">
                <span class="item-name">{{ item.product?.name }}</span>
                <span class="item-detail">
                  × {{ item.quantity }} | ¥{{ item.unitPrice?.toFixed(2) }}
                </span>
              </div>
              <div v-if="row.items?.length > 2" class="more-items">
                +{{ row.items.length - 2 }} 更多商品
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 订单金额列 -->
        <el-table-column label="订单金额" width="130" align="right">
          <template #default="{ row }">
            <div class="amount-cell">
              <span class="amount-value">¥{{ calculateTotal(row.items).toFixed(2) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 订单日期列 -->
        <el-table-column label="订单日期" width="160">
          <template #default="{ row }">
            <div class="date-info">
              <el-icon class="info-icon"><Calendar /></el-icon>
              <span>{{ formatDate(row.orderDate) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 状态列 -->
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.status)" 
              effect="dark"
              size="default"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <!-- 合同拟定状态：显示合同下载和确认按钮 -->
              <template v-if="row.status === 'CONTRACT_DRAFT'">
                <el-dropdown @command="(cmd) => handleContractAction(row, cmd)">
                  <el-button type="warning" size="small">
                    <el-icon><Document /></el-icon>
                    合同 <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="word">
                        <el-icon><Download /></el-icon> 下载Word
                      </el-dropdown-item>
                      <el-dropdown-item command="pdf">
                        <el-icon><Download /></el-icon> 下载PDF
                      </el-dropdown-item>
                      <el-dropdown-item command="confirm" divided>
                        <el-icon><Check /></el-icon> 确认合同
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              
              <!-- 已付款/已开票状态：显示出库按钮 -->
              <el-button 
                v-if="canShipOrder(row)"
                type="success" 
                size="small" 
                @click="handleShipOrder(row)"
                :loading="submitting"
              >
                <el-icon><Sell /></el-icon>
                出库
              </el-button>
              
              <!-- 其他状态可以查看合同 -->
              <el-dropdown v-if="canViewContract(row) && row.status !== 'CONTRACT_DRAFT'" @command="(cmd) => handleContractAction(row, cmd)">
                <el-button type="info" size="small" plain>
                  <el-icon><Document /></el-icon>
                  查看合同
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="word">
                      <el-icon><Download /></el-icon> 下载Word
                    </el-dropdown-item>
                    <el-dropdown-item command="pdf">
                      <el-icon><Download /></el-icon> 下载PDF
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              
              <el-button 
                type="danger" 
                size="small" 
                @click="handleDelete(row)"
                plain
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty State -->
      <el-empty 
        v-if="!loading && orders.length === 0" 
        description="暂无订单数据"
        :image-size="120"
      />
    </el-card>

    <!-- 创建订单对话框 -->
    <el-dialog 
      v-model="showAddDialog" 
      title="创建新订单" 
      width="750px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px" class="order-form">
        <el-form-item label="选择客户" required>
          <el-select 
            v-model="form.customerId" 
            placeholder="请选择客户" 
            size="large"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="customer in customers"
              :key="customer.id"
              :label="customer.name"
              :value="customer.id"
            >
              <span>{{ customer.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px;">
                {{ customer.phone || '-' }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-divider content-position="left">
          <el-icon><List /></el-icon>
          订单商品
        </el-divider>
        
        <div class="items-container">
          <div v-for="(item, index) in form.items" :key="index" class="item-form-card">
            <div class="item-form-header">
              <span class="item-number">商品 {{ index + 1 }}</span>
              <el-button 
                v-if="form.items.length > 1"
                type="danger" 
                size="small"
                text
                @click="removeItem(index)"
              >
                <el-icon><Delete /></el-icon>
                移除
              </el-button>
            </div>
            
            <el-form-item label="选择产品" required>
              <el-select 
                v-model="item.productId" 
                placeholder="搜索并选择产品" 
                filterable
                style="width: 100%"
                @change="onProductChange(item, index)"
              >
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="product.name"
                  :value="product.id"
                >
                  <div class="product-option">
                    <span class="product-name">{{ product.name }}</span>
                    <span class="product-info">
                      <el-tag size="small" type="info">库存: {{ product.stock || 0 }}</el-tag>
                      <span class="product-price">¥{{ product.price?.toFixed(2) }}</span>
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="数量" required>
                  <el-input-number 
                    v-model="item.quantity" 
                    :min="0.01" 
                    :precision="2" 
                    :step="1"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单价" required>
                  <el-input-number 
                    v-model="item.unitPrice" 
                    :precision="2" 
                    :min="0"
                    :step="0.01"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <div class="item-subtotal">
              <span class="subtotal-label">小计:</span>
              <span class="subtotal-value">¥{{ ((item.quantity || 0) * (item.unitPrice || 0)).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        
        <el-button 
          type="primary" 
          plain 
          @click="addItem" 
          class="add-item-btn"
          size="large"
        >
          <el-icon><Plus /></el-icon>
          添加更多商品
        </el-button>

        <!-- 订单总计 -->
        <div class="order-total">
          <span class="total-label">订单总额:</span>
          <span class="total-value">¥{{ calculateFormTotal().toFixed(2) }}</span>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleAdd" :loading="submitting" size="large">
            <el-icon v-if="!submitting"><Check /></el-icon>
            确认创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 更新状态对话框 -->
    <el-dialog 
      v-model="showStatusUpdateDialog" 
      title="更新订单状态" 
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(currentOrder?.status)" size="large" effect="dark">
            {{ getStatusText(currentOrder?.status) }}
          </el-tag>
        </el-form-item>
        
        <el-form-item label="新状态" required>
          <el-select v-model="newStatus" placeholder="请选择新状态" size="large" style="width: 100%">
            <el-option label="已创建" value="CREATED">
              <el-tag type="info" size="small">已创建</el-tag>
            </el-option>
            <el-option label="合同拟定" value="CONTRACT_DRAFT">
              <el-tag type="warning" size="small">合同拟定</el-tag>
            </el-option>
            <el-option label="未付款" value="UNPAID">
              <el-tag type="warning" size="small">未付款</el-tag>
            </el-option>
            <el-option label="付款完成" value="PAID">
              <el-tag type="success" size="small">付款完成</el-tag>
            </el-option>
            <el-option label="已开票" value="INVOICED">
              <el-tag type="primary" size="small">已开票</el-tag>
            </el-option>
            <el-option label="已发货" value="SHIPPED">
              <el-tag size="small">已发货</el-tag>
            </el-option>
            <el-option label="已完成" value="COMPLETED">
              <el-tag type="success" size="small">已完成</el-tag>
            </el-option>
            <el-option label="已取消" value="CANCELLED">
              <el-tag type="danger" size="small">已取消</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showStatusUpdateDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleStatusUpdate" :loading="submitting" size="large">
            确认更新
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, productApi, customerApi } from '../api'

const orders = ref([])
const products = ref([])
const customers = ref([])
const loading = ref(false)
const showAddDialog = ref(false)
const showStatusUpdateDialog = ref(false)
const submitting = ref(false)
const currentOrder = ref(null)
const newStatus = ref('')

const form = ref({
  customerId: null,
  items: [
    { productId: null, quantity: 1, unitPrice: 0, costUnitPrice: 0 }
  ]
})

const loadOrders = async () => {
  loading.value = true
  try {
    orders.value = await orderApi.getAll()
  } catch (error) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const loadProducts = async () => {
  try {
    products.value = await productApi.getAll()
  } catch (error) {
    ElMessage.error('加载产品列表失败')
  }
}

const loadCustomers = async () => {
  try {
    customers.value = await customerApi.getAll()
  } catch (error) {
    ElMessage.error('加载客户列表失败')
  }
}

const calculateTotal = (items) => {
  if (!items || items.length === 0) return 0
  return items.reduce((sum, item) => sum + (item.subtotal || 0), 0)
}

const calculateFormTotal = () => {
  return form.value.items.reduce((sum, item) => {
    return sum + ((item.quantity || 0) * (item.unitPrice || 0))
  }, 0)
}

const addItem = () => {
  form.value.items.push({ productId: null, quantity: 1, unitPrice: 0, costUnitPrice: 0 })
}

const removeItem = (index) => {
  form.value.items.splice(index, 1)
}

const onProductChange = (item, index) => {
  const product = products.value.find(p => p.id === item.productId)
  if (product) {
    item.unitPrice = product.price || 0
    item.costUnitPrice = product.price || 0
  }
}

const handleAdd = async () => {
  if (!form.value.customerId) {
    ElMessage.warning('请选择客户')
    return
  }

  const hasInvalidItem = form.value.items.some(item => !item.productId || item.quantity <= 0)
  if (hasInvalidItem) {
    ElMessage.warning('请完整填写所有订单项')
    return
  }

  submitting.value = true
  try {
    const orderData = {
      customer: { id: form.value.customerId },
      orderDate: new Date().toISOString(),
      items: form.value.items.map(item => {
        const product = products.value.find(p => p.id === item.productId)
        return {
          product: { id: item.productId },
          quantity: item.quantity,
          unitPrice: item.unitPrice || product?.price || 0,
          costUnitPrice: item.costUnitPrice || product?.price || 0,
          subtotal: item.quantity * (item.unitPrice || product?.price || 0),
          costSubtotal: item.quantity * (item.costUnitPrice || product?.price || 0)
        }
      })
    }
    
    await orderApi.create(orderData)
    ElMessage.success('订单创建成功')
    showAddDialog.value = false
    form.value = {
      customerId: null,
      items: [{ productId: null, quantity: 1, unitPrice: 0, costUnitPrice: 0 }]
    }
    loadOrders()
  } catch (error) {
    ElMessage.error('创建失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

const showStatusDialog = (row) => {
  currentOrder.value = row
  newStatus.value = row.status
  showStatusUpdateDialog.value = true
}

const handleStatusUpdate = async () => {
  if (!newStatus.value) {
    ElMessage.warning('请选择新状态')
    return
  }

  submitting.value = true
  try {
    await orderApi.updateStatus(currentOrder.value.id, newStatus.value)
    ElMessage.success('状态更新成功')
    showStatusUpdateDialog.value = false
    loadOrders()
  } catch (error) {
    ElMessage.error('状态更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除订单 ${row.orderNumber || '#' + row.id} 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await orderApi.delete(row.id)
    ElMessage.success('订单删除成功')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

const getStatusType = (status) => {
  const types = {
    'CREATED': 'info',
    'CONTRACT_DRAFT': 'warning',
    'UNPAID': 'warning',
    'PAID': 'success',
    'PENDING_STOCK': 'warning',
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
    'CONTRACT_DRAFT': '合同拟定',
    'UNPAID': '未付款',
    'PAID': '已付款',
    'PENDING_STOCK': '待备货',
    'INVOICED': '已开票',
    'SHIPPED': '已发货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

// 检查是否可以查看合同
const canViewContract = (order) => {
  return ['CONTRACT_DRAFT', 'UNPAID', 'PAID', 'INVOICED', 'SHIPPED', 'COMPLETED'].includes(order.status)
}

// 检查订单是否可以出库
const canShipOrder = (order) => {
  return ['PAID', 'INVOICED', 'PENDING_STOCK'].includes(order.status)
}

// 出库操作
const handleShipOrder = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要将订单 ${row.orderNumber || '#' + row.id} 标记为已发货吗？\n系统将验证库存并扣减相应数量。`,
      '出库确认',
      {
        confirmButtonText: '确认出库',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    submitting.value = true
    await orderApi.shipOrder(row.id)
    ElMessage.success('订单已成功出库！库存已扣减。')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      const errorMsg = error.response?.data?.message || error.message || '出库失败'
      ElMessage.error({
        message: errorMsg,
        duration: 5000,
        showClose: true
      })
    }
  } finally {
    submitting.value = false
  }
}

// 统一处理合同操作
const handleContractAction = async (row, command) => {
  if (command === 'word') {
    await downloadContract(row.id, 'word')
  } else if (command === 'pdf') {
    await downloadContract(row.id, 'pdf')
  } else if (command === 'confirm') {
    await confirmContract(row.id)
  }
}

// 下载合同
const downloadContract = async (orderId, format) => {
  try {
    const { contractApi } = await import('../api')
    let response
    
    if (format === 'word') {
      response = await contractApi.downloadWord(orderId)
    } else {
      response = await contractApi.downloadPdf(orderId)
    }
    
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `contract_${orderId}.${format === 'word' ? 'docx' : 'pdf'}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success(`合同${format === 'word' ? 'Word' : 'PDF'}下载成功`)
  } catch (error) {
    ElMessage.error('下载失败: ' + (error.message || '未知错误'))
  }
}

// 确认合同
const confirmContract = async (orderId) => {
  try {
    const { contractApi } = await import('../api')
    await contractApi.confirm(orderId)
    ElMessage.success('合同已确认')
    loadOrders()
  } catch (error) {
    ElMessage.error('确认失败: ' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  loadOrders()
  loadProducts()
  loadCustomers()
})
</script>

<style scoped>
.orders-page {
  padding: 0;
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 4px;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* Main Card */
.main-card {
  border-radius: 8px;
}

.main-card :deep(.el-card__body) {
  padding: 0;
}

/* Table Customization */
.order-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}

.customer-info,
.date-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.info-icon {
  color: #909399;
  font-size: 16px;
}

/* Order Items Display */
.order-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
}

.item-name {
  font-weight: 500;
  color: #303133;
  flex: 1;
}

.item-detail {
  color: #909399;
  font-size: 12px;
  margin-left: 12px;
  white-space: nowrap;
}

.more-items {
  color: #409eff;
  font-size: 12px;
  padding: 4px 0;
}

/* Amount Display */
.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.amount-value {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

/* Order Form */
.order-form {
  padding: 0 8px;
}

.items-container {
  max-height: 450px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.item-form-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.item-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.item-number {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.item-subtotal {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #dcdfe6;
}

.subtotal-label {
  color: #606266;
  font-size: 14px;
}

.subtotal-value {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.add-item-btn {
  width: 100%;
  margin-bottom: 20px;
}

/* Product Option */
.product-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.product-name {
  flex: 1;
  font-size: 14px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-price {
  font-weight: 600;
  color: #f56c6c;
  font-size: 14px;
}

/* Order Total */
.order-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(to right, #f5f7fa, #fff);
  border-radius: 8px;
  border: 2px dashed #409eff;
  margin-top: 8px;
}

.total-label {
  font-size: 16px;
  font-weight: 600;
  color: #606266;
}

.total-value {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

/* Dialog Footer */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .action-buttons {
    flex-direction: column;
    width: 100%;
  }
}
</style>
