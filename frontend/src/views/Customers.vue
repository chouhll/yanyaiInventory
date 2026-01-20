<template>
  <div class="customers-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>客户列表</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            添加客户
          </el-button>
        </div>
      </template>

      <el-table :data="customers" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="客户编号" width="100" />
        <el-table-column prop="name" label="客户名称" width="150" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column label="开票信息" min-width="250">
          <template #default="{ row }">
            <div v-if="row.companyName" class="invoice-info">
              <div><strong>{{ row.companyName }}</strong></div>
              <div class="info-detail">税号: {{ row.taxNumber || '-' }}</div>
            </div>
            <span v-else class="text-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
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

    <!-- 添加客户对话框 -->
    <el-dialog v-model="showAddDialog" title="添加客户" width="700px">
      <el-form :model="form" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        
        <el-form-item label="客户名称" required>
          <el-input v-model="form.name" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入联系地址" />
        </el-form-item>
        
        <el-divider content-position="left">开票信息</el-divider>
        
        <el-form-item label="公司抬头">
          <el-input v-model="form.companyName" placeholder="请输入公司抬头" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNumber" placeholder="请输入税号" />
        </el-form-item>
        <el-form-item label="公司地址">
          <el-input v-model="form.companyAddress" type="textarea" :rows="2" placeholder="请输入公司地址" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="form.bankName" placeholder="请输入开户银行" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑客户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑客户" width="700px">
      <el-form :model="form" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        
        <el-form-item label="客户名称" required>
          <el-input v-model="form.name" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入联系地址" />
        </el-form-item>
        
        <el-divider content-position="left">开票信息</el-divider>
        
        <el-form-item label="公司抬头">
          <el-input v-model="form.companyName" placeholder="请输入公司抬头" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNumber" placeholder="请输入税号" />
        </el-form-item>
        <el-form-item label="公司地址">
          <el-input v-model="form.companyAddress" type="textarea" :rows="2" placeholder="请输入公司地址" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="form.bankName" placeholder="请输入开户银行" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerApi } from '../api'

const customers = ref([])
const loading = ref(false)
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const submitting = ref(false)
const editingId = ref(null)

const form = ref({
  name: '',
  email: '',
  phone: '',
  address: '',
  taxNumber: '',
  companyName: '',
  companyAddress: '',
  bankName: '',
  bankAccount: ''
})

const loadCustomers = async () => {
  loading.value = true
  try {
    customers.value = await customerApi.getAll()
  } catch (error) {
    ElMessage.error('加载客户列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写客户名称')
    return
  }

  submitting.value = true
  try {
    await customerApi.create(form.value)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    form.value = {
      name: '',
      email: '',
      phone: '',
      address: '',
      taxNumber: '',
      companyName: '',
      companyAddress: '',
      bankName: '',
      bankAccount: ''
    }
    loadCustomers()
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

const handleEdit = (row) => {
  editingId.value = row.id
  form.value = {
    name: row.name || '',
    email: row.email || '',
    phone: row.phone || '',
    address: row.address || '',
    taxNumber: row.taxNumber || '',
    companyName: row.companyName || '',
    companyAddress: row.companyAddress || '',
    bankName: row.bankName || '',
    bankAccount: row.bankAccount || ''
  }
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写客户名称')
    return
  }

  submitting.value = true
  try {
    await customerApi.update(editingId.value, form.value)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    form.value = {
      name: '',
      email: '',
      phone: '',
      address: '',
      taxNumber: '',
      companyName: '',
      companyAddress: '',
      bankName: '',
      bankAccount: ''
    }
    editingId.value = null
    loadCustomers()
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除客户"${row.name}"吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await customerApi.delete(row.id)
    ElMessage.success('删除成功')
    loadCustomers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadCustomers()
})
</script>

<style scoped>
.customers-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.invoice-info {
  font-size: 13px;
  line-height: 1.6;
}

.info-detail {
  color: #606266;
  font-size: 12px;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>