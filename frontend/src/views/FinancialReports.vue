<template>
  <div class="financial-reports">
    <h1>财务报表</h1>
    
    <!-- 日期选择器 -->
    <div class="date-selector">
      <label>报表日期: </label>
      <input type="date" v-model="startDate" />
      <span> 至 </span>
      <input type="date" v-model="endDate" />
      <button @click="loadReports" class="btn-primary">查询</button>
      <button @click="setDateRange('today')" class="btn-secondary">今天</button>
      <button @click="setDateRange('thisWeek')" class="btn-secondary">本周</button>
      <button @click="setDateRange('thisMonth')" class="btn-secondary">本月</button>
      <button @click="setDateRange('thisYear')" class="btn-secondary">本年</button>
    </div>

    <!-- 报表标签页 -->
    <div class="tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['tab', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id">
        {{ tab.name }}
      </button>
    </div>

    <!-- 资产负债表 -->
    <div v-if="activeTab === 'balance'" class="report-section">
      <h2>资产负债表 (Balance Sheet)</h2>
      <p class="report-date">截至日期: {{ endDate }}</p>
      
      <div v-if="balanceSheet" class="financial-statement">
        <!-- 资产 -->
        <div class="statement-section">
          <h3 class="section-title">资产 (Assets)</h3>
          <table class="financial-table">
            <thead>
              <tr>
                <th>科目</th>
                <th class="amount">金额 (¥)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="indent-1">流动资产</td>
                <td class="amount"></td>
              </tr>
              <tr>
                <td class="indent-2">现金</td>
                <td class="amount">{{ formatAmount(balanceSheet.assets.currentAssets.cash) }}</td>
              </tr>
              <tr>
                <td class="indent-2">应收账款</td>
                <td class="amount">{{ formatAmount(balanceSheet.assets.currentAssets.accountsReceivable) }}</td>
              </tr>
              <tr>
                <td class="indent-2">库存商品</td>
                <td class="amount">{{ formatAmount(balanceSheet.assets.currentAssets.inventory) }}</td>
              </tr>
              <tr class="subtotal">
                <td class="indent-1">流动资产合计</td>
                <td class="amount">{{ formatAmount(balanceSheet.assets.currentAssets.total) }}</td>
              </tr>
              <tr class="total">
                <td><strong>资产总计</strong></td>
                <td class="amount"><strong>{{ formatAmount(balanceSheet.assets.totalAssets) }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 负债 -->
        <div class="statement-section">
          <h3 class="section-title">负债 (Liabilities)</h3>
          <table class="financial-table">
            <thead>
              <tr>
                <th>科目</th>
                <th class="amount">金额 (¥)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="indent-1">流动负债</td>
                <td class="amount"></td>
              </tr>
              <tr>
                <td class="indent-2">应付账款</td>
                <td class="amount">{{ formatAmount(balanceSheet.liabilities.currentLiabilities.accountsPayable) }}</td>
              </tr>
              <tr>
                <td class="indent-2">应交税费</td>
                <td class="amount">{{ formatAmount(balanceSheet.liabilities.currentLiabilities.taxPayable) }}</td>
              </tr>
              <tr class="subtotal">
                <td class="indent-1">流动负债合计</td>
                <td class="amount">{{ formatAmount(balanceSheet.liabilities.currentLiabilities.total) }}</td>
              </tr>
              <tr class="total">
                <td><strong>负债合计</strong></td>
                <td class="amount"><strong>{{ formatAmount(balanceSheet.liabilities.totalLiabilities) }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 所有者权益 -->
        <div class="statement-section">
          <h3 class="section-title">所有者权益 (Equity)</h3>
          <table class="financial-table">
            <thead>
              <tr>
                <th>科目</th>
                <th class="amount">金额 (¥)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="indent-1">留存收益</td>
                <td class="amount">{{ formatAmount(balanceSheet.equity.retainedEarnings) }}</td>
              </tr>
              <tr class="total">
                <td><strong>所有者权益合计</strong></td>
                <td class="amount"><strong>{{ formatAmount(balanceSheet.equity.totalEquity) }}</strong></td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 平衡验证 -->
        <div class="balance-check" :class="{ valid: balanceSheet.balanceCheck }">
          <span v-if="balanceSheet.balanceCheck">✓ 资产负债表平衡</span>
          <span v-else>⚠ 资产负债表不平衡</span>
        </div>
      </div>
    </div>

    <!-- 利润表 -->
    <div v-if="activeTab === 'income'" class="report-section">
      <h2>利润表 (Income Statement)</h2>
      <p class="report-date">期间: {{ startDate }} 至 {{ endDate }}</p>
      
      <div v-if="incomeStatement" class="financial-statement">
        <table class="financial-table">
          <thead>
            <tr>
              <th>项目</th>
              <th class="amount">金额 (¥)</th>
              <th class="amount">比率 (%)</th>
            </tr>
          </thead>
          <tbody>
            <tr class="section-header">
              <td colspan="3"><strong>一、营业收入</strong></td>
            </tr>
            <tr>
              <td class="indent-1">营业收入</td>
              <td class="amount">{{ formatAmount(incomeStatement.operatingRevenue) }}</td>
              <td class="amount">100.00</td>
            </tr>
            
            <tr class="section-header">
              <td colspan="3"><strong>二、营业成本</strong></td>
            </tr>
            <tr>
              <td class="indent-1">营业成本</td>
              <td class="amount negative">{{ formatAmount(incomeStatement.operatingCost) }}</td>
              <td class="amount">{{ formatPercent(incomeStatement.operatingCost, incomeStatement.operatingRevenue) }}</td>
            </tr>
            
            <tr class="subtotal">
              <td><strong>三、毛利润</strong></td>
              <td class="amount"><strong>{{ formatAmount(incomeStatement.grossProfit) }}</strong></td>
              <td class="amount"><strong>{{ formatNumber(incomeStatement.grossMargin) }}</strong></td>
            </tr>
            
            <tr class="section-header">
              <td colspan="3"><strong>四、期间费用</strong></td>
            </tr>
            <tr>
              <td class="indent-1">销售费用</td>
              <td class="amount">{{ formatAmount(incomeStatement.operatingExpenses.salesExpenses) }}</td>
              <td class="amount">-</td>
            </tr>
            <tr>
              <td class="indent-1">管理费用</td>
              <td class="amount">{{ formatAmount(incomeStatement.operatingExpenses.adminExpenses) }}</td>
              <td class="amount">-</td>
            </tr>
            <tr>
              <td class="indent-1">财务费用</td>
              <td class="amount">{{ formatAmount(incomeStatement.operatingExpenses.financialExpenses) }}</td>
              <td class="amount">-</td>
            </tr>
            
            <tr class="subtotal">
              <td><strong>五、营业利润</strong></td>
              <td class="amount"><strong>{{ formatAmount(incomeStatement.operatingProfit) }}</strong></td>
              <td class="amount"><strong>{{ formatPercent(incomeStatement.operatingProfit, incomeStatement.operatingRevenue) }}</strong></td>
            </tr>
            
            <tr class="total">
              <td><strong>六、净利润</strong></td>
              <td class="amount profit"><strong>{{ formatAmount(incomeStatement.netProfit) }}</strong></td>
              <td class="amount profit"><strong>{{ formatNumber(incomeStatement.netMargin) }}</strong></td>
            </tr>
          </tbody>
        </table>

        <div class="summary-info">
          <p>期间订单数量: {{ incomeStatement.orderCount }} 笔</p>
        </div>
      </div>
    </div>

    <!-- 现金流量表 -->
    <div v-if="activeTab === 'cashflow'" class="report-section">
      <h2>现金流量表 (Cash Flow Statement)</h2>
      <p class="report-date">期间: {{ startDate }} 至 {{ endDate }}</p>
      
      <div v-if="cashFlow" class="financial-statement">
        <table class="financial-table">
          <thead>
            <tr>
              <th>项目</th>
              <th class="amount">金额 (¥)</th>
            </tr>
          </thead>
          <tbody>
            <!-- 经营活动现金流 -->
            <tr class="section-header">
              <td colspan="2"><strong>一、经营活动产生的现金流量</strong></td>
            </tr>
            <tr>
              <td class="indent-1">销售商品收到的现金</td>
              <td class="amount positive">{{ formatAmount(cashFlow.operatingActivities.cashFromSales) }}</td>
            </tr>
            <tr>
              <td class="indent-1">购买商品支付的现金</td>
              <td class="amount negative">{{ formatAmount(cashFlow.operatingActivities.cashForPurchases) }}</td>
            </tr>
            <tr>
              <td class="indent-1">支付的各项税费</td>
              <td class="amount negative">{{ formatAmount(cashFlow.operatingActivities.taxPayments) }}</td>
            </tr>
            <tr class="subtotal">
              <td><strong>经营活动现金流量净额</strong></td>
              <td class="amount" :class="getAmountClass(cashFlow.operatingActivities.netCashFlow)">
                <strong>{{ formatAmount(cashFlow.operatingActivities.netCashFlow) }}</strong>
              </td>
            </tr>

            <!-- 投资活动现金流 -->
            <tr class="section-header">
              <td colspan="2"><strong>二、投资活动产生的现金流量</strong></td>
            </tr>
            <tr>
              <td class="indent-1">购建固定资产支付的现金</td>
              <td class="amount">{{ formatAmount(cashFlow.investingActivities.purchaseOfAssets) }}</td>
            </tr>
            <tr>
              <td class="indent-1">处置固定资产收到的现金</td>
              <td class="amount">{{ formatAmount(cashFlow.investingActivities.saleOfAssets) }}</td>
            </tr>
            <tr class="subtotal">
              <td><strong>投资活动现金流量净额</strong></td>
              <td class="amount"><strong>{{ formatAmount(cashFlow.investingActivities.netCashFlow) }}</strong></td>
            </tr>

            <!-- 筹资活动现金流 -->
            <tr class="section-header">
              <td colspan="2"><strong>三、筹资活动产生的现金流量</strong></td>
            </tr>
            <tr>
              <td class="indent-1">取得借款收到的现金</td>
              <td class="amount">{{ formatAmount(cashFlow.financingActivities.borrowings) }}</td>
            </tr>
            <tr>
              <td class="indent-1">偿还债务支付的现金</td>
              <td class="amount">{{ formatAmount(cashFlow.financingActivities.repayments) }}</td>
            </tr>
            <tr class="subtotal">
              <td><strong>筹资活动现金流量净额</strong></td>
              <td class="amount"><strong>{{ formatAmount(cashFlow.financingActivities.netCashFlow) }}</strong></td>
            </tr>

            <!-- 现金汇总 -->
            <tr class="total">
              <td><strong>四、现金净增加额</strong></td>
              <td class="amount" :class="getAmountClass(cashFlow.netCashChange)">
                <strong>{{ formatAmount(cashFlow.netCashChange) }}</strong>
              </td>
            </tr>
            <tr>
              <td class="indent-1">期初现金余额</td>
              <td class="amount">{{ formatAmount(cashFlow.beginningCash) }}</td>
            </tr>
            <tr class="total highlight">
              <td><strong>五、期末现金余额</strong></td>
              <td class="amount"><strong>{{ formatAmount(cashFlow.endingCash) }}</strong></td>
            </tr>
          </tbody>
        </table>

        <!-- 现金流健康度指标 -->
        <div class="health-indicators">
          <h4>现金流健康度指标</h4>
          <div class="indicator">
            <span>经营活动现金比率:</span>
            <span class="value">{{ formatNumber(cashFlow.healthIndicators.operatingCashRatio) }}%</span>
          </div>
          <div class="indicator">
            <span>现金流充足性:</span>
            <span class="value" :class="{ positive: cashFlow.healthIndicators.cashFlowAdequacy, negative: !cashFlow.healthIndicators.cashFlowAdequacy }">
              {{ cashFlow.healthIndicators.cashFlowAdequacy ? '充足' : '不足' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>正在加载财务报表...</p>
    </div>

    <!-- 错误信息 -->
    <div v-if="error" class="error-message">
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'FinancialReports',
  data() {
    return {
      activeTab: 'balance',
      tabs: [
        { id: 'balance', name: '资产负债表' },
        { id: 'income', name: '利润表' },
        { id: 'cashflow', name: '现金流量表' }
      ],
      startDate: '',
      endDate: '',
      balanceSheet: null,
      incomeStatement: null,
      cashFlow: null,
      loading: false,
      error: null
    };
  },
  mounted() {
    this.setDateRange('thisMonth');
    this.loadReports();
  },
  methods: {
    setDateRange(range) {
      const today = new Date();
      let start, end;

      switch (range) {
        case 'today':
          start = end = today;
          break;
        case 'thisWeek':
          start = new Date(today);
          start.setDate(today.getDate() - today.getDay());
          end = today;
          break;
        case 'thisMonth':
          start = new Date(today.getFullYear(), today.getMonth(), 1);
          end = today;
          break;
        case 'thisYear':
          start = new Date(today.getFullYear(), 0, 1);
          end = today;
          break;
        default:
          start = new Date(today.getFullYear(), today.getMonth(), 1);
          end = today;
      }

      this.startDate = this.formatDate(start);
      this.endDate = this.formatDate(end);
    },

    formatDate(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },

    async loadReports() {
      this.loading = true;
      this.error = null;

      try {
        // 并行加载三个报表
        const [balanceResponse, incomeResponse, cashFlowResponse] = await Promise.all([
          axios.get(`http://localhost:8080/api/statistics/balance-sheet?asOfDate=${this.endDate}`),
          axios.get(`http://localhost:8080/api/statistics/income-statement?startDate=${this.startDate}&endDate=${this.endDate}`),
          axios.get(`http://localhost:8080/api/statistics/cash-flow?startDate=${this.startDate}&endDate=${this.endDate}`)
        ]);

        this.balanceSheet = balanceResponse.data;
        this.incomeStatement = incomeResponse.data;
        this.cashFlow = cashFlowResponse.data;
      } catch (err) {
        console.error('加载财务报表失败:', err);
        this.error = '加载财务报表失败，请稍后重试';
      } finally {
        this.loading = false;
      }
    },

    formatAmount(value) {
      if (value === null || value === undefined) return '0.00';
      return Number(value).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
    },

    formatNumber(value) {
      if (value === null || value === undefined) return '0.00';
      return Number(value).toFixed(2);
    },

    formatPercent(value, total) {
      if (!total || total === 0) return '0.00';
      return ((value / total) * 100).toFixed(2);
    },

    getAmountClass(value) {
      if (value > 0) return 'positive';
      if (value < 0) return 'negative';
      return '';
    }
  }
};
</script>

<style scoped>
.financial-reports {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

h1 {
  color: #2c3e50;
  margin-bottom: 20px;
}

h2 {
  color: #34495e;
  margin-bottom: 10px;
  border-bottom: 2px solid #3498db;
  padding-bottom: 10px;
}

.date-selector {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-selector label {
  font-weight: 600;
}

.date-selector input[type="date"] {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background: #3498db;
  color: white;
}

.btn-primary:hover {
  background: #2980b9;
}

.btn-secondary {
  background: #95a5a6;
  color: white;
}

.btn-secondary:hover {
  background: #7f8c8d;
}

.tabs {
  display: flex;
  gap: 5px;
  margin-bottom: 20px;
  border-bottom: 2px solid #ddd;
}

.tab {
  padding: 12px 24px;
  background: #ecf0f1;
  border: none;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

.tab:hover {
  background: #d5dbdb;
}

.tab.active {
  background: #3498db;
  color: white;
}

.report-section {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.report-date {
  color: #7f8c8d;
  font-size: 14px;
  margin-bottom: 20px;
  font-style: italic;
}

.financial-statement {
  margin-top: 20px;
}

.statement-section {
  margin-bottom: 30px;
}

.section-title {
  color: #2980b9;
  font-size: 18px;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #3498db;
}

.financial-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.financial-table th {
  background: #34495e;
  color: white;
  padding: 12px;
  text-align: left;
  font-weight: 600;
}

.financial-table th.amount {
  text-align: right;
  width: 150px;
}

.financial-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #ecf0f1;
}

.financial-table td.amount {
  text-align: right;
  font-family: 'Courier New', monospace;
}

.financial-table tr:hover {
  background: #f8f9fa;
}

.indent-1 {
  padding-left: 30px;
}

.indent-2 {
  padding-left: 60px;
}

.section-header td {
  background: #ecf0f1;
  font-weight: 600;
  padding: 12px;
}

.subtotal {
  background: #e8f4f8 !important;
  font-weight: 600;
}

.total {
  background: #d5e8f0 !important;
  font-weight: 700;
  font-size: 16px;
}

.total.highlight {
  background: #3498db !important;
  color: white;
}

.positive {
  color: #27ae60;
}

.negative {
  color: #e74c3c;
}

.profit {
  color: #27ae60;
  font-size: 18px;
}

.balance-check {
  margin-top: 20px;
  padding: 15px;
  border-radius: 8px;
  text-align: center;
  font-weight: 600;
  font-size: 16px;
}

.balance-check.valid {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.balance-check:not(.valid) {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.summary-info {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.summary-info p {
  margin: 5px 0;
  color: #34495e;
}

.health-indicators {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #27ae60;
}

.health-indicators h4 {
  color: #2c3e50;
  margin-bottom: 15px;
}

.indicator {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #ddd;
}

.indicator:last-child {
  border-bottom: none;
}

.indicator .value {
  font-weight: 600;
  font-size: 16px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #7f8c8d;
  font-size: 16px;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #f5c6cb;
  margin-top: 20px;
}
</style>