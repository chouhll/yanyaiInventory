<template>
  <div class="suppliers-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span style="font-size: 18px; font-weight: 600;">供应商管理</span>
          <el-button type="primary" @click="showDialog = true">
            <el-icon><Plus /></el-icon>
            新增供应商
          </el-button>
        </div>
      </template>

      <el-table :data="suppliers" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="供应商名称" min-width="150" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="taxNumber" label="税号" min-width="180" />
        <el-table-column prop="creditRating" label="信用等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.creditRating" :type="getCreditType(row.creditRating)">
              {{ row.creditRating }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="row.active ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.active ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="showDialog" 
      :title="isEditing ? '编辑供应商' : '新增供应商'" 
      width="700px"
    >
      <el-form :model="form" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="form.address" placeholder="请输入联系地址" />
        </el-form-item>
        <el-form-item label="电子邮箱">
          <el-input v-model="form.email" placeholder="请输入电子邮箱" />
        </el-form-item>

        <el-divider content-position="left">开票信息</el-divider>
        <el-form-item label="公司名称">
          <el-input v-model="form.companyName" placeholder="用于开票的公司名称" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNumber" placeholder="纳税人识别号" />
        </el-form-item>
        <el-form-item label="公司地址">
          <el-input v-model="form.companyAddress" placeholder="用于开票的公司地址" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="form.bankName" placeholder="开户银行名称" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="银行账号" />
        </el-form-item>

        <el-divider content-position="left">业务信息</el-divider>
        <el-form-item label="信用等级">
          <el-select v-model="form.creditRating" placeholder="请选择信用等级">
            <el-option label="A (优秀)" value="A" />
            <el-option label="B (良好)" value="B" />
            <el-option label="C (一般)" value="C" />
            <el-option label="D (较差)" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="结算方式">
          <el-select v-model="form.paymentMethod" placeholder="请选择结算方式">
            <el-option label="现金" value="CASH" />
            <el-option label="转账" value="TRANSFER" />
            <el-option label="支票" value="CHECK" />
            <el-option label="赊账" value="CREDIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期（天）">
          <el-input-number v-model="form.paymentTermDays" :min="0" :max="365" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="form.remarks" 
            type="textarea" 
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEditing ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { supplierApi } from '../api'

const suppliers = ref([])
const loading = ref(false)
const showDialog = ref(false)
const isEditing = ref(false)
const submitting = ref(false)

const form = ref({
  name: '',
  contactPerson: '',
  phone: '',
  address: '',
  email: '',
  companyName: '',
  taxNumber: '',
  companyAddress: '',
  bankName: '',
  bankAccount: '',
  creditRating: '',
  paymentMethod: '',
  paymentTermDays: 0,
  remarks: '',
  active: true
})

const loadSuppliers = async () => {
  loading.value = true
  try {
    suppliers.value = await supplierApi.getAll()
  } catch (error) {
    ElMessage.error('加载供应商列表失败')
  } finally {
    loading.value = false
  }
}

const getCreditType = (rating) => {
  const types = { 'A': 'success', 'B': '', 'C': 'warning', 'D': 'danger' }
  return types[rating] || 'info'
}

const handleEdit = (row) => {
  isEditing.value = true
  form.value = { ...row }
  showDialog.value = true
}

const handleSubmit = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入供应商名称')
    return
  }

  submitting.value = true
  try {
    if (isEditing.value) {
      await supplierApi.update(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await supplierApi.create(form.value)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadSuppliers()
    resetForm()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row) => {
  try {
    await supplierApi.toggleStatus(row.id, !row.active)
    ElMessage.success(row.active ? '已禁用' : '已启用')
    loadSuppliers()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该供应商吗？', '提示', {
      type: 'warning'
    })
    await supplierApi.delete(row.id)
    ElMessage.success('删除成功')
    loadSuppliers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetForm = () => {
  form.value = {
    name: '',
    contactPerson: '',
    phone: '',
    address: '',
    email: '',
    companyName: '',
    taxNumber: '',
    companyAddress: '',
    bankName: '',
    bankAccount: '',
    creditRating: '',
    paymentMethod: '',
    paymentTermDays: 0,
    remarks: '',
    active: true
  }
  isEditing.value = false
}

onMounted(() => {
  loadSuppliers()
})
</script>

<style scoped>
.suppliers-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>