<template>
  <div class="data-analytics">
    <h1>数据分析</h1>
    
    <!-- 日期和分组选择 -->
    <div class="controls">
      <div class="date-selector">
        <select v-model="selectedPeriod" @change="onPeriodChange">
          <option value="week">最近一周</option>
          <option value="month">最近一月</option>
          <option value="quarter">最近三月</option>
          <option value="year">本年度</option>
        </select>
        <select v-model="groupBy" @change="loadTrendData">
          <option value="day">按日</option>
          <option value="week">按周</option>
          <option value="month">按月</option>
        </select>
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

    <!-- 销售趋势 -->
    <div v-if="activeTab === 'sales'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <h2>销售趋势分析</h2>
        <div class="chart-container">
          <div class="chart" v-if="salesTrend && salesTrend.length > 0">
            <div 
              v-for="(item, index) in salesTrend" 
              :key="index"
              class="chart-bar-group"
            >
              <div 
                class="chart-bar sales"
                :style="{ height: getBarHeight(item.amount, maxSalesAmount) + 'px' }"
                :title="`${item.period}: ¥${formatAmount(item.amount)}`"
              ></div>
              <div class="chart-label">{{ formatPeriodLabel(item.period) }}</div>
              <div class="chart-value">¥{{ formatAmount(item.amount) }}</div>
            </div>
          </div>
          <div v-else class="no-data">暂无数据</div>
        </div>
        <div class="stats-summary">
          <div class="stat-card">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥{{ formatAmount(salesSummary.total) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">订单数量</div>
            <div class="stat-value">{{ salesSummary.count }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">平均订单额</div>
            <div class="stat-value">¥{{ formatAmount(salesSummary.average) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 采购趋势 -->
    <div v-if="activeTab === 'purchase'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <h2>采购趋势分析</h2>
        <div class="chart-container">
          <div class="chart" v-if="purchaseTrend && purchaseTrend.length > 0">
            <div 
              v-for="(item, index) in purchaseTrend" 
              :key="index"
              class="chart-bar-group"
            >
              <div 
                class="chart-bar purchase"
                :style="{ height: getBarHeight(item.amount, maxPurchaseAmount) + 'px' }"
                :title="`${item.period}: ¥${formatAmount(item.amount)}`"
              ></div>
              <div class="chart-label">{{ formatPeriodLabel(item.period) }}</div>
              <div class="chart-value">¥{{ formatAmount(item.amount) }}</div>
            </div>
          </div>
          <div v-else class="no-data">暂无数据</div>
        </div>
        <div class="stats-summary">
          <div class="stat-card">
            <div class="stat-label">总采购额</div>
            <div class="stat-value">¥{{ formatAmount(purchaseSummary.total) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">采购次数</div>
            <div class="stat-value">{{ purchaseSummary.count }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">平均采购额</div>
            <div class="stat-value">¥{{ formatAmount(purchaseSummary.average) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 库存周转率 -->
    <div v-if="activeTab === 'turnover'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <h2>库存周转率分析</h2>
        <div v-if="!turnoverAnalysis || turnoverAnalysis.length === 0" class="no-data">暂无数据</div>
        <table v-else class="analysis-table">
          <thead>
            <tr>
              <th>产品编码</th>
              <th>产品名称</th>
              <th>分类</th>
              <th>当前库存</th>
              <th>销售数量</th>
              <th>周转率</th>
              <th>周转天数</th>
              <th>评级</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in turnoverAnalysis" :key="item.productId">
              <td>{{ item.inventoryCode }}</td>
              <td class="product-name">{{ item.productName }}</td>
              <td>{{ item.category || '-' }}</td>
              <td class="number">{{ item.currentStock }}</td>
              <td class="number">{{ formatAmount(item.salesQuantity) }}</td>
              <td class="number highlight">{{ formatAmount(item.turnoverRate) }}</td>
              <td class="number">{{ formatAmount(item.turnoverDays) }}</td>
              <td>
                <span :class="['rating-badge', getRatingClass(item.turnoverRating)]">
                  {{ item.turnoverRating }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 经营看板 -->
    <div v-if="activeTab === 'dashboard'" class="tab-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <h2>经营分析看板</h2>
        <div class="dashboard-grid">
          <div class="dashboard-section">
            <h3>销售指标</h3>
            <div class="metrics">
              <div class="metric-item">
                <span class="metric-label">总销售额</span>
                <span class="metric-value sales">¥{{ formatAmount(businessDashboard?.salesMetrics?.totalSales) }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">订单数量</span>
                <span class="metric-value">{{ businessDashboard?.salesMetrics?.orderCount || 0 }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">平均订单值</span>
                <span class="metric-value">¥{{ formatAmount(businessDashboard?.salesMetrics?.averageOrderValue) }}</span>
              </div>
            </div>
          </div>

          <div class="dashboard-section">
            <h3>采购指标</h3>
            <div class="metrics">
              <div class="metric-item">
                <span class="metric-label">总采购额</span>
                <span class="metric-value purchase">¥{{ formatAmount(businessDashboard?.purchaseMetrics?.totalPurchase) }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">采购次数</span>
                <span class="metric-value">{{ businessDashboard?.purchaseMetrics?.purchaseCount || 0 }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">平均采购值</span>
                <span class="metric-value">¥{{ formatAmount(businessDashboard?.purchaseMetrics?.averagePurchaseValue) }}</span>
              </div>
            </div>
          </div>

          <div class="dashboard-section">
            <h3>利润指标</h3>
            <div class="metrics">
              <div class="metric-item">
                <span class="metric-label">毛利润</span>
                <span class="metric-value profit">¥{{ formatAmount(businessDashboard?.profitMetrics?.grossProfit) }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">毛利率</span>
                <span class="metric-value profit">{{ formatAmount(businessDashboard?.profitMetrics?.profitMargin) }}%</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">估算成本</span>
                <span class="metric-value">¥{{ formatAmount(businessDashboard?.profitMetrics?.estimatedCost) }}</span>
              </div>
            </div>
          </div>

          <div class="dashboard-section">
            <h3>库存指标</h3>
            <div class="metrics">
              <div class="metric-item">
                <span class="metric-label">产品总数</span>
                <span class="metric-value">{{ businessDashboard?.inventoryMetrics?.totalProducts || 0 }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">低库存产品</span>
                <span class="metric-value warning">{{ businessDashboard?.inventoryMetrics?.lowStockCount || 0 }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-label">缺货产品</span>
                <span class="metric-value danger">{{ businessDashboard?.inventoryMetrics?.outOfStockCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api';

export default {
  name: 'DataAnalytics',
  data() {
    return {
      activeTab: 'sales',
      tabs: [
        { key: 'sales', label: '销售趋势' },
        { key: 'purchase', label: '采购趋势' },
        { key: 'turnover', label: '库存周转' },
        { key: 'dashboard', label: '经营看板' }
      ],
      selectedPeriod: 'month',
      groupBy: 'day',
      dateRange: { startDate: null, endDate: null },
      loading: false,
      salesTrend: [],
      purchaseTrend: [],
      turnoverAnalysis: [],
      businessDashboard: {}
    };
  },
  computed: {
    maxSalesAmount() {
      if (!this.salesTrend || this.salesTrend.length === 0) return 0;
      return Math.max(...this.salesTrend.map(item => parseFloat(item.amount || 0)));
    },
    maxPurchaseAmount() {
      if (!this.purchaseTrend || this.purchaseTrend.length === 0) return 0;
      return Math.max(...this.purchaseTrend.map(item => parseFloat(item.amount || 0)));
    },
    salesSummary() {
      if (!this.salesTrend || this.salesTrend.length === 0) {
        return { total: 0, count: 0, average: 0 };
      }
      const total = this.salesTrend.reduce((sum, item) => sum + parseFloat(item.amount || 0), 0);
      const count = this.salesTrend.reduce((sum, item) => sum + (item.count || 0), 0);
      const average = count > 0 ? total / count : 0;
      return { total, count, average };
    },
    purchaseSummary() {
      if (!this.purchaseTrend || this.purchaseTrend.length === 0) {
        return { total: 0, count: 0, average: 0 };
      }
      const total = this.purchaseTrend.reduce((sum, item) => sum + parseFloat(item.amount || 0), 0);
      const count = this.purchaseTrend.reduce((sum, item) => sum + (item.count || 0), 0);
      const average = count > 0 ? total / count : 0;
      return { total, count, average };
    }
  },
  mounted() {
    this.onPeriodChange();
  },
  methods: {
    onPeriodChange() {
      const now = new Date();
      let startDate, endDate = new Date();

      switch (this.selectedPeriod) {
        case 'week':
          startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
          this.groupBy = 'day';
          break;
        case 'month':
          startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
          this.groupBy = 'day';
          break;
        case 'quarter':
          startDate = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
          this.groupBy = 'week';
          break;
        case 'year':
          startDate = new Date(now.getFullYear(), 0, 1);
          this.groupBy = 'month';
          break;
      }

      this.dateRange.startDate = startDate.toISOString();
      this.dateRange.endDate = endDate.toISOString();
      this.loadAllData();
    },
    async loadAllData() {
      this.loading = true;
      try {
        await Promise.all([
          this.loadTrendData(),
          this.loadTurnoverAnalysis(),
          this.loadBusinessDashboard()
        ]);
      } catch (error) {
        console.error('加载数据失败:', error);
        alert('加载数据失败: ' + (error.response?.data?.message || error.message));
      } finally {
        this.loading = false;
      }
    },
    async loadTrendData() {
      const params = {
        startDate: this.dateRange.startDate,
        endDate: this.dateRange.endDate,
        groupBy: this.groupBy
      };

      const [salesResponse, purchaseResponse] = await Promise.all([
        api.get('/analytics/sales-trend', { params }),
        api.get('/analytics/purchase-trend', { params })
      ]);

      this.salesTrend = salesResponse.data;
      this.purchaseTrend = purchaseResponse.data;
    },
    async loadTurnoverAnalysis() {
      const params = {
        startDate: this.dateRange.startDate,
        endDate: this.dateRange.endDate
      };
      const response = await api.get('/analytics/inventory-turnover', { params });
      this.turnoverAnalysis = response.data;
    },
    async loadBusinessDashboard() {
      const params = {
        startDate: this.dateRange.startDate,
        endDate: this.dateRange.endDate
      };
      const response = await api.get('/analytics/business-dashboard', { params });
      this.businessDashboard = response.data;
    },
    getBarHeight(amount, maxAmount) {
      const minHeight = 20;
      const maxHeight = 200;
      if (maxAmount === 0) return minHeight;
      const height = (parseFloat(amount) / maxAmount) * maxHeight;
      return Math.max(minHeight, height);
    },
    formatAmount(amount) {
      return amount ? parseFloat(amount).toFixed(2) : '0.00';
    },
    formatPeriodLabel(period) {
      if (this.groupBy === 'day') {
        return period.substring(5); // MM-DD
      } else if (this.groupBy === 'week') {
        return period.substring(2); // W##
      }
      return period.substring(5); // MM
    },
    getRatingClass(rating) {
      const classes = {
        '优秀': 'excellent',
        '良好': 'good',
        '一般': 'average',
        '滞销': 'poor'
      };
      return classes[rating] || '';
    }
  }
};
</script>

<style scoped>
.data-analytics {
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
  color: #333;
}

h2 {
  margin-bottom: 20px;
  color: #333;
  font-size: 18px;
}

.controls {
  background: white;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.date-selector {
  display: flex;
  gap: 10px;
}

.date-selector select {
  padding: 8px 12px;
  border: 1px solid #ddd;
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
  min-height: 500px;
}

.loading, .no-data {
  text-align: center;
  padding: 50px;
  color: #999;
}

.chart-container {
  background: #f9f9f9;
  padding: 30px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 10px;
  height: 280px;
  padding-bottom: 40px;
}

.chart-bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  max-width: 60px;
}

.chart-bar {
  width: 100%;
  border-radius: 4px 4px 0 0;
  transition: all 0.3s;
  cursor: pointer;
  min-height: 20px;
}

.chart-bar.sales {
  background: linear-gradient(to top, #667eea, #764ba2);
}

.chart-bar.purchase {
  background: linear-gradient(to top, #f093fb, #f5576c);
}

.chart-bar:hover {
  opacity: 0.8;
  transform: translateY(-5px);
}

.chart-label {
  margin-top: 5px;
  font-size: 11px;
  color: #666;
  text-align: center;
}

.chart-value {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  border-radius: 8px;
  color: white;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
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

.product-name {
  font-weight: 500;
}

.number {
  text-align: right;
  font-family: 'Courier New', monospace;
}

.number.highlight {
  font-weight: 600;
  color: #409eff;
}

.rating-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.rating-badge.excellent {
  background: #e1f3d8;
  color: #67c23a;
}

.rating-badge.good {
  background: #f0f9ff;
  color: #409eff;
}

.rating-badge.average {
  background: #fdf6ec;
  color: #e6a23c;
}

.rating-badge.poor {
  background: #fef0f0;
  color: #f56c6c;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.dashboard-section {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.dashboard-section h3 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.metrics {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  border-radius: 4px;
}

.metric-label {
  font-size: 14px;
  color: #666;
}

.metric-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.metric-value.sales {
  color: #667eea;
}

.metric-value.purchase {
  color: #f5576c;
}

.metric-value.profit {
  color: #67c23a;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-value.danger {
  color: #f56c6c;
}
</style>