package com.yantai.superinventory.service;

import com.yantai.superinventory.model.*;
import com.yantai.superinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 * 提供销售趋势、采购趋势、库存周转率等分析
 */
@Service
public class AnalyticsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * 获取销售趋势数据（按日/周/月）
     */
    public List<Map<String, Object>> getSalesTrend(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getOrderDate().isAfter(startDate) && order.getOrderDate().isBefore(endDate))
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                           order.getStatus() == OrderStatus.PAID || 
                           order.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        return groupAndCalculate(orders, groupBy, true);
    }

    /**
     * 获取采购趋势数据（按日/周/月）
     */
    public List<Map<String, Object>> getPurchaseTrend(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getPurchaseDate() != null)
            .filter(p -> p.getPurchaseDate().isAfter(startDate) && p.getPurchaseDate().isBefore(endDate))
            .collect(Collectors.toList());

        return groupAndCalculatePurchases(purchases, groupBy);
    }

    /**
     * 获取库存周转率分析
     */
    public List<Map<String, Object>> getInventoryTurnoverAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Product product : products) {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("productId", product.getId());
            analysis.put("productName", product.getName());
            analysis.put("inventoryCode", product.getInventoryCode());
            analysis.put("category", product.getCategory());

            // 计算销售数量
            BigDecimal salesQuantity = calculateProductSalesQuantity(product.getId(), startDate, endDate);
            
            // 计算平均库存（简化为当前库存）
            BigDecimal avgInventory = product.getStock() != null ? 
                new BigDecimal(product.getStock()) : BigDecimal.ZERO;

            // 计算周转率 = 销售数量 / 平均库存
            BigDecimal turnoverRate = BigDecimal.ZERO;
            if (avgInventory.compareTo(BigDecimal.ZERO) > 0) {
                turnoverRate = salesQuantity.divide(avgInventory, 2, RoundingMode.HALF_UP);
            }

            // 计算周转天数 = 期间天数 / 周转率
            long periodDays = java.time.temporal.ChronoUnit.DAYS.between(
                startDate.toLocalDate(), endDate.toLocalDate());
            BigDecimal turnoverDays = BigDecimal.ZERO;
            if (turnoverRate.compareTo(BigDecimal.ZERO) > 0) {
                turnoverDays = new BigDecimal(periodDays).divide(turnoverRate, 0, RoundingMode.HALF_UP);
            }

            analysis.put("currentStock", product.getStock());
            analysis.put("salesQuantity", salesQuantity);
            analysis.put("turnoverRate", turnoverRate);
            analysis.put("turnoverDays", turnoverDays);
            
            // 周转率评级
            String rating = "优秀";
            if (turnoverRate.compareTo(new BigDecimal("5")) >= 0) {
                rating = "优秀";
            } else if (turnoverRate.compareTo(new BigDecimal("3")) >= 0) {
                rating = "良好";
            } else if (turnoverRate.compareTo(new BigDecimal("1")) >= 0) {
                rating = "一般";
            } else {
                rating = "滞销";
            }
            analysis.put("turnoverRating", rating);

            result.add(analysis);
        }

        // 按周转率排序
        return result.stream()
            .sorted((a, b) -> ((BigDecimal) b.get("turnoverRate"))
                .compareTo((BigDecimal) a.get("turnoverRate")))
            .collect(Collectors.toList());
    }

    /**
     * 获取经营分析看板数据
     */
    public Map<String, Object> getBusinessAnalysisDashboard(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> dashboard = new HashMap<>();

        // 销售分析
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getOrderDate().isAfter(startDate) && order.getOrderDate().isBefore(endDate))
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                           order.getStatus() == OrderStatus.PAID || 
                           order.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        BigDecimal totalSales = BigDecimal.ZERO;
        int totalOrderCount = orders.size();
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    totalSales = totalSales.add(item.getSubtotal());
                }
            }
        }

        // 采购分析
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getPurchaseDate() != null)
            .filter(p -> p.getPurchaseDate().isAfter(startDate) && p.getPurchaseDate().isBefore(endDate))
            .collect(Collectors.toList());

        BigDecimal totalPurchase = purchases.stream()
            .map(Purchase::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalPurchaseCount = purchases.size();

        // 利润分析
        BigDecimal estimatedCost = totalSales.multiply(new BigDecimal("0.6"));
        BigDecimal grossProfit = totalSales.subtract(estimatedCost);
        BigDecimal profitMargin = totalSales.compareTo(BigDecimal.ZERO) > 0 ?
            grossProfit.divide(totalSales, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
            BigDecimal.ZERO;

        // 库存分析
        List<Product> products = productRepository.findAll();
        int totalProducts = products.size();
        int lowStockCount = (int) products.stream().filter(Product::isLowStock).count();
        int outOfStockCount = (int) products.stream().filter(Product::isOutOfStock).count();

        dashboard.put("salesMetrics", Map.of(
            "totalSales", totalSales,
            "orderCount", totalOrderCount,
            "averageOrderValue", totalOrderCount > 0 ? 
                totalSales.divide(new BigDecimal(totalOrderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO
        ));

        dashboard.put("purchaseMetrics", Map.of(
            "totalPurchase", totalPurchase,
            "purchaseCount", totalPurchaseCount,
            "averagePurchaseValue", totalPurchaseCount > 0 ?
                totalPurchase.divide(new BigDecimal(totalPurchaseCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO
        ));

        dashboard.put("profitMetrics", Map.of(
            "grossProfit", grossProfit,
            "profitMargin", profitMargin,
            "estimatedCost", estimatedCost
        ));

        dashboard.put("inventoryMetrics", Map.of(
            "totalProducts", totalProducts,
            "lowStockCount", lowStockCount,
            "outOfStockCount", outOfStockCount,
            "normalStockCount", totalProducts - lowStockCount - outOfStockCount
        ));

        return dashboard;
    }

    /**
     * 获取月度对比分析
     */
    public Map<String, Object> getMonthlyComparison(int year) {
        Map<String, Object> comparison = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("monthName", month + "月");

            // 销售数据
            List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getOrderDate() != null)
                .filter(order -> order.getOrderDate().isAfter(startDate) && order.getOrderDate().isBefore(endDate))
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                               order.getStatus() == OrderStatus.PAID || 
                               order.getStatus() == OrderStatus.INVOICED)
                .collect(Collectors.toList());

            BigDecimal salesAmount = BigDecimal.ZERO;
            for (Order order : orders) {
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        salesAmount = salesAmount.add(item.getSubtotal());
                    }
                }
            }

            // 采购数据
            BigDecimal purchaseAmount = purchaseRepository.findAll().stream()
                .filter(p -> p.getPurchaseDate() != null)
                .filter(p -> p.getPurchaseDate().isAfter(startDate) && p.getPurchaseDate().isBefore(endDate))
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthData.put("sales", salesAmount);
            monthData.put("purchase", purchaseAmount);
            monthData.put("profit", salesAmount.subtract(purchaseAmount.multiply(new BigDecimal("0.6"))));

            monthlyData.add(monthData);
        }

        comparison.put("year", year);
        comparison.put("monthlyData", monthlyData);

        return comparison;
    }

    // 私有辅助方法

    private List<Map<String, Object>> groupAndCalculate(List<Order> orders, String groupBy, boolean isSales) {
        Map<String, TrendData> grouped = new TreeMap<>();

        for (Order order : orders) {
            String key = getGroupKey(order.getOrderDate(), groupBy);
            TrendData data = grouped.computeIfAbsent(key, k -> new TrendData(key));

            BigDecimal amount = BigDecimal.ZERO;
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    amount = amount.add(item.getSubtotal());
                }
            }

            data.addAmount(amount);
            data.incrementCount();
        }

        return grouped.values().stream()
            .map(TrendData::toMap)
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> groupAndCalculatePurchases(List<Purchase> purchases, String groupBy) {
        Map<String, TrendData> grouped = new TreeMap<>();

        for (Purchase purchase : purchases) {
            String key = getGroupKey(purchase.getPurchaseDate(), groupBy);
            TrendData data = grouped.computeIfAbsent(key, k -> new TrendData(key));

            data.addAmount(purchase.getTotalAmount());
            data.incrementCount();
        }

        return grouped.values().stream()
            .map(TrendData::toMap)
            .collect(Collectors.toList());
    }

    private String getGroupKey(LocalDateTime dateTime, String groupBy) {
        LocalDate date = dateTime.toLocalDate();
        switch (groupBy.toLowerCase()) {
            case "day":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "week":
                int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                return date.getYear() + "-W" + String.format("%02d", weekOfYear);
            case "month":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default:
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    private BigDecimal calculateProductSalesQuantity(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> order.getOrderDate().isAfter(startDate) && order.getOrderDate().isBefore(endDate))
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                           order.getStatus() == OrderStatus.PAID || 
                           order.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProduct().getId().equals(productId)) {
                        total = total.add(item.getQuantity());
                    }
                }
            }
        }
        return total;
    }

    // 内部类：趋势数据
    private static class TrendData {
        private String period;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int count = 0;

        public TrendData(String period) {
            this.period = period;
        }

        public void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
        }

        public void incrementCount() {
            this.count++;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("period", period);
            map.put("amount", totalAmount);
            map.put("count", count);
            map.put("average", count > 0 ? 
                totalAmount.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            return map;
        }
    }
}