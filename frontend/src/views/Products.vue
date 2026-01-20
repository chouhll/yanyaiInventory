<template>
  <div class="products-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>产品列表</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            添加产品
          </el-button>
        </div>
      </template>

      <el-table :data="products" style="width: 100%" v-loading="loading">
        <el-table-column prop="inventoryCode" label="库存编码" width="120" />
        <el-table-column prop="name" label="产品名称" width="180" />
        <el-table-column prop="specification" label="规格型号" width="150" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="stock" label="库存" width="100">
          <template #default="{ row }">
            <el-tag :type="row.stock > 10 ? 'success' : 'warning'">
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120">
          <template #default="{ row }">
            ¥{{ row.price?.toFixed(2) || '0.00' }}
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

    <!-- 添加产品对话框 -->
    <el-dialog v-model="showAddDialog" title="添加产品" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="库存编码" required>
          <el-input v-model="form.inventoryCode" placeholder="请输入库存编码" />
        </el-form-item>
        <el-form-item label="产品名称" required>
          <el-input v-model="form.name" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specification" placeholder="请输入规格型号" />
        </el-form-item>
        <el-form-item label="产品分类">
          <el-input v-model="form.category" placeholder="请输入产品分类" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" placeholder="如：个、箱、吨" />
        </el-form-item>
        <el-form-item label="SKU">
          <el-input v-model="form.sku" placeholder="请输入SKU" />
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="form.price" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="库存数量" required>
          <el-input-number v-model="form.stock" :min="0" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi } from '../api'

const products = ref([])
const loading = ref(false)
const showAddDialog = ref(false)
const submitting = ref(false)

const form = ref({
  inventoryCode: '',
  name: '',
  specification: '',
  category: '',
  unit: '',
  sku: '',
  price: 0,
  stock: 0
})

const loadProducts = async () => {
  loading.value = true
  try {
    products.value = await productApi.getAll()
  } catch (error) {
    ElMessage.error('加载产品列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!form.value.inventoryCode || !form.value.name) {
    ElMessage.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    await productApi.create(form.value)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    form.value = {
      inventoryCode: '',
      name: '',
      specification: '',
      category: '',
      unit: '',
      sku: '',
      price: 0,
      stock: 0
    }
    loadProducts()
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除产品"${row.name}"吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await productApi.delete(row.id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.products-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>