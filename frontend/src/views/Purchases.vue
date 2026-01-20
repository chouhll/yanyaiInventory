<template>
  <div class="purchases-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>采购列表</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            创建采购单
          </el-button>
        </div>
      </template>

      <el-table :data="purchases" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="采购单号" width="100" />
        <el-table-column label="产品" width="180">
          <template #default="{ row }">
            {{ row.product?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="120">
          <template #default="{ row }">
            ¥{{ row.unitPrice?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column label="供应商" width="150">
          <template #default="{ row }">
            {{ row.supplier?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="采购日期" width="180">
          <template #default="{ row }">
            {{ formatDate(row.purchaseDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建采购单对话框 -->
    <el-dialog v-model="showAddDialog" title="创建采购单" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="产品" required>
          <el-select v-model="form.productId" placeholder="请选择产品" style="width: 100%">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="采购数量" required>
          <el-input-number v-model="form.quantity" :min="1" :precision="2" />
        </el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="form.unitPrice" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-select v-model="form.supplierId" placeholder="请选择供应商" style="width: 100%">
            <el-option
              v-for="supplier in suppliers"
              :key="supplier.id"
              :label="supplier.name"
              :value="supplier.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-input v-model="form.warehouse" placeholder="请输入仓库位置" />
        </el-form-item>
        <el-form-item label="采购单号">
          <el-input v-model="form.purchaseOrderNo" placeholder="请输入采购单号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="PENDING">待入库</el-radio>
            <el-radio label="COMPLETED">已完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseApi, productApi, supplierApi } from '../api'

const purchases = ref([])
const products = ref([])
const suppliers = ref([])
const loading = ref(false)
const showAddDialog = ref(false)
const submitting = ref(false)

const form = ref({
  productId: null,
  quantity: 1,
  unitPrice: 0,
  supplierId: null,
  warehouse: '',
  purchaseOrderNo: '',
  remarks: '',
  status: 'PENDING'
})

const loadPurchases = async () => {
  loading.value = true
  try {
    purchases.value = await purchaseApi.getAll()
  } catch (error) {
    ElMessage.error('加载采购列表失败')
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

const loadSuppliers = async () => {
  try {
    suppliers.value = await supplierApi.getAll()
  } catch (error) {
    ElMessage.error('加载供应商列表失败')
  }
}

const handleAdd = async () => {
  if (!form.value.productId || !form.value.supplierId) {
    ElMessage.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    const totalAmount = form.value.quantity * form.value.unitPrice
    
    await purchaseApi.create({
      product: { id: form.value.productId },
      quantity: form.value.quantity,
      unitPrice: form.value.unitPrice,
      totalAmount: totalAmount,
      supplier: { id: form.value.supplierId },
      warehouse: form.value.warehouse,
      purchaseOrderNo: form.value.purchaseOrderNo,
      remarks: form.value.remarks,
      status: form.value.status,
      purchaseDate: new Date().toISOString()
    })
    
    ElMessage.success('创建成功')
    showAddDialog.value = false
    form.value = {
      productId: null,
      quantity: 1,
      unitPrice: 0,
      supplierId: null,
      warehouse: '',
      purchaseOrderNo: '',
      remarks: '',
      status: 'PENDING'
    }
    loadPurchases()
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待入库',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除采购单 #${row.id} 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await purchaseApi.delete(row.id)
    ElMessage.success('删除成功')
    loadPurchases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadPurchases()
  loadProducts()
  loadSuppliers()
})
</script>

<style scoped>
.purchases-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
