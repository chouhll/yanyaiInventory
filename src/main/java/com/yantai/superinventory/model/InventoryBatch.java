package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存批次实体
 * Inventory batch entity for batch and expiration management (FIFO)
 */
@Data
@Entity
public class InventoryBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 批次号
    @Column(unique = true, nullable = false)
    private String batchNumber;

    // 产品
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 仓库
    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // 库位
    @ManyToOne
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    // 采购单关联
    private String purchaseReference;

    // 生产日期
    private LocalDate productionDate;

    // 到期日期（保质期）
    private LocalDate expirationDate;

    // 入库日期
    @Column(nullable = false)
    private LocalDate inboundDate;

    // 初始数量
    @Column(nullable = false)
    private BigDecimal initialQuantity;

    // 剩余数量
    @Column(nullable = false)
    private BigDecimal remainingQuantity;

    // 单位成本
    @Column(nullable = false)
    private BigDecimal unitCost;

    // 批次状态（AVAILABLE-可用, LOCKED-锁定, EXPIRED-过期, DEPLETED-已用完）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.AVAILABLE;

    // 供应商
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // 备注
    @Column(length = 500)
    private String remarks;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum BatchStatus {
        AVAILABLE,  // 可用
        LOCKED,     // 锁定
        EXPIRED,    // 过期
        DEPLETED    // 已用完
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // 自动更新状态
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = BatchStatus.DEPLETED;
        } else if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            this.status = BatchStatus.EXPIRED;
        }
    }

    /**
     * 检查批次是否即将过期（30天内）
     */
    public boolean isExpiringSoon() {
        if (expirationDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(30);
        return expirationDate.isBefore(warningDate) && expirationDate.isAfter(LocalDate.now());
    }

    /**
     * 检查批次是否已过期
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now());
    }
}