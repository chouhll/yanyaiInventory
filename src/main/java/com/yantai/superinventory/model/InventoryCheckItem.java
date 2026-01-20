package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存盘点明细实体
 * Inventory check item entity for detailed counting records
 */
@Data
@Entity
public class InventoryCheckItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属盘点单
    @ManyToOne
    @JoinColumn(name = "inventory_check_id", nullable = false)
    private InventoryCheck inventoryCheck;

    // 产品
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 库位
    @ManyToOne
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    // 批次
    @ManyToOne
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch;

    // 账面数量（系统库存）
    @Column(nullable = false)
    private BigDecimal bookQuantity;

    // 实际数量（盘点结果）
    @Column(nullable = false)
    private BigDecimal actualQuantity;

    // 差异数量（实际-账面，正数为盘盈，负数为盘亏）
    @Column(nullable = false)
    private BigDecimal discrepancyQuantity;

    // 单位成本
    private BigDecimal unitCost;

    // 差异金额
    private BigDecimal discrepancyAmount;

    // 差异原因
    @Column(length = 500)
    private String discrepancyReason;

    // 处理方式（ADJUST-调整库存, IGNORE-忽略）
    @Enumerated(EnumType.STRING)
    private ProcessAction processAction;

    // 是否已处理
    @Column(nullable = false)
    private Boolean processed = false;

    // 处理时间
    private LocalDateTime processedAt;

    // 备注
    @Column(length = 500)
    private String remarks;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ProcessAction {
        ADJUST,   // 调整库存
        IGNORE    // 忽略差异
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        calculateDiscrepancy();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateDiscrepancy();
    }

    private void calculateDiscrepancy() {
        // 自动计算差异数量
        if (actualQuantity != null && bookQuantity != null) {
            this.discrepancyQuantity = actualQuantity.subtract(bookQuantity);
            
            // 计算差异金额
            if (unitCost != null) {
                this.discrepancyAmount = discrepancyQuantity.multiply(unitCost);
            }
        }
    }

    /**
     * 判断是否盘盈
     */
    public boolean isSurplus() {
        return discrepancyQuantity != null && discrepancyQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否盘亏
     */
    public boolean isShortage() {
        return discrepancyQuantity != null && discrepancyQuantity.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 判断是否有差异
     */
    public boolean hasDiscrepancy() {
        return discrepancyQuantity != null && discrepancyQuantity.compareTo(BigDecimal.ZERO) != 0;
    }
}