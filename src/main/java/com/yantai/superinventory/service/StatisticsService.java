package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Invoice;
import com.yantai.superinventory.model.Order;
import com.yantai.superinventory.model.OrderStatus;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.model.Purchase;
import com.yantai.superinventory.repository.CustomerRepository;
import com.yantai.superinventory.repository.InvoiceRepository;
import com.yantai.superinventory.repository.OrderRepository;
import com.yantai.superinventory.repository.PaymentRepository;
import com.yantai.superinventory.repository.ProductRepository;
import com.yantai.superinventory.repository.PurchaseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    private final OrderRepository orderRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    
    public StatisticsService(
            OrderRepository orderRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }
    
    public Map<String, Object> getDashboardStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // 转换日期为LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        Date startDateObj = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDateObj = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
        
        // 获取数据
        List<Order> allOrders = orderRepository.findAll();
        List<Purchase> allPurchases = purchaseRepository.findAll();
        List<Invoice> allInvoices = invoiceRepository.findAll();
        
        // 筛选日期范围内的数据 - Order使用LocalDateTime
        List<Order> periodOrders = allOrders.stream()
                .filter(o -> o.getOrderDate() != null && 
                            !o.getOrderDate().isBefore(startDateTime) && 
                            o.getOrderDate().isBefore(endDateTime))
                .collect(Collectors.toList());
        
        // Purchase使用LocalDateTime
        List<Purchase> periodPurchases = allPurchases.stream()
                .filter(p -> p.getPurchaseDate() != null &&
                            !p.getPurchaseDate().isBefore(startDateTime) && 
                            p.getPurchaseDate().isBefore(endDateTime))
                .collect(Collectors.toList());
        
        // Invoice使用LocalDateTime
        List<Invoice> periodInvoices = allInvoices.stream()
                .filter(i -> i.getInvoiceDate() != null && 
                            !i.getInvoiceDate().isBefore(startDateTime) && 
                            i.getInvoiceDate().isBefore(endDateTime))
                .collect(Collectors.toList());
        
        // 基础统计
        stats.put("totalProducts", productRepository.count());
        stats.put("totalCustomers", customerRepository.count());
        stats.put("totalOrders", allOrders.size());
        stats.put("totalPurchases", allPurchases.size());
        
        // 期间订单统计
        stats.put("periodOrders", periodOrders.size());
        stats.put("periodPurchases", periodPurchases.size());
        stats.put("periodInvoices", periodInvoices.size());
        
        // 财务统计 - 销售收入
        BigDecimal salesRevenue = periodOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || 
                           o.getStatus() == OrderStatus.INVOICED || 
                           o.getStatus() == OrderStatus.SHIPPED || 
                           o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("salesRevenue", salesRevenue);
        
        // 财务统计 - 采购成本
        BigDecimal purchaseCost = periodPurchases.stream()
                .map(p -> p.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("purchaseCost", purchaseCost);
        
        // 财务统计 - 销售成本
        BigDecimal salesCost = periodOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || 
                           o.getStatus() == OrderStatus.INVOICED || 
                           o.getStatus() == OrderStatus.SHIPPED || 
                           o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getCostSubtotal() != null ? item.getCostSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 财务统计 - 毛利润
        BigDecimal grossProfit = salesRevenue.subtract(salesCost);
        stats.put("grossProfit", grossProfit);
        
        // 财务统计 - 毛利率
        if (salesRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossMargin = grossProfit.divide(salesRevenue, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
            stats.put("grossMargin", grossMargin);
        } else {
            stats.put("grossMargin", BigDecimal.ZERO);
        }
        
        // 发票统计
        BigDecimal invoicedAmount = periodInvoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .map(i -> i.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("invoicedAmount", invoicedAmount);
        
        BigDecimal taxAmount = periodInvoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .map(i -> i.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("taxAmount", taxAmount);
        
        // 订单状态统计
        Map<String, Long> ordersByStatus = periodOrders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getStatus().name(), 
                    Collectors.counting()
                ));
        stats.put("ordersByStatus", ordersByStatus);
        
        // 待收款金额 (UNPAID + CREATED orders)
        BigDecimal pendingPayment = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.UNPAID || o.getStatus() == OrderStatus.CREATED)
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("pendingPayment", pendingPayment);
        
        // 最近订单
        List<Order> recentOrders = allOrders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(5)
                .collect(Collectors.toList());
        stats.put("recentOrders", recentOrders);
        
        // 日期范围
        stats.put("startDate", startDate.toString());
        stats.put("endDate", endDate.toString());
        
        return stats;
    }
    
    /**
     * 获取资产负债表 (Balance Sheet)
     * 资产 = 负债 + 所有者权益
     */
    public Map<String, Object> getBalanceSheet(LocalDate asOfDate) {
        Map<String, Object> balanceSheet = new LinkedHashMap<>();
        
        LocalDateTime endDateTime = asOfDate.plusDays(1).atStartOfDay();
        
        // ========== 资产 (Assets) ==========
        Map<String, Object> assets = new LinkedHashMap<>();
        
        // 1. 流动资产 (Current Assets)
        Map<String, Object> currentAssets = new LinkedHashMap<>();
        
        // 1.1 库存商品 (Inventory) - 所有产品的库存价值
        List<Product> allProducts = productRepository.findAll();
        BigDecimal inventoryValue = allProducts.stream()
                .map(p -> {
                    BigDecimal quantity = p.getStock() != null ? BigDecimal.valueOf(p.getStock()) : BigDecimal.ZERO;
                    BigDecimal cost = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                    return quantity.multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        currentAssets.put("inventory", inventoryValue);
        
        // 1.2 应收账款 (Accounts Receivable) - 未付款的订单
        BigDecimal accountsReceivable = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.UNPAID || o.getStatus() == OrderStatus.CREATED)
                .filter(o -> o.getOrderDate().isBefore(endDateTime))
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        currentAssets.put("accountsReceivable", accountsReceivable);
        
        // 1.3 现金 (Cash) - 已收款金额
        BigDecimal cashReceived = paymentRepository.findAll().stream()
                .filter(p -> "RECEIVABLE".equals(p.getType()))
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().isBefore(endDateTime))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        currentAssets.put("cash", cashReceived);
        
        // 流动资产合计
        BigDecimal totalCurrentAssets = inventoryValue.add(accountsReceivable).add(cashReceived);
        currentAssets.put("total", totalCurrentAssets);
        
        assets.put("currentAssets", currentAssets);
        assets.put("totalAssets", totalCurrentAssets); // 暂时只有流动资产
        
        // ========== 负债 (Liabilities) ==========
        Map<String, Object> liabilities = new LinkedHashMap<>();
        
        // 2. 流动负债 (Current Liabilities)
        Map<String, Object> currentLiabilities = new LinkedHashMap<>();
        
        // 2.1 应付账款 (Accounts Payable) - 未付款的采购
        BigDecimal accountsPayable = purchaseRepository.findAll().stream()
                .filter(p -> p.getPurchaseDate() != null && p.getPurchaseDate().isBefore(endDateTime))
                .map(p -> p.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 减去已支付金额
        BigDecimal cashPaid = paymentRepository.findAll().stream()
                .filter(p -> "PAYABLE".equals(p.getType()))
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().isBefore(endDateTime))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        accountsPayable = accountsPayable.subtract(cashPaid);
        if (accountsPayable.compareTo(BigDecimal.ZERO) < 0) {
            accountsPayable = BigDecimal.ZERO;
        }
        currentLiabilities.put("accountsPayable", accountsPayable);
        
        // 2.2 应交税费 (Tax Payable) - 未缴纳的税额
        BigDecimal taxPayable = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .filter(i -> i.getInvoiceDate() != null && i.getInvoiceDate().isBefore(endDateTime))
                .map(i -> i.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        currentLiabilities.put("taxPayable", taxPayable);
        
        // 流动负债合计
        BigDecimal totalCurrentLiabilities = accountsPayable.add(taxPayable);
        currentLiabilities.put("total", totalCurrentLiabilities);
        
        liabilities.put("currentLiabilities", currentLiabilities);
        liabilities.put("totalLiabilities", totalCurrentLiabilities);
        
        // ========== 所有者权益 (Equity) ==========
        Map<String, Object> equity = new LinkedHashMap<>();
        
        // 所有者权益 = 资产 - 负债
        BigDecimal totalEquity = totalCurrentAssets.subtract(totalCurrentLiabilities);
        equity.put("retainedEarnings", totalEquity); // 留存收益
        equity.put("totalEquity", totalEquity);
        
        // ========== 组装资产负债表 ==========
        balanceSheet.put("asOfDate", asOfDate.toString());
        balanceSheet.put("assets", assets);
        balanceSheet.put("liabilities", liabilities);
        balanceSheet.put("equity", equity);
        
        // 验证资产负债平衡: 资产 = 负债 + 所有者权益
        BigDecimal totalLiabilitiesAndEquity = totalCurrentLiabilities.add(totalEquity);
        balanceSheet.put("balanceCheck", totalCurrentAssets.equals(totalLiabilitiesAndEquity));
        
        return balanceSheet;
    }
    
    /**
     * 获取利润表 (Income Statement / Profit & Loss)
     * 净利润 = 营业收入 - 营业成本 - 费用
     */
    public Map<String, Object> getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> incomeStatement = new LinkedHashMap<>();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // 筛选期间内的订单
        List<Order> periodOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null && 
                            !o.getOrderDate().isBefore(startDateTime) && 
                            o.getOrderDate().isBefore(endDateTime))
                .filter(o -> o.getStatus() == OrderStatus.PAID || 
                           o.getStatus() == OrderStatus.INVOICED || 
                           o.getStatus() == OrderStatus.SHIPPED || 
                           o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());
        
        // ========== 营业收入 (Operating Revenue) ==========
        BigDecimal operatingRevenue = periodOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // ========== 营业成本 (Operating Cost) ==========
        BigDecimal operatingCost = periodOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(item -> item.getCostSubtotal() != null ? item.getCostSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // ========== 毛利润 (Gross Profit) ==========
        BigDecimal grossProfit = operatingRevenue.subtract(operatingCost);
        
        // ========== 毛利率 (Gross Margin %) ==========
        BigDecimal grossMargin = BigDecimal.ZERO;
        if (operatingRevenue.compareTo(BigDecimal.ZERO) > 0) {
            grossMargin = grossProfit.divide(operatingRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        // ========== 期间费用 (Operating Expenses) ==========
        // 注: 当前系统没有费用记录，这里预留接口
        BigDecimal operatingExpenses = BigDecimal.ZERO;
        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        expenses.put("salesExpenses", BigDecimal.ZERO);      // 销售费用
        expenses.put("adminExpenses", BigDecimal.ZERO);      // 管理费用
        expenses.put("financialExpenses", BigDecimal.ZERO);  // 财务费用
        expenses.put("total", operatingExpenses);
        
        // ========== 营业利润 (Operating Profit) ==========
        BigDecimal operatingProfit = grossProfit.subtract(operatingExpenses);
        
        // ========== 净利润 (Net Profit) ==========
        // 注: 暂时等于营业利润，如果有营业外收支和所得税会调整
        BigDecimal netProfit = operatingProfit;
        
        // ========== 净利率 (Net Margin %) ==========
        BigDecimal netMargin = BigDecimal.ZERO;
        if (operatingRevenue.compareTo(BigDecimal.ZERO) > 0) {
            netMargin = netProfit.divide(operatingRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        // ========== 组装利润表 ==========
        incomeStatement.put("period", Map.of("startDate", startDate.toString(), "endDate", endDate.toString()));
        incomeStatement.put("operatingRevenue", operatingRevenue);
        incomeStatement.put("operatingCost", operatingCost);
        incomeStatement.put("grossProfit", grossProfit);
        incomeStatement.put("grossMargin", grossMargin);
        incomeStatement.put("operatingExpenses", expenses);
        incomeStatement.put("operatingProfit", operatingProfit);
        incomeStatement.put("netProfit", netProfit);
        incomeStatement.put("netMargin", netMargin);
        
        // 订单数量统计
        incomeStatement.put("orderCount", periodOrders.size());
        
        return incomeStatement;
    }
    
    /**
     * 获取现金流量表 (Cash Flow Statement)
     * 展示现金的流入和流出
     */
    public Map<String, Object> getCashFlowStatement(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> cashFlow = new LinkedHashMap<>();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // ========== 1. 经营活动现金流 (Operating Activities) ==========
        Map<String, Object> operatingActivities = new LinkedHashMap<>();
        
        // 1.1 销售商品收到的现金 (Cash from Sales)
        BigDecimal cashFromSales = paymentRepository.findAll().stream()
                .filter(p -> "RECEIVABLE".equals(p.getType()))
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .filter(p -> p.getPaymentDate() != null && 
                            !p.getPaymentDate().isBefore(startDateTime) && 
                            p.getPaymentDate().isBefore(endDateTime))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        operatingActivities.put("cashFromSales", cashFromSales);
        
        // 1.2 购买商品支付的现金 (Cash for Purchases)
        BigDecimal cashForPurchases = paymentRepository.findAll().stream()
                .filter(p -> "PAYABLE".equals(p.getType()))
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .filter(p -> p.getPaymentDate() != null && 
                            !p.getPaymentDate().isBefore(startDateTime) && 
                            p.getPaymentDate().isBefore(endDateTime))
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        operatingActivities.put("cashForPurchases", cashForPurchases.negate()); // 支出为负
        
        // 1.3 支付的各项税费 (Tax Payments)
        BigDecimal taxPayments = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .filter(i -> i.getInvoiceDate() != null && 
                            !i.getInvoiceDate().isBefore(startDateTime) && 
                            i.getInvoiceDate().isBefore(endDateTime))
                .map(i -> i.getTaxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        operatingActivities.put("taxPayments", taxPayments.negate()); // 支出为负
        
        // 经营活动现金流净额
        BigDecimal netOperatingCash = cashFromSales
                .subtract(cashForPurchases)
                .subtract(taxPayments);
        operatingActivities.put("netCashFlow", netOperatingCash);
        
        // ========== 2. 投资活动现金流 (Investing Activities) ==========
        Map<String, Object> investingActivities = new LinkedHashMap<>();
        // 注: 当前系统没有投资活动记录，预留接口
        investingActivities.put("purchaseOfAssets", BigDecimal.ZERO);
        investingActivities.put("saleOfAssets", BigDecimal.ZERO);
        investingActivities.put("netCashFlow", BigDecimal.ZERO);
        
        // ========== 3. 筹资活动现金流 (Financing Activities) ==========
        Map<String, Object> financingActivities = new LinkedHashMap<>();
        // 注: 当前系统没有筹资活动记录，预留接口
        financingActivities.put("borrowings", BigDecimal.ZERO);
        financingActivities.put("repayments", BigDecimal.ZERO);
        financingActivities.put("netCashFlow", BigDecimal.ZERO);
        
        // ========== 现金流量总计 ==========
        BigDecimal netCashChange = netOperatingCash; // 只有经营活动有现金流
        
        // 期初现金 (假设为0，实际应该从上期结转)
        BigDecimal beginningCash = BigDecimal.ZERO;
        
        // 期末现金
        BigDecimal endingCash = beginningCash.add(netCashChange);
        
        // ========== 组装现金流量表 ==========
        cashFlow.put("period", Map.of("startDate", startDate.toString(), "endDate", endDate.toString()));
        cashFlow.put("operatingActivities", operatingActivities);
        cashFlow.put("investingActivities", investingActivities);
        cashFlow.put("financingActivities", financingActivities);
        cashFlow.put("netCashChange", netCashChange);
        cashFlow.put("beginningCash", beginningCash);
        cashFlow.put("endingCash", endingCash);
        
        // 现金流健康度指标
        Map<String, Object> healthIndicators = new LinkedHashMap<>();
        healthIndicators.put("operatingCashRatio", calculateRatio(netOperatingCash, cashFromSales));
        healthIndicators.put("cashFlowAdequacy", netOperatingCash.compareTo(BigDecimal.ZERO) > 0);
        cashFlow.put("healthIndicators", healthIndicators);
        
        return cashFlow;
    }
    
    /**
     * 计算比率 (百分比)
     */
    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
