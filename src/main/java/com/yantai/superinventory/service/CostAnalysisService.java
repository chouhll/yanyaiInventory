package com.yantai.superinventory.service;

import com.yantai.superinventory.model.*;
import com.yantai.superinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本分析服务
 * 提供产品利润分析、客户利润分析、供应商成本分析
 */
@Service
public class CostAnalysisService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    /**
     * 产品利润分析
     * 分析每个产品的销售额、成本、毛利润和毛利率
     */
    public List<Map<String, Object>> getProductProfitAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> !order.getOrderDate().isBefore(startDate) && order.getOrderDate().isBefore(endDate))
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                           order.getStatus() == OrderStatus.PAID || 
                           order.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        // 按产品分组统计
        Map<Long, ProductStats> productStatsMap = new HashMap<>();
        
        // 预加载所有产品信息避免N+1查询
        Map<Long, Product> productMap = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        for (Order order : orders) {
            if (order.getItems() == null || order.getItems().isEmpty()) continue;
            
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) continue;
                
                Long productId = item.getProduct().getId();
                Product product = productMap.get(productId);
                if (product == null) continue;
                
                ProductStats stats = productStatsMap.computeIfAbsent(productId, 
                    k -> new ProductStats(product));

                BigDecimal salesAmount = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                
                // 使用实际成本或估算
                BigDecimal totalCost = item.getCostSubtotal() != null ? 
                    item.getCostSubtotal() : salesAmount.multiply(new BigDecimal("0.6"));

                stats.addSales(salesAmount, quantity, totalCost);
            }
        }

        // 转换为结果列表并排序
        return productStatsMap.values().stream()
            .map(ProductStats::toMap)
            .sorted((a, b) -> ((BigDecimal) b.get("grossProfit"))
                .compareTo((BigDecimal) a.get("grossProfit")))
            .collect(Collectors.toList());
    }

    /**
     * 客户利润分析
     * 分析每个客户的销售额、毛利贡献和客户价值
     */
    public List<Map<String, Object>> getCustomerProfitAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(order -> order.getOrderDate() != null)
            .filter(order -> !order.getOrderDate().isBefore(startDate) && order.getOrderDate().isBefore(endDate))
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED || 
                           order.getStatus() == OrderStatus.PAID || 
                           order.getStatus() == OrderStatus.INVOICED)
            .filter(order -> order.getCustomer() != null)
            .collect(Collectors.toList());

        // 按客户分组统计
        Map<Long, CustomerStats> customerStatsMap = new HashMap<>();
        
        // 预加载所有客户信息
        Map<Long, Customer> customerMap = customerRepository.findAll().stream()
            .collect(Collectors.toMap(Customer::getId, c -> c));

        for (Order order : orders) {
            if (order.getCustomer() == null) continue;
            
            Long customerId = order.getCustomer().getId();
            Customer customer = customerMap.get(customerId);
            if (customer == null) continue;
            
            CustomerStats stats = customerStatsMap.computeIfAbsent(customerId,
                k -> new CustomerStats(customer));

            BigDecimal orderAmount = BigDecimal.ZERO;
            BigDecimal orderCost = BigDecimal.ZERO;

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    BigDecimal subtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                    orderAmount = orderAmount.add(subtotal);
                    
                    // 使用实际成本或估算
                    BigDecimal itemCost = item.getCostSubtotal() != null ? 
                        item.getCostSubtotal() : subtotal.multiply(new BigDecimal("0.6"));
                    orderCost = orderCost.add(itemCost);
                }
            }

            stats.addOrder(orderAmount, orderCost);
        }

        // 转换为结果列表并排序（按销售额排序）
        return customerStatsMap.values().stream()
            .map(CustomerStats::toMap)
            .sorted((a, b) -> ((BigDecimal) b.get("totalSales"))
                .compareTo((BigDecimal) a.get("totalSales")))
            .collect(Collectors.toList());
    }

    /**
     * 供应商成本分析
     * 分析每个供应商的采购额、采购次数和价格趋势
     */
    public List<Map<String, Object>> getSupplierCostAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getPurchaseDate() != null)
            .filter(p -> !p.getPurchaseDate().isBefore(startDate) && p.getPurchaseDate().isBefore(endDate))
            .filter(p -> p.getSupplier() != null)
            .collect(Collectors.toList());

        // 按供应商分组统计
        Map<Long, SupplierStats> supplierStatsMap = new HashMap<>();
        
        // 预加载所有供应商信息
        Map<Long, Supplier> supplierMap = supplierRepository.findAll().stream()
            .collect(Collectors.toMap(Supplier::getId, s -> s));

        for (Purchase purchase : purchases) {
            if (purchase.getSupplier() == null) continue;
            
            Long supplierId = purchase.getSupplier().getId();
            Supplier supplier = supplierMap.get(supplierId);
            if (supplier == null) continue;
            
            SupplierStats stats = supplierStatsMap.computeIfAbsent(supplierId,
                k -> new SupplierStats(supplier));

            BigDecimal amount = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal quantity = purchase.getQuantity() != null ? purchase.getQuantity() : BigDecimal.ZERO;
            BigDecimal unitPrice = purchase.getUnitPrice() != null ? purchase.getUnitPrice() : BigDecimal.ZERO;
            
            stats.addPurchase(amount, quantity, unitPrice);
        }

        // 转换为结果列表并排序（按采购额排序）
        return supplierStatsMap.values().stream()
            .map(SupplierStats::toMap)
            .sorted((a, b) -> ((BigDecimal) b.get("totalPurchaseAmount"))
                .compareTo((BigDecimal) a.get("totalPurchaseAmount")))
            .collect(Collectors.toList());
    }

    /**
     * 获取产品销售排行榜
     */
    public List<Map<String, Object>> getProductSalesRanking(LocalDateTime startDate, LocalDateTime endDate, int topN) {
        List<Map<String, Object>> analysis = getProductProfitAnalysis(startDate, endDate);
        return analysis.stream()
            .limit(topN)
            .collect(Collectors.toList());
    }

    /**
     * 获取客户价值评估（按客户终身价值排序）
     */
    public List<Map<String, Object>> getCustomerValueRanking(LocalDateTime startDate, LocalDateTime endDate, int topN) {
        List<Map<String, Object>> analysis = getCustomerProfitAnalysis(startDate, endDate);
        return analysis.stream()
            .limit(topN)
            .collect(Collectors.toList());
    }

    // 内部类：产品统计
    private static class ProductStats {
        private Product product;
        private BigDecimal totalSales = BigDecimal.ZERO;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private int orderCount = 0;

        public ProductStats(Product product) {
            this.product = product;
        }

        public void addSales(BigDecimal salesAmount, BigDecimal quantity, BigDecimal cost) {
            this.totalSales = this.totalSales.add(salesAmount);
            this.totalQuantity = this.totalQuantity.add(quantity);
            this.totalCost = this.totalCost.add(cost);
            this.orderCount++;
        }

        public Map<String, Object> toMap() {
            BigDecimal grossProfit = totalSales.subtract(totalCost);
            BigDecimal grossProfitRate = totalSales.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalSales, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

            Map<String, Object> map = new HashMap<>();
            map.put("productId", product.getId());
            map.put("productName", product.getName());
            map.put("productCode", product.getInventoryCode());
            map.put("specification", product.getSpecification());
            map.put("totalSales", totalSales);
            map.put("totalQuantity", totalQuantity);
            map.put("totalCost", totalCost);
            map.put("grossProfit", grossProfit);
            map.put("grossProfitRate", grossProfitRate);
            map.put("orderCount", orderCount);
            map.put("averagePrice", totalQuantity.compareTo(BigDecimal.ZERO) > 0 ?
                totalSales.divide(totalQuantity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            return map;
        }
    }

    // 内部类：客户统计
    private static class CustomerStats {
        private Customer customer;
        private BigDecimal totalSales = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private int orderCount = 0;

        public CustomerStats(Customer customer) {
            this.customer = customer;
        }

        public void addOrder(BigDecimal salesAmount, BigDecimal cost) {
            this.totalSales = this.totalSales.add(salesAmount);
            this.totalCost = this.totalCost.add(cost);
            this.orderCount++;
        }

        public Map<String, Object> toMap() {
            BigDecimal grossProfit = totalSales.subtract(totalCost);
            BigDecimal grossProfitRate = totalSales.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalSales, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;
            BigDecimal averageOrderValue = orderCount > 0 ?
                totalSales.divide(new BigDecimal(orderCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

            Map<String, Object> map = new HashMap<>();
            map.put("customerId", customer.getId());
            map.put("customerName", customer.getName());
            map.put("totalSales", totalSales);
            map.put("totalCost", totalCost);
            map.put("grossProfit", grossProfit);
            map.put("grossProfitRate", grossProfitRate);
            map.put("orderCount", orderCount);
            map.put("averageOrderValue", averageOrderValue);
            // 客户价值评分（综合考虑销售额和订单频次）
            BigDecimal valueScore = totalSales.add(new BigDecimal(orderCount * 100));
            map.put("valueScore", valueScore);
            return map;
        }
    }

    // 内部类：供应商统计
    private static class SupplierStats {
        private Supplier supplier;
        private BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private int purchaseCount = 0;
        private List<BigDecimal> prices = new ArrayList<>();

        public SupplierStats(Supplier supplier) {
            this.supplier = supplier;
        }

        public void addPurchase(BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice) {
            this.totalPurchaseAmount = this.totalPurchaseAmount.add(amount);
            this.totalQuantity = this.totalQuantity.add(quantity);
            this.purchaseCount++;
            this.prices.add(unitPrice);
        }

        public Map<String, Object> toMap() {
            BigDecimal averagePrice = !prices.isEmpty() ?
                prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(prices.size()), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

            BigDecimal minPrice = !prices.isEmpty() ?
                prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO) :
                BigDecimal.ZERO;

            BigDecimal maxPrice = !prices.isEmpty() ?
                prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO) :
                BigDecimal.ZERO;

            // 价格稳定性（价格区间越小越稳定）
            BigDecimal priceVolatility = maxPrice.compareTo(BigDecimal.ZERO) > 0 ?
                maxPrice.subtract(minPrice).divide(maxPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

            Map<String, Object> map = new HashMap<>();
            map.put("supplierId", supplier.getId());
            map.put("supplierName", supplier.getName());
            map.put("creditRating", supplier.getCreditRating());
            map.put("totalPurchaseAmount", totalPurchaseAmount);
            map.put("totalQuantity", totalQuantity);
            map.put("purchaseCount", purchaseCount);
            map.put("averagePrice", averagePrice);
            map.put("minPrice", minPrice);
            map.put("maxPrice", maxPrice);
            map.put("priceVolatility", priceVolatility);
            map.put("averagePurchaseAmount", purchaseCount > 0 ?
                totalPurchaseAmount.divide(new BigDecimal(purchaseCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
            return map;
        }
    }
}