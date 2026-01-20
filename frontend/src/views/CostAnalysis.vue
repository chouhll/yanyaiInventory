<template>
  <div class="cost-analysis">
    <h1>成本分析</h1>
    
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

    <!-- 产品利润分析 -->
    <div v-if="activeTab === 'products'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <div class="summary-cards">
          <div class="summary-card">
            <div class="card-label">总销售额</div>
            <div class="card-value">¥{{ formatAmount(productSummary.totalSales) }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">总成本</div>
            <div class="card-value cost">¥{{ formatAmount(productSummary.totalCost) }}</div>
          </div>
          <div class="summary-card highlight">
            <div class="card-label">总毛利润</div>
            <div class="card-value profit">¥{{ formatAmount(productSummary.totalProfit) }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">平均毛利率</div>
            <div class="card-value rate">{{ formatPercent(productSummary.avgProfitRate) }}%</div>
          </div>
        </div>

        <div v-if="productAnalysis.length === 0" class="no-data">暂无产品分析数据</div>
        <table v-else class="analysis-table">
          <thead>
            <tr>
              <th>产品编码</th>
              <th>产品名称</th>
              <th>规格</th>
              <th>销售额</th>
              <th>销售数量</th>
              <th>成本</th>
              <th>毛利润</th>
              <th>毛利率</th>
              <th>订单次数</th>
              <th>平均单价</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in productAnalysis" :key="item.productId">
              <td>
                <span v-if="index < 3" class="rank-badge" :class="'rank-' + (index + 1)">
                  {{ index + 1 }}
                </span>
                {{ item.productCode }}
              </td>
              <td class="product-name">{{ item.productName }}</td>
              <td>{{ item.specification || '-' }}</td>
              <td class="amount">¥{{ formatAmount(item.totalSales) }}</td>
              <td class="number">{{ formatAmount(item.totalQuantity) }}</td>
              <td class="amount cost-text">¥{{ formatAmount(item.totalCost) }}</td>
              <td class="amount profit-text">¥{{ formatAmount(item.grossProfit) }}</td>
              <td :class="['rate-cell', getRateClass(item.grossProfitRate)]">
                {{ formatPercent(item.grossProfitRate) }}%
              </td>
              <td class="number">{{ item.orderCount }}</td>
              <td class="amount">¥{{ formatAmount(item.averagePrice) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 客户利润分析 -->
    <div v-if="activeTab === 'customers'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <div class="summary-cards">
          <div class="summary-card">
            <div class="card-label">客户总数</div>
            <div class="card-value">{{ customerAnalysis.length }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">总销售额</div>
            <div class="card-value">¥{{ formatAmount(customerSummary.totalSales) }}</div>
          </div>
          <div class="summary-card highlight">
            <div class="card-label">总毛利润</div>
            <div class="card-value profit">¥{{ formatAmount(customerSummary.totalProfit) }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">平均订单价值</div>
            <div class="card-value">¥{{ formatAmount(customerSummary.avgOrderValue) }}</div>
          </div>
        </div>

        <div v-if="customerAnalysis.length === 0" class="no-data">暂无客户分析数据</div>
        <table v-else class="analysis-table">
          <thead>
            <tr>
              <th>客户名称</th>
              <th>销售额</th>
              <th>成本</th>
              <th>毛利润</th>
              <th>毛利率</th>
              <th>订单次数</th>
              <th>平均订单价值</th>
              <th>客户价值评分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in customerAnalysis" :key="item.customerId">
              <td class="customer-name">
                <span v-if="index < 3" class="rank-badge" :class="'rank-' + (index + 1)">
                  {{ index + 1 }}
                </span>
                {{ item.customerName }}
              </td>
              <td class="amount">¥{{ formatAmount(item.totalSales) }}</td>
              <td class="amount cost-text">¥{{ formatAmount(item.totalCost) }}</td>
              <td class="amount profit-text">¥{{ formatAmount(item.grossProfit) }}</td>
              <td :class="['rate-cell', getRateClass(item.grossProfitRate)]">
                {{ formatPercent(item.grossProfitRate) }}%
              </td>
              <td class="number">{{ item.orderCount }}</td>
              <td class="amount">¥{{ formatAmount(item.averageOrderValue) }}</td>
              <td class="number value-score">{{ formatAmount(item.valueScore) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 供应商成本分析 -->
    <div v-if="activeTab === 'suppliers'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <div class="summary-cards">
          <div class="summary-card">
            <div class="card-label">供应商总数</div>
            <div class="card-value">{{ supplierAnalysis.length }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">总采购额</div>
            <div class="card-value">¥{{ formatAmount(supplierSummary.totalPurchase) }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">采购次数</div>
            <div class="card-value">{{ supplierSummary.totalCount }}</div>
          </div>
          <div class="summary-card">
            <div class="card-label">平均单价</div>
            <div class="card-value">¥{{ formatAmount(supplierSummary.avgPrice) }}</div>
          </div>
        </div>

        <div v-if="supplierAnalysis.length === 0" class="no-data">暂无供应商分析数据</div>
        <table v-else class="analysis-table">
          <thead>
            <tr>
              <th>供应商名称</th>
              <th>信用等级</th>
              <th>采购总额</th>
              <th>采购数量</th>
              <th>采购次数</th>
              <th>平均单价</th>
              <th>最低价</th>
              <th>最高价</th>
              <th>价格波动率</th>
              <th>平均采购额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in supplierAnalysis" :key="item.supplierId">
              <td class="supplier-name">
                <span v-if="index < 3" class="rank-badge" :class="'rank-' + (index + 1)">
                  {{ index + 1 }}
                </span>
                {{ item.supplierName }}
              </td>
              <td>
                <span :class="['credit-badge', 'credit-' + item.creditRating]">
                  {{ item.creditRating || '-' }}
                </span>
              </td>
              <td class="amount">¥{{ formatAmount(item.totalPurchaseAmount) }}</td>
              <td class="number">{{ formatAmount(item.totalQuantity) }}</td>
              <td class="number">{{ item.purchaseCount }}</td>
              <td class="amount">¥{{ formatAmount(item.averagePrice) }}</td>
              <td class="amount">¥{{ formatAmount(item.minPrice) }}</td>
              <td class="amount">¥{{ formatAmount(item.maxPrice) }}</td>
              <td :class="['rate-cell', getVolatilityClass(item.priceVolatility)]">
                {{ formatPercent(item.priceVolatility) }}%
              </td>
              <td class="amount">¥{{ formatAmount(item.averagePurchaseAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api';

export default {
  name: 'CostAnalysis',
  data() {
    return {
      activeTab: 'products',
      tabs: [
        { key: 'products', label: '产品利润分析' },
        { key: 'customers', label: '客户利润分析' },
        { key: 'suppliers', label: '供应商成本分析' }
      ],
      datePeriods: [
        { key: 'today', label: '今天' },
        { key: 'thisWeek', label: '本周' },
        { key: 'thisMonth', label: '本月' },
        { key: 'thisYear', label: '本年' },
        { key: 'custom', label: '自定义' }
      ],
      selectedPeriod: 'thisMonth',
      customStartDate: '',
      customEndDate: '',
      dateRange: {
        startDate: null,
        endDate: null
      },
      loading: false,
      productAnalysis: [],
      customerAnalysis: [],
      supplierAnalysis: []
    };
  },
  computed: {
    productSummary() {
      if (this.productAnalysis.length === 0) {
        return { totalSales: 0, totalCost: 0, totalProfit: 0, avgProfitRate: 0 };
      }
      const totalSales = this.productAnalysis.reduce((sum, p) => sum + parseFloat(p.totalSales || 0), 0);
      const totalCost = this.productAnalysis.reduce((sum, p) => sum + parseFloat(p.totalCost || 0), 0);
      const totalProfit = totalSales - totalCost;
      const avgProfitRate = totalSales > 0 ? (totalProfit / totalSales) * 100 : 0;
      return { totalSales, totalCost, totalProfit, avgProfitRate };
    },
    customerSummary() {
      if (this.customerAnalysis.length === 0) {
        return { totalSales: 0, totalProfit: 0, avgOrderValue: 0 };
      }
      const totalSales = this.customerAnalysis.reduce((sum, c) => sum + parseFloat(c.totalSales || 0), 0);
      const totalCost = this.customerAnalysis.reduce((sum, c) => sum + parseFloat(c.totalCost || 0), 0);
      const totalProfit = totalSales - totalCost;
      const totalOrders = this.customerAnalysis.reduce((sum, c) => sum + c.orderCount, 0);
      const avgOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0;
      return { totalSales, totalProfit, avgOrderValue };
    },
    supplierSummary() {
      if (this.supplierAnalysis.length === 0) {
        return { totalPurchase: 0, totalCount: 0, avgPrice: 0 };
      }
      const totalPurchase = this.supplierAnalysis.reduce((sum, s) => sum + parseFloat(s.totalPurchaseAmount || 0), 0);
      const totalCount = this.supplierAnalysis.reduce((sum, s) => sum + s.purchaseCount, 0);
      const avgPrice = this.supplierAnalysis.reduce((sum, s) => sum + parseFloat(s.averagePrice || 0), 0) / 
                      this.supplierAnalysis.length;
      return { totalPurchase, totalCount, avgPrice };
    }
  },
  mounted() {
    this.selectDatePeriod('thisMonth');
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
          this.loadProductAnalysis(),
          this.loadCustomerAnalysis(),
          this.loadSupplierAnalysis()
        ]);
      } catch (error) {
        console.error('加载数据失败:', error);
        alert('加载数据失败: ' + (error.response?.data?.message || error.message));
      } finally {
        this.loading = false;
      }
    },
    async loadProductAnalysis() {
      const response = await api.get('/cost-analysis/products', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.productAnalysis = Array.isArray(response) ? response : [];
    },
    async loadCustomerAnalysis() {
      const response = await api.get('/cost-analysis/customers', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.customerAnalysis = Array.isArray(response) ? response : [];
    },
    async loadSupplierAnalysis() {
      const response = await api.get('/cost-analysis/suppliers', {
        params: {
          startDate: this.dateRange.startDate,
          endDate: this.dateRange.endDate
        }
      });
      // 响应拦截器已经返回了response.data，所以这里直接使用response
      this.supplierAnalysis = Array.isArray(response) ? response : [];
    },
    formatAmount(amount) {
      return amount ? parseFloat(amount).toFixed(2) : '0.00';
    },
    formatPercent(percent) {
      return percent ? parseFloat(percent).toFixed(2) : '0.00';
    },
    getRateClass(rate) {
      const r = parseFloat(rate);
      if (r >= 40) return 'excellent';
      if (r >= 30) return 'good';
      if (r >= 20) return 'average';
      return 'low';
    },
    getVolatilityClass(volatility) {
      const v = parseFloat(volatility);
      if (v <= 10) return 'excellent';
      if (v <= 20) return 'good';
      if (v <= 30) return 'average';
      return 'low';
    }
  }
};
</script>

<style scoped>
.cost-analysis {
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

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.summary-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 8px;
  color: white;
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
}

.card-value.cost {
  color: #ffe0e0;
}

.card-value.profit {
  color: #fff;
}

.card-value.rate {
  color: #e0f7ff;
}

.analysis-table {
  width: 100%;
  border-collapse: collapse;
}

.analysis-table th,
.analysis-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.analysis-table th {
  background: #f5f7fa;
  font-weight: bold;
  color: #333;
}

.amount {
  text-align: right;
  font-family: 'Courier New', monospace;
}

.number {
  text-align: right;
}

.cost-text {
  color: #e6a23c;
}

.profit-text {
  color: #67c23a;
  font-weight: 600;
}

.rate-cell {
  text-align: right;
  font-weight: 600;
}

.rate-cell.excellent {
  color: #67c23a;
}

.rate-cell.good {
  color: #409eff;
}

.rate-cell.average {
  color: #e6a23c;
}

.rate-cell.low {
  color: #f56c6c;
}

.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  color: white;
  font-weight: bold;
  font-size: 12px;
  margin-right: 8px;
}

.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #333;
}

.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
  color: #333;
}

.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #e6a671);
  color: white;
}

.credit-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.credit-A {
  background: #e1f3d8;
  color: #67c23a;
}

.credit-B {
  background: #f0f9ff;
  color: #409eff;
}

.credit-C {
  background: #fdf6ec;
  color: #e6a23c;
}

.credit-D {
  background: #fef0f0;
  color: #f56c6c;
}

.product-name,
.customer-name,
.supplier-name {
  font-weight: 500;
  color: #333;
}

.value-score {
  color: #409eff;
  font-weight: 600;
}
</style>