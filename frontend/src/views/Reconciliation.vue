<template>
  <div class="reconciliation">
    <h1>对账管理</h1>
    
    <!-- 操作按钮 -->
    <div class="action-bar">
      <button @click="showCreateDialog = true" class="create-btn">
        <span>+ 创建对账单</span>
      </button>
      <div class="filter-buttons">
        <button 
          v-for="type in reconciliationTypes" 
          :key="type.key"
          @click="filterType = type.key"
          :class="['filter-btn', { active: filterType === type.key }]"
        >
          {{ type.label }}
        </button>
      </div>
    </div>

    <!-- 对账单列表 -->
    <div class="reconciliation-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="filteredReconciliations.length === 0" class="no-data">暂无对账单</div>
      <table v-else class="reconciliation-table">
        <thead>
          <tr>
            <th>对账单号</th>
            <th>类型</th>
            <th>对象</th>
            <th>对账期间</th>
            <th>我方金额</th>
            <th>对方金额</th>
            <th>差异金额</th>
            <th>状态</th>
            <th>对账日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rec in filteredReconciliations" :key="rec.id">
            <td class="rec-number">{{ rec.reconciliationNumber }}</td>
            <td>
              <span :class="['type-badge', rec.type.toLowerCase()]">
                {{ rec.type === 'SUPPLIER' ? '供应商' : '客户' }}
              </span>
            </td>
            <td class="name">{{ getPartyName(rec) }}</td>
            <td>{{ formatPeriod(rec.periodStart, rec.periodEnd) }}</td>
            <td class="amount">¥{{ formatAmount(rec.ourAmount) }}</td>
            <td class="amount">¥{{ formatAmount(rec.theirAmount) }}</td>
            <td :class="['amount', getDifferenceClass(rec.differenceAmount)]">
              ¥{{ formatAmount(rec.differenceAmount) }}
            </td>
            <td>
              <span :class="['status-badge', getStatusClass(rec.status)]">
                {{ getStatusLabel(rec.status) }}
              </span>
            </td>
            <td>{{ formatDate(rec.reconciliationDate) }}</td>
            <td class="actions">
              <button @click="viewDetails(rec)" class="action-btn view">查看</button>
              <button 
                v-if="rec.status === 'DRAFT'" 
                @click="submitReconciliation(rec.id)"
                class="action-btn submit"
              >
                提交
              </button>
              <button 
                v-if="rec.status === 'SUBMITTED'" 
                @click="confirmReconciliation(rec.id)"
                class="action-btn confirm"
              >
                确认
              </button>
              <button 
                v-if="rec.status !== 'CONFIRMED'" 
                @click="deleteReconciliation(rec.id)"
                class="action-btn delete"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 创建对账单对话框 -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <h2>创建对账单</h2>
        <div class="form-group">
          <label>对账类型</label>
          <select v-model="newReconciliation.type">
            <option value="SUPPLIER">供应商对账</option>
            <option value="CUSTOMER">客户对账</option>
          </select>
        </div>
        <div class="form-group" v-if="newReconciliation.type === 'SUPPLIER'">
          <label>选择供应商</label>
          <select v-model="newReconciliation.supplierId">
            <option value="">请选择供应商</option>
            <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
              {{ supplier.name }}
            </option>
          </select>
        </div>
        <div class="form-group" v-if="newReconciliation.type === 'CUSTOMER'">
          <label>选择客户</label>
          <select v-model="newReconciliation.customerId">
            <option value="">请选择客户</option>
            <option v-for="customer in customers" :key="customer.id" :value="customer.id">
              {{ customer.name }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label>对账期间开始</label>
          <input type="date" v-model="newReconciliation.periodStart" />
        </div>
        <div class="form-group">
          <label>对账期间结束</label>
          <input type="date" v-model="newReconciliation.periodEnd" />
        </div>
        <div class="dialog-actions">
          <button @click="createReconciliation" class="btn-primary">创建</button>
          <button @click="showCreateDialog = false" class="btn-secondary">取消</button>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <div v-if="showDetailsDialog" class="dialog-overlay" @click.self="showDetailsDialog = false">
      <div class="dialog large">
        <h2>对账单详情</h2>
        <div v-if="selectedReconciliation" class="details-content">
          <div class="detail-row">
            <span class="label">对账单号:</span>
            <span class="value">{{ selectedReconciliation.reconciliationNumber }}</span>
          </div>
          <div class="detail-row">
            <span class="label">状态:</span>
            <span :class="['status-badge', getStatusClass(selectedReconciliation.status)]">
              {{ getStatusLabel(selectedReconciliation.status) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="label">对账期间:</span>
            <span class="value">
              {{ formatPeriod(selectedReconciliation.periodStart, selectedReconciliation.periodEnd) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="label">我方金额:</span>
            <span class="value">¥{{ formatAmount(selectedReconciliation.ourAmount) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">对方金额:</span>
            <span class="value">¥{{ formatAmount(selectedReconciliation.theirAmount) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">差异金额:</span>
            <span :class="['value', getDifferenceClass(selectedReconciliation.differenceAmount)]">
              ¥{{ formatAmount(selectedReconciliation.differenceAmount) }}
            </span>
          </div>
          <div class="detail-row" v-if="selectedReconciliation.details">
            <span class="label">对账明细:</span>
            <pre class="details-text">{{ selectedReconciliation.details }}</pre>
          </div>
          <div class="detail-row" v-if="selectedReconciliation.differenceRemark">
            <span class="label">差异说明:</span>
            <span class="value">{{ selectedReconciliation.differenceRemark }}</span>
          </div>
        </div>
        <div class="dialog-actions">
          <button @click="showDetailsDialog = false" class="btn-secondary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api';

export default {
  name: 'Reconciliation',
  data() {
    return {
      reconciliations: [],
      suppliers: [],
      customers: [],
      loading: false,
      filterType: 'ALL',
      reconciliationTypes: [
        { key: 'ALL', label: '全部' },
        { key: 'SUPPLIER', label: '供应商对账' },
        { key: 'CUSTOMER', label: '客户对账' }
      ],
      showCreateDialog: false,
      showDetailsDialog: false,
      selectedReconciliation: null,
      newReconciliation: {
        type: 'SUPPLIER',
        supplierId: '',
        customerId: '',
        periodStart: '',
        periodEnd: ''
      }
    };
  },
  computed: {
    filteredReconciliations() {
      if (this.filterType === 'ALL') {
        return this.reconciliations;
      }
      return this.reconciliations.filter(r => r.type === this.filterType);
    }
  },
  mounted() {
    this.loadReconciliations();
    this.loadSuppliers();
    this.loadCustomers();
  },
  methods: {
    async loadReconciliations() {
      this.loading = true;
      try {
        const response = await api.get('/reconciliations');
        // 响应拦截器已经返回了response.data，所以这里直接使用response
        this.reconciliations = Array.isArray(response) ? response : [];
      } catch (error) {
        console.error('加载对账单失败:', error);
        alert('加载对账单失败: ' + (error.response?.data?.message || error.message));
      } finally {
        this.loading = false;
      }
    },
    async loadSuppliers() {
      try {
        const response = await api.get('/suppliers');
        // 响应拦截器已经返回了response.data，所以这里直接使用response
        this.suppliers = Array.isArray(response) ? response : [];
      } catch (error) {
        console.error('加载供应商失败:', error);
      }
    },
    async loadCustomers() {
      try {
        const response = await api.get('/customers');
        // 响应拦截器已经返回了response.data，所以这里直接使用response
        this.customers = Array.isArray(response) ? response : [];
      } catch (error) {
        console.error('加载客户失败:', error);
      }
    },
    async createReconciliation() {
      if (!this.newReconciliation.periodStart || !this.newReconciliation.periodEnd) {
        alert('请选择对账期间');
        return;
      }

      const params = {
        periodStart: this.newReconciliation.periodStart,
        periodEnd: this.newReconciliation.periodEnd
      };

      try {
        const endpoint = this.newReconciliation.type === 'SUPPLIER' 
          ? `/reconciliations/supplier?supplierId=${this.newReconciliation.supplierId}`
          : `/reconciliations/customer?customerId=${this.newReconciliation.customerId}`;
        
        await api.post(endpoint, null, { params });
        alert('对账单创建成功');
        this.showCreateDialog = false;
        this.resetNewReconciliation();
        this.loadReconciliations();
      } catch (error) {
        console.error('创建对账单失败:', error);
        alert('创建对账单失败: ' + (error.response?.data?.message || error.message));
      }
    },
    async submitReconciliation(id) {
      if (!confirm('确定要提交此对账单吗？')) return;
      
      try {
        await api.post(`/reconciliations/${id}/submit`);
        alert('对账单已提交');
        this.loadReconciliations();
      } catch (error) {
        console.error('提交对账单失败:', error);
        alert('提交对账单失败: ' + (error.response?.data?.message || error.message));
      }
    },
    async confirmReconciliation(id) {
      const confirmedBy = prompt('请输入确认人姓名:');
      if (!confirmedBy) return;

      try {
        await api.post(`/reconciliations/${id}/confirm`, { confirmedBy });
        alert('对账单已确认');
        this.loadReconciliations();
      } catch (error) {
        console.error('确认对账单失败:', error);
        alert('确认对账单失败: ' + (error.response?.data?.message || error.message));
      }
    },
    async deleteReconciliation(id) {
      if (!confirm('确定要删除此对账单吗？')) return;

      try {
        await api.delete(`/reconciliations/${id}`);
        alert('对账单已删除');
        this.loadReconciliations();
      } catch (error) {
        console.error('删除对账单失败:', error);
        alert('删除对账单失败: ' + (error.response?.data?.message || error.message));
      }
    },
    viewDetails(reconciliation) {
      this.selectedReconciliation = reconciliation;
      this.showDetailsDialog = true;
    },
    resetNewReconciliation() {
      this.newReconciliation = {
        type: 'SUPPLIER',
        supplierId: '',
        customerId: '',
        periodStart: '',
        periodEnd: ''
      };
    },
    getPartyName(rec) {
      return rec.supplier ? rec.supplier.name : rec.customer ? rec.customer.name : '-';
    },
    formatAmount(amount) {
      return amount ? parseFloat(amount).toFixed(2) : '0.00';
    },
    formatDate(date) {
      return date || '-';
    },
    formatPeriod(start, end) {
      return `${start} ~ ${end}`;
    },
    getStatusLabel(status) {
      const labels = {
        'DRAFT': '草稿',
        'SUBMITTED': '已提交',
        'CONFIRMED': '已确认',
        'DISPUTED': '有异议',
        'RESOLVED': '已解决'
      };
      return labels[status] || status;
    },
    getStatusClass(status) {
      const classes = {
        'DRAFT': 'draft',
        'SUBMITTED': 'submitted',
        'CONFIRMED': 'confirmed',
        'DISPUTED': 'disputed',
        'RESOLVED': 'resolved'
      };
      return classes[status] || '';
    },
    getDifferenceClass(amount) {
      const diff = parseFloat(amount || 0);
      if (diff === 0) return 'zero';
      return diff > 0 ? 'positive' : 'negative';
    }
  }
};
</script>

<style scoped>
.reconciliation {
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
  color: #333;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.create-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
}

.create-btn:hover {
  opacity: 0.9;
}

.filter-buttons {
  display: flex;
  gap: 10px;
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover {
  background: #f5f5f5;
}

.filter-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.reconciliation-list {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.loading, .no-data {
  text-align: center;
  padding: 50px;
  color: #999;
}

.reconciliation-table {
  width: 100%;
  border-collapse: collapse;
}

.reconciliation-table th,
.reconciliation-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.reconciliation-table th {
  background: #f5f7fa;
  font-weight: bold;
  color: #333;
}

.rec-number {
  font-family: 'Courier New', monospace;
  color: #409eff;
}

.type-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.type-badge.supplier {
  background: #e1f3d8;
  color: #67c23a;
}

.type-badge.customer {
  background: #f0f9ff;
  color: #409eff;
}

.name {
  font-weight: 500;
}

.amount {
  text-align: right;
  font-family: 'Courier New', monospace;
}

.amount.zero {
  color: #909399;
}

.amount.positive {
  color: #67c23a;
}

.amount.negative {
  color: #f56c6c;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-badge.draft {
  background: #f4f4f5;
  color: #909399;
}

.status-badge.submitted {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-badge.confirmed {
  background: #e1f3d8;
  color: #67c23a;
}

.status-badge.disputed {
  background: #fef0f0;
  color: #f56c6c;
}

.status-badge.resolved {
  background: #f0f9ff;
  color: #409eff;
}

.actions {
  display: flex;
  gap: 5px;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.action-btn.view {
  background: #409eff;
  color: white;
}

.action-btn.submit {
  background: #e6a23c;
  color: white;
}

.action-btn.confirm {
  background: #67c23a;
  color: white;
}

.action-btn.delete {
  background: #f56c6c;
  color: white;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: white;
  padding: 30px;
  border-radius: 8px;
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.dialog.large {
  width: 700px;
}

.dialog h2 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.form-group select,
.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-secondary {
  background: #ddd;
  color: #333;
}

.details-content {
  margin: 20px 0;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.detail-row .label {
  width: 120px;
  font-weight: bold;
  color: #666;
}

.detail-row .value {
  flex: 1;
  color: #333;
}

.details-text {
  white-space: pre-wrap;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
}
</style>