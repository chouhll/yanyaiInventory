<template>
  <div class="order-detail-page">
    <!-- Header Section -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="$router.back()" size="large" plain>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="header-info">
          <h2 class="page-title">订单详情</h2>
          <el-tag :type="getStatusType(order.status)" size="large" effect="dark">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="prepareEditForm" size="large" v-if="!isEditing">
          <el-icon><Edit /></el-icon>
          编辑订单
        </el-button>
        <el-button 
          type="warning" 
          @click="handleShowInvoiceDialog"
          size="large"
          v-if="order.status === 'PAID' && !invoice"
        >
          <el-icon><Document /></el-icon>
          开具发票
        </el-button>
      </div>
    </div>

    <!-- Main Content -->
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="24">
        <!-- Status Progress -->
        <el-card shadow="hover" class="status-card">
          <template #header>
            <div class="card-title">
              <el-icon><Histogram /></el-icon>
              <span>订单状态流程</span>
            </div>
          </template>
          <el-steps :active="currentStepIndex" finish-status="success" align-center>
            <el-step
              v-for="step in statusSteps"
              :key="step.value"
              :title="step.label"
              :status="getStepStatus(step.value)"
            />
          </el-steps>
          <div class="status-actions">
            <el-button type="primary" @click="showStatusDialog = true" size="default">
              <el-icon><Refresh /></el-icon>
              更新状态
            </el-button>
          </div>
        </el-card>

        <!-- Basic Information -->
        <el-card shadow="hover" class="info-card">
          <template #header>
            <div class="card-title">
              <el-icon><InfoFilled /></el-icon>
              <span>基本信息</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">订单号:</span>
                <span class="info-value">{{ order.orderNumber || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">订单ID:</span>
                <span class="info-value">#{{ order.id }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">客户:</span>
                <span class="info-value">{{ order.customer?.name || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">订单日期:</span>
                <span class="info-value">{{ formatDate(order.orderDate) }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- Order Items -->
        <el-card shadow="hover" class="items-card">
          <template #header>
            <div class="card-title">
              <el-icon><ShoppingCart /></el-icon>
              <span>订单商品</span>
            </div>
          </template>
          <el-table 
            :data="order.items" 
            border 
            :header-cell-style="{ background: '#f5f7fa', fontWeight: '600' }"
          >
            <el-table-column label="产品名称" min-width="180">
              <template #default="{ row }">
                <div class="product-cell">
                  <span class="product-name">{{ row.product?.name || '-' }}</span>
                  <span class="product-spec" v-if="row.product?.specification">
                    {{ row.product.specification }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="large">{{ row.quantity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120" align="right">
              <template #default="{ row }">
                <span class="price-text">¥{{ row.unitPrice?.toFixed(2) || '0.00' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="小计" width="140" align="right">
              <template #default="{ row }">
                <span class="subtotal-text">¥{{ row.subtotal?.toFixed(2) || '0.00' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- Order Total -->
          <div class="order-summary">
            <div class="summary-row">
              <span class="summary-label">商品总额:</span>
              <span class="summary-value">¥{{ orderTotal.toFixed(2) }}</span>
            </div>
            <el-divider style="margin: 12px 0;" />
            <div class="summary-row total">
              <span class="summary-label">订单总计:</span>
              <span class="summary-value total-amount">¥{{ orderTotal.toFixed(2) }}</span>
            </div>
          </div>
        </el-card>

        <!-- Invoice Information -->
        <el-card v-if="invoice" shadow="hover" class="invoice-card">
          <template #header>
            <div class="card-title">
              <el-icon><Document /></el-icon>
              <span>发票信息</span>
              <el-tag 
                v-if="invoice.status === 'ISSUED'" 
                type="success" 
                effect="dark"
                size="default"
                style="margin-left: auto;"
              >
                已开具
              </el-tag>
              <el-tag 
                v-else-if="invoice.status === 'VOIDED'" 
                type="info" 
                size="default"
                style="margin-left: auto;"
              >
                已作废
              </el-tag>
            </div>
          </template>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">发票号码:</span>
                <span class="info-value">{{ invoice.invoiceNumber }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">开票日期:</span>
                <span class="info-value">{{ formatDate(invoice.invoiceDate) }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">发票类型:</span>
                <span class="info-value">
                  {{ invoice.invoiceType === 'NORMAL' ? '普通发票' : '专用发票' }}
                </span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">总金额:</span>
                <span class="info-value price-text">¥{{ invoice.totalAmount?.toFixed(2) || '0.00' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">税额:</span>
                <span class="info-value">¥{{ invoice.taxAmount?.toFixed(2) || '0.00' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">价税合计:</span>
                <span class="info-value total-amount">¥{{ invoice.amountWithTax?.toFixed(2) || '0.00' }}</span>
              </div>
            </el-col>
          </el-row>
          
          <div v-if="invoice.status === 'ISSUED'" style="margin-top: 20px;">
            <el-button type="danger" size="default" @click="handleVoidInvoice">
              <el-icon><Close /></el-icon>
              作废发票
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑订单对话框 -->
    <el-dialog 
      v-model="showEditDialog" 
      title="编辑订单" 
      width="750px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="90px" class="order-form">
        <el-form-item label="选择客户" required>
          <el-select 
            v-model="editForm.customerId" 
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
            />
          </el-select>
        </el-form-item>
        
        <el-divider content-position="left">订单商品</el-divider>
        
        <div class="items-container">
          <div v-for="(item, index) in editForm.items" :key="index" class="item-form-card">
            <div class="item-form-header">
              <span class="item-number">商品 {{ index + 1 }}</span>
              <el-button 
                v-if="editForm.items.length > 1"
                type="danger" 
                size="small"
                text
                @click="removeEditItem(index)"
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
                @change="onProductChange(item)"
              >
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="`${product.name} (库存: ${product.stock || 0})`"
                  :value="product.id"
                />
              </el-select>
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="数量" required>
                  <el-input-number 
                    v-model="item.quantity" 
                    :min="0.01" 
                    :precision="2" 
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
          @click="addEditItem" 
          class="add-item-btn"
          size="large"
        >
          <el-icon><Plus /></el-icon>
          添加更多商品
        </el-button>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleUpdate" :loading="submitting" size="large">
            保存更改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 开具发票对话框 -->
    <el-dialog 
      v-model="showInvoiceDialog" 
      title="开具发票" 
      width="650px"
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="!order.customer?.taxNumber"
        title="客户开票信息不完整"
        type="warning"
        description="请先到客户管理页面完善该客户的税号等开票信息"
        :closable="false"
        style="margin-bottom: 20px;"
      />
      
      <div v-else>
        <div class="invoice-section">
          <h4 class="section-title">客户开票信息</h4>
          <el-row :gutter="16" class="invoice-info">
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">公司抬头:</span>
                <span class="info-value">{{ order.customer.companyName || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">税号:</span>
                <span class="info-value">{{ order.customer.taxNumber || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="24">
              <div class="info-item">
                <span class="info-label">公司地址:</span>
                <span class="info-value">{{ order.customer.companyAddress || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">开户银行:</span>
                <span class="info-value">{{ order.customer.bankName || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="info-label">银行账号:</span>
                <span class="info-value">{{ order.customer.bankAccount || '-' }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <el-divider />
        
        <el-form :model="invoiceForm" label-width="90px">
          <el-form-item label="发票类型" required>
            <el-radio-group v-model="invoiceForm.invoiceType" size="large">
              <el-radio label="NORMAL">普通发票</el-radio>
              <el-radio label="SPECIAL">专用发票</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="税率" required>
            <el-select v-model="invoiceForm.taxRate" size="large" style="width: 200px;">
              <el-option label="13%" :value="0.13" />
              <el-option label="9%" :value="0.09" />
              <el-option label="6%" :value="0.06" />
              <el-option label="3%" :value="0.03" />
            </el-select>
          </el-form-item>
        </el-form>
        
        <el-divider />
        
        <div class="invoice-calculation">
          <div class="calc-row">
            <span class="calc-label">订单金额:</span>
            <span class="calc-value">¥{{ orderTotal.toFixed(2) }}</span>
          </div>
          <div class="calc-row">
            <span class="calc-label">税额 ({{ (invoiceForm.taxRate * 100).toFixed(0) }}%):</span>
            <span class="calc-value">¥{{ taxAmount }}</span>
          </div>
          <el-divider style="margin: 12px 0;" />
          <div class="calc-row total">
            <span class="calc-label">价税合计:</span>
            <span class="calc-value total-amount">¥{{ amountWithTax }}</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showInvoiceDialog = false" size="large">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleIssueInvoice" 
            :loading="submitting"
            :disabled="!order.customer?.taxNumber"
            size="large"
          >
            <el-icon v-if="!submitting"><Check /></el-icon>
            确认开票
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 发票PDF预览对话框 -->
    <el-dialog 
      v-model="showInvoicePdfDialog" 
      title="发票预览" 
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="invoice-pdf-preview">
        <iframe 
          :src="invoicePdfUrl" 
          style="width: 100%; height: 700px; border: 1px solid #ddd; border-radius: 4px;"
          v-if="invoicePdfUrl"
        ></iframe>
        <div v-else class="loading-preview">
          <el-icon style="font-size: 48px; color: #909399;"><Document /></el-icon>
          <p style="margin-top: 20px; color: #909399;">生成发票中...</p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showInvoicePdfDialog = false" size="large">关闭</el-button>
          <el-button type="primary" @click="handleConfirmInvoice" :loading="submitting" size="large">
            <el-icon v-if="!submitting"><Check /></el-icon>
            确认开具
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 更新状态对话框 -->
    <el-dialog 
      v-model="showStatusDialog" 
      title="更新订单状态" 
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(order.status)" size="large" effect="dark">
            {{ getStatusText(order.status) }}
          </el-tag>
        </el-form-item>
        
        <el-form-item label="新状态" required>
          <el-select v-model="newStatus" placeholder="请选择新状态" size="large" style="width: 100%">
            <el-option label="已创建" value="CREATED" />
            <el-option label="未付款" value="UNPAID" />
            <el-option label="付款完成" value="PAID" />
            <el-option label="已开票" value="INVOICED" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showStatusDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleStatusUpdate" :loading="submitting" size="large">
            确认更新
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, productApi, customerApi, invoiceApi } from '../api'

const route = useRoute()
const router = useRouter()

const order = ref({
  items: [],
  customer: {}
})
const products = ref([])
const customers = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const showStatusDialog = ref(false)
const submitting = ref(false)
const isEditing = ref(false)
const newStatus = ref('')
const invoice = ref(null)
const showInvoiceDialog = ref(false)
const showInvoicePdfDialog = ref(false)
const invoicePdfUrl = ref('')

const editForm = ref({
  customerId: null,
  items: []
})

const invoiceForm = ref({
  invoiceType: 'NORMAL',
  taxRate: 0.13
})

const statusSteps = [
  { value: 'CREATED', label: '已创建' },
  { value: 'UNPAID', label: '未付款' },
  { value: 'PAID', label: '已付款' },
  { value: 'INVOICED', label: '已开票' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' }
]

const currentStepIndex = computed(() => {
  if (order.value.status === 'CANCELLED') return -1
  const index = statusSteps.findIndex(step => step.value === order.value.status)
  return index >= 0 ? index : 0
})

const orderTotal = computed(() => {
  if (!order.value.items) return 0
  return order.value.items.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

const taxAmount = computed(() => {
  return (orderTotal.value * invoiceForm.value.taxRate).toFixed(2)
})

const amountWithTax = computed(() => {
  return (orderTotal.value + parseFloat(taxAmount.value)).toFixed(2)
})

const loadOrder = async () => {
  loading.value = true
  try {
    order.value = await orderApi.getById(route.params.id)
    newStatus.value = order.value.status
  } catch (error) {
    ElMessage.error('加载订单详情失败')
    router.back()
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

const loadInvoice = async () => {
  if (order.value && order.value.id) {
    try {
      invoice.value = await invoiceApi.getByOrder(order.value.id)
    } catch (error) {
      invoice.value = null
    }
  }
}

const handleShowInvoiceDialog = () => {
  if (!order.value.customer?.taxNumber) {
    ElMessage.warning('客户开票信息不完整，请先完善客户的税号等信息')
    return
  }
  showInvoiceDialog.value = true
}

const getStepStatus = (stepValue) => {
  const stepIndex = statusSteps.findIndex(s => s.value === stepValue)
  const currentIndex = currentStepIndex.value
  
  if (order.value.status === 'CANCELLED') {
    return 'error'
  }
  
  if (stepIndex < currentIndex) return 'finish'
  if (stepIndex === currentIndex) return 'process'
  return 'wait'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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

const prepareEditForm = () => {
  editForm.value = {
    customerId: order.value.customer?.id,
    items: order.value.items.map(item => ({
      productId: item.product?.id,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      costUnitPrice: item.costUnitPrice || item.unitPrice
    }))
  }
  showEditDialog.value = true
}

const onProductChange = (item) => {
  const product = products.value.find(p => p.id === item.productId)
  if (product) {
    item.unitPrice = product.price || 0
    item.costUnitPrice = product.price || 0
  }
}

const addEditItem = () => {
  editForm.value.items.push({
    productId: null,
    quantity: 1,
    unitPrice: 0,
    costUnitPrice: 0
  })
}

const removeEditItem = (index) => {
  editForm.value.items.splice(index, 1)
}

const handleUpdate = async () => {
  if (!editForm.value.customerId) {
    ElMessage.warning('请选择客户')
    return
  }

  const hasInvalidItem = editForm.value.items.some(item => !item.productId || item.quantity <= 0)
  if (hasInvalidItem) {
    ElMessage.warning('请完整填写所有订单项')
    return
  }

  submitting.value = true
  try {
    const orderData = {
      id: order.value.id,
      customer: { id: editForm.value.customerId },
      status: order.value.status,
      items: editForm.value.items.map(item => {
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
    
    await orderApi.update(order.value.id, orderData)
    ElMessage.success('订单更新成功')
    showEditDialog.value = false
    loadOrder()
  } catch (error) {
    ElMessage.error('更新失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

const handleStatusUpdate = async () => {
  if (!newStatus.value) {
    ElMessage.warning('请选择新状态')
    return
  }

  submitting.value = true
  try {
    await orderApi.updateStatus(order.value.id, newStatus.value)
    ElMessage.success('状态更新成功')
    showStatusDialog.value = false
    loadOrder()
  } catch (error) {
    ElMessage.error('状态更新失败')
  } finally {
    submitting.value = false
  }
}

const generateMockInvoicePdf = () => {
  const invoiceNumber = 'INV-' + new Date().getFullYear() + String(new Date().getMonth() + 1).padStart(2, '0') + String(new Date().getDate()).padStart(2, '0') + '-' + Math.random().toString(36).substr(2, 4).toUpperCase()
  const invoiceDate = new Date().toLocaleDateString('zh-CN')
  const invoiceType = invoiceForm.value.invoiceType === 'SPECIAL' ? '专用' : '普通'
  const customerName = order.value.customer?.companyName || order.value.customer?.name || ''
  const taxNumber = order.value.customer?.taxNumber || ''
  const address = order.value.customer?.companyAddress || ''
  const bankInfo = (order.value.customer?.bankName || '') + ' ' + (order.value.customer?.bankAccount || '')
  const orderNum = order.value.orderNumber
  const totalAmount = orderTotal.value.toFixed(2)
  const taxRate = (invoiceForm.value.taxRate * 100).toFixed(0)
  const taxAmountValue = taxAmount.value
  const totalWithTax = amountWithTax.value
  
  const itemsHtml = order.value.items?.map(item => 
    '<tr>' +
    '<td>' + (item.product?.name || '') + '</td>' +
    '<td>' + (item.product?.specification || '-') + '</td>' +
    '<td class="amount">' + item.quantity + '</td>' +
    '<td class="amount">¥' + item.unitPrice?.toFixed(2) + '</td>' +
    '<td class="amount">¥' + item.subtotal?.toFixed(2) + '</td>' +
    '</tr>'
  ).join('') || ''
  
  const htmlContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <style>
        body { font-family: 'SimSun', serif; padding: 40px; background: #f5f5f5; }
        .invoice { background: white; border: 2px solid #000; padding: 40px; max-width: 900px; margin: 0 auto; }
        .header { text-align: center; font-size: 28px; font-weight: bold; margin-bottom: 30px; color: #333; }
        .invoice-no { text-align: right; color: #d32f2f; margin-bottom: 25px; font-size: 16px; }
        .section { margin: 25px 0; }
        .section-title { font-weight: bold; background: #f0f0f0; padding: 10px 15px; margin: 15px 0; border-left: 4px solid #1976d2; }
        .info-row { margin: 12px 0; line-height: 1.8; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
        th, td { border: 1px solid #333; padding: 12px; text-align: left; }
        th { background: #f5f5f5; font-weight: 600; }
        .amount { text-align: right; }
        .total-row { font-weight: bold; background: #fafafa; }
        .highlight-row { font-size: 18px; background: #fff3e0; }
        .highlight-row td:last-child { color: #d32f2f; font-size: 20px; font-weight: 700; }
        .footer { margin-top: 40px; }
        .note-text { min-height: 60px; color: #666; }
        .stamp { text-align: right; margin-top: 50px; color: #666; }
        .stamp-text { margin-top: 30px; font-size: 14px; }
        .stamp-notice { margin-top: 50px; color: #999; }
      </style>
    </head>
    <body>
      <div class="invoice">
        <div class="header">增值税` + invoiceType + `发票</div>
        <div class="invoice-no">发票号码：` + invoiceNumber + `</div>
        
        <div class="section">
          <div class="section-title">购买方信息</div>
          <div class="info-row"><strong>名称：</strong>` + customerName + `</div>
          <div class="info-row"><strong>纳税人识别号：</strong>` + taxNumber + `</div>
          <div class="info-row"><strong>地址、电话：</strong>` + address + `</div>
          <div class="info-row"><strong>开户行及账号：</strong>` + bankInfo + `</div>
        </div>
        
        <div class="section">
          <div class="section-title">订单明细</div>
          <div class="info-row"><strong>订单号：</strong>` + orderNum + `</div>
          <div class="info-row"><strong>开票日期：</strong>` + invoiceDate + `</div>
          
          <table>
            <thead>
              <tr>
                <th>货物或应税劳务名称</th>
                <th>规格型号</th>
                <th class="amount">数量</th>
                <th class="amount">单价</th>
                <th class="amount">金额</th>
              </tr>
            </thead>
            <tbody>
              ` + itemsHtml + `
              <tr class="total-row">
                <td colspan="4">合计金额</td>
                <td class="amount">¥` + totalAmount + `</td>
              </tr>
              <tr class="total-row">
                <td colspan="4">税率 ` + taxRate + `%</td>
                <td class="amount">¥` + taxAmountValue + `</td>
              </tr>
              <tr class="total-row highlight-row">
                <td colspan="4">价税合计（大写）</td>
                <td class="amount">¥` + totalWithTax + `</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div class="footer">
          <div class="section-title">备注</div>
          <p class="note-text">本发票由系统生成，仅供预览使用。</p>
          <div class="stamp">
            <p class="stamp-text">销售方（公章）</p>
            <p class="stamp-notice">[此为系统生成的模拟发票]</p>
          </div>
        </div>
      </div>
    </body>
    </html>
  `
  
  const blob = new Blob([htmlContent], { type: 'text/html' })
  return URL.createObjectURL(blob)
}

const handleIssueInvoice = () => {
  if (!order.value.customer?.taxNumber) {
    ElMessage.warning('客户开票信息不完整')
    return
  }
  
  invoicePdfUrl.value = generateMockInvoicePdf()
  showInvoiceDialog.value = false
  showInvoicePdfDialog.value = true
}

const handleConfirmInvoice = async () => {
  submitting.value = true
  try {
    const data = {
      orderId: order.value.id,
      invoiceType: invoiceForm.value.invoiceType,
      taxRate: invoiceForm.value.taxRate
    }
    
    await invoiceApi.issue(data)
    ElMessage.success('发票开具成功')
    showInvoicePdfDialog.value = false
    
    if (invoicePdfUrl.value) {
      URL.revokeObjectURL(invoicePdfUrl.value)
      invoicePdfUrl.value = ''
    }
    
    await loadOrder()
    await loadInvoice()
  } catch (error) {
    const message = error.response?.data?.message || error.message || '开票失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const handleVoidInvoice = async () => {
  try {
    const result = await ElMessageBox.prompt('请输入作废原因', '作废发票', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入作废原因'
    })
    
    submitting.value = true
    await invoiceApi.void(invoice.value.id, result.value)
    ElMessage.success('发票已作废')
    
    await loadOrder()
    await loadInvoice()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('作废失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadOrder()
  await loadInvoice()
  loadProducts()
  loadCustomers()
})
</script>

<style scoped>
.order-detail-page {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* Card Styles */
.status-card,
.info-card,
.items-card,
.invoice-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.card-title .el-icon {
  font-size: 18px;
  color: #409eff;
}

/* Status Card */
.status-actions {
  text-align: center;
  margin-top: 24px;
}

:deep(.el-steps) {
  padding: 0 20px;
}

:deep(.el-step__title) {
  font-size: 14px;
  font-weight: 500;
}

/* Info Items */
.info-item {
  padding: 12px 0;
  display: flex;
  align-items: center;
}

.info-label {
  font-weight: 500;
  color: #606266;
  min-width: 90px;
}

.info-value {
  color: #303133;
  flex: 1;
}

/* Product Cell */
.product-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.product-spec {
  font-size: 12px;
  color: #909399;
}

/* Price Display */
.price-text {
  color: #606266;
  font-weight: 500;
}

.subtotal-text {
  color: #409eff;
  font-weight: 600;
  font-size: 15px;
}

.total-amount {
  color: #f56c6c !important;
  font-weight: 700;
  font-size: 18px;
}

/* Order Summary */
.order-summary {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.summary-row.total {
  padding: 12px 0;
}

.summary-label {
  font-size: 15px;
  color: #606266;
  font-weight: 500;
}

.summary-value {
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

/* Invoice Section */
.invoice-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
}

.invoice-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.invoice-info .info-item {
  padding: 8px 0;
}

/* Invoice Calculation */
.invoice-calculation {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.calc-row.total {
  padding: 16px 0;
}

.calc-label {
  font-size: 15px;
  color: #606266;
  font-weight: 500;
}

.calc-value {
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

/* Loading Preview */
.loading-preview {
  text-align: center;
  padding: 100px 50px;
  background: #fafafa;
  border-radius: 4px;
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
  
  .header-left {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>