<template>
  <div class="tax-management">
    <h1>税务管理</h1>
    
    <!-- 日期选择器 -->
    <div class="date-selector">
      <div class="date-buttons">
        <button 
          v-for="period in datePeriods" 
          :key="period.key"
          @click="selectDatePeriod(period.key)"
          :class="['date-btn', { active: selectedPeriod === period.key }]"
        >
          {{ period.label }}
        </button>
      </div>
      <div class="custom-date-range" v-if="selectedPeriod === 'custom'">
        <input type="datetime-local" v-model="customStartDate" />
        <span>至</span>
        <input type="datetime-local" v-model="customEndDate" />
        <button @click="loadData" class="refresh-btn">查询</button>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.key"
        @click="activeTab = tab.key"
        :class="['tab', { active: activeTab === tab.key }]"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 进项发票 -->
    <div v-if="activeTab === 'input'" class="tab-content">
      <div class="actions-bar">
        <button @click="showCreateInputInvoiceDialog = true" class="create-btn">
          ➕ 录入进项发票
        </button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="inputInvoices.length === 0" class="no-data">暂无进项发票数据</div>
      <table v-else class="invoice-table">
        <thead>
          <tr>
            <th>发票号码</th>
            <th>供应商</th>
            <th>发票日期</th>
            <th>发票类型</th>
            <th>金额（不含税）</th>
            <th>税额</th>
            <th>价税合计</th>
            <th>认证状态</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="invoice in inputInvoices" :key="invoice.id">
            <td>{{ invoice.invoiceNumber }}</td>
            <td>{{ invoice.companyName }}</td>
            <td>{{ formatDateTime(invoice.invoiceDate) }}</td>
            <td>
              <span :class="['badge', invoice.invoiceType === 'SPECIAL' ? 'badge-special' : 'badge-normal']">
                {{ invoice.invoiceType === 'SPECIAL' ? '专用发票' : '普通发票' }}
              </span>
            </td>
            <td class="amount">¥{{ formatAmount(invoice.totalAmount) }}</td>
            <td class="amount">¥{{ formatAmount(invoice.taxAmount) }}</td>
            <td class="amount">¥{{ formatAmount(invoice.amountWithTax) }}</td>
            <td>
              <span v-if="invoice.isAuthenticated" class="badge badge-success">
                已认证 ({{ formatDate(invoice.authenticationDate) }})
              </span>
              <span v-else class="badge badge-warning">未认证</span>
            </td>
            <td>
              <span :class="['badge', getStatusClass(invoice.status)]">
                {{ getStatusText(invoice.status) }}
              </span>
            </td>
            <td>
              <button 
                v-if="!invoice.isAuthenticated && invoice.status === 'ISSUED'"
                @click="authenticateInvoice(invoice.id)"
                class="action-btn auth-btn"
              >
                认证
              </button>
              <button 
                @click="viewInvoiceDetails(invoice)"
                class="action-btn view-btn"
              >
                查看
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 销项发票 -->
    <div v-if="activeTab === 'output'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="outputInvoices.length === 0" class="no-data">暂无销项发票数据</div>
      <table v-else class="invoice-table">
        <thead>
          <tr>
            <th>发票号码</th>
            <th>客户</th>
            <th>发票日期</th>
            <th>发票类型</th>
            <th>金额（不含税）</th>
            <th>税额</th>
            <th>价税合计</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="invoice in outputInvoices" :key="invoice.id">
            <td>{{ invoice.invoiceNumber }}</td>
            <td>{{ invoice.companyName }}</td>
            <td>{{ formatDateTime(invoice.invoiceDate) }}</td>
            <td>
              <span :class="['badge', invoice.invoiceType === 'SPECIAL' ? 'badge-special' : 'badge-normal']">
                {{ invoice.invoiceType === 'SPECIAL' ? '专用发票' : '普通发票' }}
              </span>
            </td>
            <td class="amount">¥{{ formatAmount(invoice.totalAmount) }}</td>
            <td class="amount">¥{{ formatAmount(invoice.taxAmount) }}</td>
            <td class="amount">¥{{ formatAmount(invoice.amountWithTax) }}</td>
            <td>
              <span :class="['badge', getStatusClass(invoice.status)]">
                {{ getStatusText(invoice.status) }}
              </span>
            </td>
            <td>
              <button 
                @click="viewInvoiceDetails(invoice)"
                class="action-btn view-btn"
              >
                查看
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 税务申报 -->
    <div v-if="activeTab === 'declaration'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="declaration-content">
        <h2>增值税申报汇总表</h2>
        <div class="declaration-period">
          申报期间：{{ formatDateTime(dateRange.startDate) }} 至 {{ formatDateTime(dateRange.endDate) }}
        </div>
        
        <div class="declaration-cards">
          <div class="decl-card">
            <div class="decl-label">销项税额</div>
            <div class="decl-value output-tax">¥{{ formatAmount(taxDeclaration.outputTaxAmount) }}</div>
            <div class="decl-detail">销售额: ¥{{ formatAmount(taxDeclaration.outputAmount) }}</div>
            <div class="decl-detail">发票数量: {{ taxDeclaration.outputInvoiceCount }}</div>
          </div>

          <div class="decl-card">
            <div class="decl-label">进项税额</div>
            <div class="decl-value input-tax">¥{{ formatAmount(taxDeclaration.inputTaxAmount) }}</div>
            <div class="decl-detail">采购额: ¥{{ formatAmount(taxDeclaration.inputAmount) }}</div>
            <div class="decl-detail">
              已认证: {{ taxDeclaration.authenticatedInputCount }} / {{ taxDeclaration.inputInvoiceCount }}
            </div>
          </div>

          <div class="decl-card highlight">
            <div class="decl-label">应纳税额</div>
            <div class="decl-value payable-tax">¥{{ formatAmount(taxDeclaration.payableTaxAmount) }}</div>
            <div class="decl-formula">销项税额 - 进项税额</div>
          </div>
        </div>

        <div class="declaration-table">
          <h3>增值税纳税申报表（简表）</h3>
          <table class="tax-table">
            <thead>
              <tr>
                <th>栏次</th>
                <th>项目</th>
                <th>金额（元）</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>1</td>
                <td>销售额（不含税）</td>
                <td class="amount">{{ formatAmount(taxDeclaration.outputAmount) }}</td>
              </tr>
              <tr class="highlight-row">
                <td>2</td>
                <td>销项税额</td>
                <td class="amount">{{ formatAmount(taxDeclaration.outputTaxAmount) }}</td>
              </tr>
              <tr>
                <td>3</td>
                <td>进项税额</td>
                <td class="amount">{{ formatAmount(taxDeclaration.inputTaxAmount) }}</td>
              </tr>
              <tr>
                <td>4</td>
                <td>上期留抵税额</td>
                <td class="amount">0.00</td>
              </tr>
              <tr>
                <td>5</td>
                <td>应抵扣税额合计（3+4）</td>
                <td class="amount">{{ formatAmount(taxDeclaration.inputTaxAmount) }}</td>
              </tr>
              <tr class="total-row">
                <td>6</td>
                <td>应纳税额（2-5）</td>
                <td class="amount bold">{{ formatAmount(taxDeclaration.payableTaxAmount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 发票统计 -->
    <div v-if="activeTab === 'statistics'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="statistics-content">
        <h2>按客户统计发票</h2>
        <div v-if="customerStatistics.length === 0" class="no-data">暂无统计数据</div>
        <table v-else class="statistics-table">
          <thead>
            <tr>
              <th>客户名称</th>
              <th>发票数量</th>
              <th>销售额（不含税）</th>
              <th>税额</th>
              <th>价税合计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="stat in customerStatistics" :key="stat.customerId">
              <td>{{ stat.customerName }}</td>
              <td>{{ stat.invoiceCount }}</td>
              <td class="amount">¥{{ formatAmount(stat.totalAmount) }}</td>
              <td class="amount">¥{{ formatAmount(stat.totalTax) }}</td>
              <td class="amount">¥{{ formatAmount(stat.totalAmountWithTax) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td>合计</td>
              <td>{{ totalStatistics.invoiceCount }}</td>
              <td class="amount">¥{{ formatAmount(totalStatistics.totalAmount) }}</td>
              <td class="amount">¥{{ formatAmount(totalStatistics.totalTax) }}</td>
              <td class="amount">¥{{ formatAmount(totalStatistics.totalAmountWithTax) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- 创建进项发票对话框 -->
    <div v-if="showCreateInputInvoiceDialog" class="modal-overlay" @click.self="showCreateInputInvoiceDialog = false">
      <div class="modal-content">
        <h2>录入进项发票</h2>
        <form @submit.prevent="createInputInvoice">
          <div class="form-group">
            <label>采购单ID *</label>
            <input type="number" v-model="newInputInvoice.purchaseId" required />
          </div>
          <div class="form-group">
            <label>发票号码 *</label>
            <input type="text" v-model="newInputInvoice.invoiceNumber" required />
          </div>
          <div class="form-group">
            <label>发票类型 *</label>
            <select v-model="newInputInvoice.invoiceType" required>
              <option value="SPECIAL">专用发票</option>
              <option value="NORMAL">普通发票</option>
            </select>
          </div>
          <div class="form-group">
            <label>税率 *</label>
            <input type="number" step="0.01" v-model="newInputInvoice.taxRate" required />
            <small>例如：13% 填写 0.13</small>
          </div>
          <div class="form-group">
            <label>发票日期</label>
            <input type="datetime-local" v-model="newInputInvoice.invoiceDate" />
          </div>
          <div class="form-actions">
            <button type="button" @click="showCreateInputInvoiceDialog = false" class="cancel-btn">
              取消
            </button>
            <button type="submit" class="submit-btn">创建</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api';

export default {
  name: 'TaxManagement',
  data() {
    return {
      activeTab: 'input',
      tabs: [
        { key: 'input', label: '进项发票' },
        { key: 'output', label: '销项发票' },
        { key: 'declaration', label: '税务申报' },
        { key: 'statistics', label: '发票统计' }
      ],
      datePeriods: [
        { key: 'today', label: '今天' },
        { key: 'thisWeek', label: '本周' },
        { key: 'thisMonth', label: '本月' },
        { key: 'thisYear', label: '本年' },
        { key: 'lastSixMonths', label: '最近6个月' },
        { key: 'custom', label: '自定义' }
      ],
      selectedPeriod: 'lastSixMonths',
      customStartDate: '',
      customEndDate: '',
      dateRange: {
        startDate: null,
        endDate: null
      },
      loading: false,
      inputInvoices: [],
      outputInvoices: [],
      taxDeclaration: {
        outputTaxAmount: 0,
        inputTaxAmount: 0,
        payableTaxAmount: 0,
        outputAmount: 0,
        inputAmount: 0,
        outputInvoiceCount: 0,
        inputInvoiceCount: 0,
        authenticatedInputCount: 0
      },
      customerStatistics: [],
      showCreateInputInvoiceDialog: false,
      newInputInvoice: {
        purchaseId: null,
        invoiceNumber: '',
        invoiceType: 'SPECIAL',
        taxRate: 0.13,
        invoiceDate: null
      }
    };
  },
  computed: {
    totalStatistics() {
      return {
        invoiceCount: this.customerStatistics.reduce((sum, s) => sum + s.invoiceCount, 0),
        totalAmount: this.customerStatistics.reduce((sum, s) => sum + parseFloat(s.totalAmount || 0), 0),
        totalTax: this.customerStatistics.reduce((sum, s) => sum + parseFloat(s.totalTax || 0), 0),
        totalAmountWithTax: this.customerStatistics.reduce((sum, s) => sum + parseFloat(s.totalAmountWithTax || 0), 0)
      };
    }
  },
  mounted() {
    this.selectDatePeriod('lastSixMonths');
  },
  methods: {
    selectDatePeriod(period) {
      this.selectedPeriod = period;
      const now = new Date();
      let startDate, endDate;

      switch (period) {
        case 'today':
          startDate = new Date(now.setHours(0, 0, 0, 0));
          endDate = new Date(now.setHours(23, 59, 59, 999));
          break;
        case 'thisWeek':
          const dayOfWeek = now.getDay();
          const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
          startDate = new Date(now.setDate(diff));
          startDate.setHours(0, 0, 0, 0);
          endDate = new Date(startDate);
          endDate.setDate(startDate.getDate() + 6);
          endDate.setHours(23, 59, 59, 999);
          break;
        case 'thisMonth':
          startDate = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
          endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
          break;
        case 'thisYear':
          startDate = new Date(now.getFullYear(), 0, 1, 0, 0, 0, 0);
          endDate = new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999);
          break;
        case 'lastSixMonths':
          startDate = new Date(now);
          startDate.setMonth(now.getMonth() - 6);
          startDate.setHours(0, 0, 0, 0);
          endDate = new Date(now.setHours(23, 59, 59, 999));
          break;
        case 'custom':
          return;
      }

      this.dateRange.startDate = startDate.toISOString();
      this.dateRange.endDate = endDate.toISOString();
      this.loadData();
    },
    async loadData() {
      if (this.selectedPeriod === 'custom') {
        if (!this.customStartDate || !this.customEndDate) {
          alert('请选择日期范围');
          return;
        }
        this.dateRange.startDate = new Date(this.customStartDate).toISOString();
        this.dateRange.endDate = new Date(this.customEndDate).toISOString();
      }

      this.loading = true;
      try {
        await Promise.all([
          this.loadInputInvoices(),
          this.loadOutputInvoices(),
          this.loadTaxDeclaration(),
          this.loadCustomerStatistics()
        ]);
      } catch (error) {
        console.error('加载数据失败:', error);
        alert('加载数据失败: ' + (error.response?.data?.message || error.message));
      } finally {
        this.loading = false;
      }
    },
    async loadInputInvoices() {
      const response = await api.get('/invoices/input', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.inputInvoices = Array.isArray(response) ? response : [];
    },
    async loadOutputInvoices() {
      const response = await api.get('/invoices/output', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.outputInvoices = Array.isArray(response) ? response : [];
    },
    async loadTaxDeclaration() {
      const response = await api.get('/invoices/tax-declaration', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.taxDeclaration = response || {
        outputTaxAmount: 0,
        inputTaxAmount: 0,
        payableTaxAmount: 0,
        outputAmount: 0,
        inputAmount: 0,
        outputInvoiceCount: 0,
        inputInvoiceCount: 0,
        authenticatedInputCount: 0
      };
    },
    async loadCustomerStatistics() {
      const response = await api.get('/invoices/statistics/by-customer', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.customerStatistics = Array.isArray(response) ? response : [];
    },
    async createInputInvoice() {
      try {
        await api.post('/invoices/input', this.newInputInvoice);
        alert('进项发票创建成功');
        this.showCreateInputInvoiceDialog = false;
        this.newInputInvoice = {
          purchaseId: null,
          invoiceNumber: '',
          invoiceType: 'SPECIAL',
          taxRate: 0.13,
          invoiceDate: null
        };
        await this.loadData();
      } catch (error) {
        console.error('创建进项发票失败:', error);
        alert('创建失败: ' + (error.response?.data?.message || error.message));
      }
    },
    async authenticateInvoice(invoiceId) {
      const remark = prompt('请输入认证备注（可选）:');
      if (remark === null) return;

      try {
        await api.post(`/invoices/${invoiceId}/authenticate`, { remark: remark || '' });
        alert('发票认证成功');
        await this.loadData();
      } catch (error) {
        console.error('认证失败:', error);
        alert('认证失败: ' + (error.response?.data?.message || error.message));
      }
    },
    viewInvoiceDetails(invoice) {
      alert('发票详情:\n' + JSON.stringify(invoice, null, 2));
    },
    formatAmount(amount) {
      return amount ? parseFloat(amount).toFixed(2) : '0.00';
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '-';
      return new Date(dateTime).toLocaleString('zh-CN');
    },
    formatDate(date) {
      if (!date) return '-';
      return new Date(date).toLocaleDateString('zh-CN');
    },
    getStatusText(status) {
      const statusMap = {
        'ISSUED': '已开具',
        'VOIDED': '已作废',
        'RETURNED': '已红冲'
      };
      return statusMap[status] || status;
    },
    getStatusClass(status) {
      const classMap = {
        'ISSUED': 'badge-success',
        'VOIDED': 'badge-danger',
        'RETURNED': 'badge-warning'
      };
      return classMap[status] || 'badge-default';
    }
  }
};
</script>

<style scoped>
.tax-management {
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
  color: #333;
}

.date-selector {
  background: white;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.date-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.date-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.date-btn:hover {
  background: #f5f5f5;
}

.date-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.custom-date-range {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 10px;
}

.custom-date-range input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.refresh-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  background: white;
  border-radius: 8px 8px 0 0;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.tab {
  flex: 1;
  padding: 15px;
  text-align: center;
  cursor: pointer;
  background: white;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}

.tab:hover {
  background: #f5f5f5;
}

.tab.active {
  border-bottom-color: #409eff;
  color: #409eff;
  font-weight: bold;
}

.tab-content {
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  min-height: 400px;
}

.actions-bar {
  margin-bottom: 20px;
}

.create-btn {
  padding: 10px 20px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.create-btn:hover {
  background: #5daf34;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #999;
}

.no-data {
  text-align: center;
  padding: 50px;
  color: #999;
}

.invoice-table,
.statistics-table,
.tax-table {
  width: 100%;
  border-collapse: collapse;
}

.invoice-table th,
.invoice-table td,
.statistics-table th,
.statistics-table td,
.tax-table th,
.tax-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.invoice-table th,
.statistics-table th,
.tax-table th {
  background: #f5f7fa;
  font-weight: bold;
  color: #333;
}

.amount {
  text-align: right;
  font-family: 'Courier New', monospace;
}

.badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.badge-special {
  background: #e1f3d8;
  color: #67c23a;
}

.badge-normal {
  background: #f0f9ff;
  color: #409eff;
}

.badge-success {
  background: #e1f3d8;
  color: #67c23a;
}

.badge-warning {
  background: #fdf6ec;
  color: #e6a23c;
}

.badge-danger {
  background: #fef0f0;
  color: #f56c6c;
}

.action-btn {
  padding: 5px 10px;
  margin: 0 5px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.auth-btn {
  background: #67c23a;
  color: white;
}

.view-btn {
  background: #409eff;
  color: white;
}

.declaration-content {
  max-width: 1200px;
}

.declaration-period {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.declaration-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.decl-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 8px;
  color: white;
}

.decl-card.highlight {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.decl-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.decl-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.decl-detail {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 5px;
}

.decl-formula {
  font-size: 12px;
  opacity: 0.8;
  font-style: italic;
}

.declaration-table h3 {
  margin-bottom: 15px;
  color: #333;
}

.highlight-row {
  background: #e1f3d8;
}

.total-row {
  background: #f5f7fa;
  font-weight: bold;
}

.bold {
  font-weight: bold;
  color: #f56c6c;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  min-width: 500px;
  max-width: 600px;
}

.modal-content h2 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group small {
  color: #999;
  font-size: 12px;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.cancel-btn,
.submit-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.submit-btn {
  background: #409eff;
  color: white;
}

.submit-btn:hover {
  background: #3a8ee6;
}
</style>