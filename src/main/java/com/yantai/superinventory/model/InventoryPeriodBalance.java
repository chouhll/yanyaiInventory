package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "period"})
})
public class InventoryPeriodBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联产品
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 期间 (年月，如 2025-12)
    @Column(nullable = false)
    private String period;

    // 期初数量
    @Column(nullable = false)
    private BigDecimal beginningQuantity;

    // 期初单价
    @Column(nullable = false)
    private BigDecimal beginningUnitPrice;

    // 期初金额
    @Column(nullable = false)
    private BigDecimal beginningAmount;

    // 入库数量
    @Column(nullable = false)
    private BigDecimal inboundQuantity;

    // 入库单价
    private BigDecimal inboundUnitPrice;

    // 入库金额
    @Column(nullable = false)
    private BigDecimal inboundAmount;

    // 出库数量
    @Column(nullable = false)
    private BigDecimal outboundQuantity;

    // 出库成本单价
    private BigDecimal outboundCostUnitPrice;

    // 出库成本金额
    @Column(nullable = false)
    private BigDecimal outboundCostAmount;

    // 结存数量
    @Column(nullable = false)
    private BigDecimal endingQuantity;

    // 结存单价
    @Column(nullable = false)
    private BigDecimal endingUnitPrice;

    // 结存金额
    @Column(nullable = false)
    private BigDecimal endingAmount;

    // 仓库/单位
    private String warehouse;

    // 备注
    private String remarks;
}