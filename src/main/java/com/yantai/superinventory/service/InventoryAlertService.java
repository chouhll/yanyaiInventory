package com.yantai.superinventory.service;

import com.yantai.superinventory.model.InventoryBatch;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.repository.InventoryBatchRepository;
import com.yantai.superinventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存预警服务
 * Inventory alert service for managing stock alerts and warnings
 */
@Service
public class InventoryAlertService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryBatchRepository batchRepository;

    /**
     * 库存预警DTO
     */
    public static class InventoryAlert {
        private String alertType; // LOW_STOCK, OUT_OF_STOCK, OVER_STOCK, EXPIRING_SOON, EXPIRED, SLOW_MOVING
        private String severity;  // HIGH, MEDIUM, LOW
        private Product product;
        private Integer currentStock;
        private Integer safetyStock;
        private Integer maxStock;
        private String message;
        private InventoryBatch batch; // 用于过期预警
        private LocalDate expirationDate;
        private Integer daysUntilExpiration;

        // Constructors
        public InventoryAlert() {}

        public InventoryAlert(String alertType, String severity, Product product, String message) {
            this.alertType = alertType;
            this.severity = severity;
            this.product = product;
            this.currentStock = product.getStock();
            this.safetyStock = product.getSafetyStock();
            this.maxStock = product.getMaxStock();
            this.message = message;
        }

        // Getters and Setters
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public Integer getCurrentStock() { return currentStock; }
        public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
        public Integer getSafetyStock() { return safetyStock; }
        public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }
        public Integer getMaxStock() { return maxStock; }
        public void setMaxStock(Integer maxStock) { this.maxStock = maxStock; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public InventoryBatch getBatch() { return batch; }
        public void setBatch(InventoryBatch batch) { this.batch = batch; }
        public LocalDate getExpirationDate() { return expirationDate; }
        public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
        public Integer getDaysUntilExpiration() { return daysUntilExpiration; }
        public void setDaysUntilExpiration(Integer daysUntilExpiration) { this.daysUntilExpiration = daysUntilExpiration; }
    }

    /**
     * 获取所有库存预警
     */
    public List<InventoryAlert> getAllAlerts() {
        List<InventoryAlert> alerts = new ArrayList<>();
        
        // 1. 低库存预警
        alerts.addAll(getLowStockAlerts());
        
        // 2. 缺货预警
        alerts.addAll(getOutOfStockAlerts());
        
        // 3. 超库存预警
        alerts.addAll(getOverStockAlerts());
        
        // 4. 即将过期预警
        alerts.addAll(getExpiringSoonAlerts());
        
        // 5. 已过期预警
        alerts.addAll(getExpiredAlerts());
        
        // 6. 滞销品预警
        alerts.addAll(getSlowMovingAlerts());
        
        return alerts;
    }

    /**
     * 低库存预警
     */
    public List<InventoryAlert> getLowStockAlerts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .filter(p -> p.getAlertEnabled() && p.isLowStock() && !p.isOutOfStock())
            .map(p -> new InventoryAlert(
                "LOW_STOCK",
                "MEDIUM",
                p,
                String.format("产品 %s 库存低于安全库存！当前库存：%d，安全库存：%d", 
                    p.getName(), p.getStock(), p.getSafetyStock())
            ))
            .collect(Collectors.toList());
    }

    /**
     * 缺货预警
     */
    public List<InventoryAlert> getOutOfStockAlerts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .filter(p -> p.getAlertEnabled() && p.isOutOfStock())
            .map(p -> new InventoryAlert(
                "OUT_OF_STOCK",
                "HIGH",
                p,
                String.format("产品 %s 已缺货！请及时补货", p.getName())
            ))
            .collect(Collectors.toList());
    }

    /**
     * 超库存预警
     */
    public List<InventoryAlert> getOverStockAlerts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .filter(p -> p.getAlertEnabled() && p.isOverStock())
            .map(p -> new InventoryAlert(
                "OVER_STOCK",
                "LOW",
                p,
                String.format("产品 %s 库存超过最大库存！当前库存：%d，最大库存：%d", 
                    p.getName(), p.getStock(), p.getMaxStock())
            ))
            .collect(Collectors.toList());
    }

    /**
     * 即将过期预警（30天内）
     */
    public List<InventoryAlert> getExpiringSoonAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        
        List<InventoryBatch> expiringBatches = batchRepository.findExpiringSoonBatches(today, thirtyDaysLater);
        
        return expiringBatches.stream()
            .map(batch -> {
                InventoryAlert alert = new InventoryAlert(
                    "EXPIRING_SOON",
                    "MEDIUM",
                    batch.getProduct(),
                    String.format("产品 %s 批次 %s 即将过期！到期日期：%s，剩余数量：%s", 
                        batch.getProduct().getName(), 
                        batch.getBatchNumber(),
                        batch.getExpirationDate(),
                        batch.getRemainingQuantity())
                );
                alert.setBatch(batch);
                alert.setExpirationDate(batch.getExpirationDate());
                alert.setDaysUntilExpiration(
                    (int) java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpirationDate())
                );
                return alert;
            })
            .collect(Collectors.toList());
    }

    /**
     * 已过期预警
     */
    public List<InventoryAlert> getExpiredAlerts() {
        LocalDate today = LocalDate.now();
        List<InventoryBatch> expiredBatches = batchRepository.findExpiredBatches(today);
        
        return expiredBatches.stream()
            .map(batch -> {
                InventoryAlert alert = new InventoryAlert(
                    "EXPIRED",
                    "HIGH",
                    batch.getProduct(),
                    String.format("产品 %s 批次 %s 已过期！到期日期：%s，剩余数量：%s，请及时处理", 
                        batch.getProduct().getName(), 
                        batch.getBatchNumber(),
                        batch.getExpirationDate(),
                        batch.getRemainingQuantity())
                );
                alert.setBatch(batch);
                alert.setExpirationDate(batch.getExpirationDate());
                return alert;
            })
            .collect(Collectors.toList());
    }

    /**
     * 滞销品预警（标记为滞销的产品）
     */
    public List<InventoryAlert> getSlowMovingAlerts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .filter(p -> p.getSlowMoving() && p.getStock() != null && p.getStock() > 0)
            .map(p -> new InventoryAlert(
                "SLOW_MOVING",
                "LOW",
                p,
                String.format("产品 %s 为滞销品，当前库存：%d，请考虑促销或清仓", 
                    p.getName(), p.getStock())
            ))
            .collect(Collectors.toList());
    }

    /**
     * 获取预警统计
     */
    public Map<String, Integer> getAlertStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("lowStock", getLowStockAlerts().size());
        stats.put("outOfStock", getOutOfStockAlerts().size());
        stats.put("overStock", getOverStockAlerts().size());
        stats.put("expiringSoon", getExpiringSoonAlerts().size());
        stats.put("expired", getExpiredAlerts().size());
        stats.put("slowMoving", getSlowMovingAlerts().size());
        stats.put("total", getAllAlerts().size());
        return stats;
    }

    /**
     * 按严重程度获取预警
     */
    public List<InventoryAlert> getAlertsBySeverity(String severity) {
        return getAllAlerts().stream()
            .filter(alert -> alert.getSeverity().equals(severity))
            .collect(Collectors.toList());
    }

    /**
     * 按产品获取预警
     */
    public List<InventoryAlert> getAlertsByProduct(Long productId) {
        return getAllAlerts().stream()
            .filter(alert -> alert.getProduct().getId().equals(productId))
            .collect(Collectors.toList());
    }
}