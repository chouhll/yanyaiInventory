<template>
  <div class="warehouses">
    <div class="page-header">
      <h2>🏢 仓库管理</h2>
      <el-button type="primary" @click="showCreateWarehouse">新建仓库</el-button>
    </div>

    <!-- 仓库列表 -->
    <el-card>
      <el-table :data="warehouses" style="width: 100%" stripe>
        <el-table-column prop="code" label="仓库编码" width="120"></el-table-column>
        <el-table-column prop="name" label="仓库名称" width="150"></el-table-column>
        <el-table-column prop="type" label="类型" width="100"></el-table-column>
        <el-table-column prop="address" label="地址" min-width="200"></el-table-column>
        <el-table-column prop="manager" label="负责人" width="100"></el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130"></el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewLocations(row)">库位</el-button>
            <el-button size="small" @click="editWarehouse(row)">编辑</el-button>
            <el-button size="small" :type="row.enabled ? 'warning' : 'success'" 
              @click="toggleStatus(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteWarehouse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑仓库对话框 -->
    <el-dialog 
      v-model="warehouseDialogVisible" 
      :title="isEdit ? '编辑仓库' : '新建仓库'"
      width="600px">
      <el-form :model="warehouseForm" label-width="100px">
        <el-form-item label="仓库编码" required>
          <el-input v-model="warehouseForm.code" placeholder="如: WH001"></el-input>
        </el-form-item>
        <el-form-item label="仓库名称" required>
          <el-input v-model="warehouseForm.name"></el-input>
        </el-form-item>
        <el-form-item label="仓库类型">
          <el-select v-model="warehouseForm.type" placeholder="请选择">
            <el-option label="主仓" value="主仓"></el-option>
            <el-option label="分仓" value="分仓"></el-option>
            <el-option label="中转仓" value="中转仓"></el-option>
            <el-option label="退货仓" value="退货仓"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="warehouseForm.address" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="warehouseForm.manager"></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="warehouseForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="warehouseForm.enabled"></el-switch>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="warehouseForm.remarks" type="textarea" :rows="3"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveWarehouse">保存</el-button>
      </template>
    </el-dialog>

    <!-- 库位管理对话框 -->
    <el-dialog 
      v-model="locationsDialogVisible" 
      :title="`${currentWarehouse?.name} - 库位管理`"
      width="80%"
      top="5vh">
      <div class="locations-header">
        <el-button type="primary" size="small" @click="showCreateLocation">新建库位</el-button>
      </div>
      
      <el-table :data="locations" style="width: 100%" max-height="500">
        <el-table-column prop="code" label="库位编码" width="120"></el-table-column>
        <el-table-column prop="name" label="库位名称" width="150"></el-table-column>
        <el-table-column prop="zone" label="区域" width="100"></el-table-column>
        <el-table-column prop="rackNumber" label="货架号" width="100"></el-table-column>
        <el-table-column prop="level" label="层号" width="80"></el-table-column>
        <el-table-column prop="type" label="类型" width="100"></el-table-column>
        <el-table-column label="容量占用" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="getOccupancyPercentage(row)" 
              :status="getOccupancyStatus(row)"
              :format="() => `${row.occupied || 0}/${row.capacity || 0}`">
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editLocation(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteLocation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 创建/编辑库位对话框 -->
    <el-dialog 
      v-model="locationDialogVisible" 
      :title="isEditLocation ? '编辑库位' : '新建库位'"
      width="500px">
      <el-form :model="locationForm" label-width="100px">
        <el-form-item label="库位编码" required>
          <el-input v-model="locationForm.code" placeholder="如: A-01-01"></el-input>
        </el-form-item>
        <el-form-item label="库位名称" required>
          <el-input v-model="locationForm.name"></el-input>
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="locationForm.zone" placeholder="如: A区"></el-input>
        </el-form-item>
        <el-form-item label="货架号">
          <el-input v-model="locationForm.rackNumber" placeholder="如: 01"></el-input>
        </el-form-item>
        <el-form-item label="层号">
          <el-input v-model="locationForm.level" placeholder="如: 01"></el-input>
        </el-form-item>
        <el-form-item label="库位类型">
          <el-select v-model="locationForm.type" placeholder="请选择">
            <el-option label="普通货架" value="普通货架"></el-option>
            <el-option label="托盘位" value="托盘位"></el-option>
            <el-option label="冷藏位" value="冷藏位"></el-option>
            <el-option label="贵重品区" value="贵重品区"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="locationForm.capacity" :min="0" :step="1"></el-input-number>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="locationForm.enabled"></el-switch>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="locationForm.remarks" type="textarea" :rows="2"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLocation">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api, { warehouseApi } from '../api'

const warehouses = ref([])
const locations = ref([])
const currentWarehouse = ref(null)

const warehouseDialogVisible = ref(false)
const locationsDialogVisible = ref(false)
const locationDialogVisible = ref(false)

const isEdit = ref(false)
const isEditLocation = ref(false)

const warehouseForm = ref({
  code: '',
  name: '',
  type: '',
  address: '',
  manager: '',
  phone: '',
  enabled: true,
  remarks: ''
})

const locationForm = ref({
  code: '',
  name: '',
  zone: '',
  rackNumber: '',
  level: '',
  type: '',
  capacity: null,
  enabled: true,
  remarks: '',
  warehouse: null
})

// 加载仓库列表
const loadWarehouses = async () => {
  try {
    const response = await warehouseApi.getAll()
    // response已经是response.data (被拦截器解包)
    if (response.success) {
      warehouses.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载仓库列表失败')
    console.error(error)
  }
}

// 显示创建仓库对话框
const showCreateWarehouse = () => {
  isEdit.value = false
  warehouseForm.value = {
    code: '',
    name: '',
    type: '',
    address: '',
    manager: '',
    phone: '',
    enabled: true,
    remarks: ''
  }
  warehouseDialogVisible.value = true
}

// 编辑仓库
const editWarehouse = (warehouse) => {
  isEdit.value = true
  warehouseForm.value = { ...warehouse }
  warehouseDialogVisible.value = true
}

// 保存仓库
const saveWarehouse = async () => {
  try {
    console.log('保存仓库 - isEdit:', isEdit.value)
    console.log('保存仓库 - 表单数据:', warehouseForm.value)
    
    let response
    if (isEdit.value) {
      console.log('调用更新API:', `/api/warehouses/${warehouseForm.value.id}`)
      response = await warehouseApi.update(warehouseForm.value.id, warehouseForm.value)
      ElMessage.success('仓库更新成功')
    } else {
      console.log('调用创建API: POST /api/warehouses')
      response = await warehouseApi.create(warehouseForm.value)
      console.log('创建响应:', response)
      ElMessage.success('仓库创建成功')
    }
    warehouseDialogVisible.value = false
    loadWarehouses()
  } catch (error) {
    console.error('保存仓库失败:', error)
    console.error('错误详情:', error.response)
    ElMessage.error(error.response?.data?.message || error.message || '保存失败')
  }
}

// 切换仓库状态
const toggleStatus = async (warehouse) => {
  try {
    await warehouseApi.toggleStatus(warehouse.id)
    ElMessage.success('状态更新成功')
    loadWarehouses()
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

// 删除仓库
const deleteWarehouse = async (warehouse) => {
  try {
    await ElMessageBox.confirm('确定要删除该仓库吗？', '提示', {
      type: 'warning'
    })
    await warehouseApi.delete(warehouse.id)
    ElMessage.success('仓库删除成功')
    loadWarehouses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 查看库位
const viewLocations = async (warehouse) => {
  currentWarehouse.value = warehouse
  try {
    const response = await warehouseApi.getLocations(warehouse.id)
    // response已经是response.data (被拦截器解包)
    if (response.success) {
      locations.value = response.data
    }
    locationsDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载库位失败')
    console.error(error)
  }
}

// 显示创建库位对话框
const showCreateLocation = () => {
  isEditLocation.value = false
  locationForm.value = {
    code: '',
    name: '',
    zone: '',
    rackNumber: '',
    level: '',
    type: '',
    capacity: null,
    enabled: true,
    remarks: '',
    warehouse: { id: currentWarehouse.value.id }
  }
  locationDialogVisible.value = true
}

// 编辑库位
const editLocation = (location) => {
  isEditLocation.value = true
  locationForm.value = { ...location }
  locationDialogVisible.value = true
}

// 保存库位
const saveLocation = async () => {
  try {
    if (isEditLocation.value) {
      await warehouseApi.updateLocation(locationForm.value.id, locationForm.value)
      ElMessage.success('库位更新成功')
    } else {
      await warehouseApi.createLocation(locationForm.value)
      ElMessage.success('库位创建成功')
    }
    locationDialogVisible.value = false
    viewLocations(currentWarehouse.value)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

// 删除库位
const deleteLocation = async (location) => {
  try {
    await ElMessageBox.confirm('确定要删除该库位吗？', '提示', {
      type: 'warning'
    })
    await warehouseApi.deleteLocation(location.id)
    ElMessage.success('库位删除成功')
    viewLocations(currentWarehouse.value)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 计算库位占用率
const getOccupancyPercentage = (location) => {
  if (!location.capacity || location.capacity === 0) return 0
  const occupied = location.occupied || 0
  return Math.round((occupied / location.capacity) * 100)
}

// 获取占用状态
const getOccupancyStatus = (location) => {
  const percentage = getOccupancyPercentage(location)
  if (percentage >= 90) return 'exception'
  if (percentage >= 70) return 'warning'
  return 'success'
}

onMounted(() => {
  loadWarehouses()
})
</script>

<style scoped>
.warehouses {
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

.locations-header {
  margin-bottom: 15px;
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
</style>