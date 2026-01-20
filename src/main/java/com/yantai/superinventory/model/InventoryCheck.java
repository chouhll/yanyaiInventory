package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存盘点单实体
 * Inventory check/counting entity for periodic stock verification
 */
@Data
@Entity
public class InventoryCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 盘点单号（自动生成：CHK-YYYYMMDD-XXXX）
    @Column(unique = true, nullable = false)
    private String checkNumber;

    // 盘点日期
    @Column(nullable = false)
    private LocalDateTime checkDate;

    // 盘点仓库
    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // 盘点类型（FULL-全盘, PARTIAL-抽盘, CYCLE-循环盘）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckType checkType;

    // 盘点状态（DRAFT-草稿, IN_PROGRESS-进行中, COMPLETED-已完成, APPROVED-已审批）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status = CheckStatus.DRAFT;

    // 盘点人
    private String checker;

    // 审核人
    private String approver;

    // 审核日期
    private LocalDateTime approvalDate;

    // 盘点明细
    @OneToMany(mappedBy = "inventoryCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryCheckItem> items = new ArrayList<>();

    // 备注
    @Column(length = 1000)
    private String remarks;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum CheckType {
        FULL,      // 全盘
        PARTIAL,   // 抽盘
        CYCLE      // 循环盘
    }

    public enum CheckStatus {
        DRAFT,         // 草稿
        IN_PROGRESS,   // 进行中
        COMPLETED,     // 已完成
        APPROVED       // 已审批
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 计算盘点差异总数
     */
    public int getTotalDiscrepancies() {
        return (int) items.stream()
            .filter(item -> item.getDiscrepancyQuantity().compareTo(BigDecimal.ZERO) != 0)
            .count();
    }

    /**
     * 计算盘盈数量
     */
    public BigDecimal getTotalSurplus() {
        return items.stream()
            .map(InventoryCheckItem::getDiscrepancyQuantity)
            .filter(qty -> qty.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算盘亏数量
     */
    public BigDecimal getTotalShortage() {
        return items.stream()
            .map(InventoryCheckItem::getDiscrepancyQuantity)
            .filter(qty -> qty.compareTo(BigDecimal.ZERO) < 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .abs();
    }
}