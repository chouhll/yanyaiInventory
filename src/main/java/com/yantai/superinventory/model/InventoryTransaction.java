package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联产品
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 交易类型：INBOUND(入库), OUTBOUND(出库)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // 交易日期
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // 数量
    @Column(nullable = false)
    private BigDecimal quantity;

    // 单价
    @Column(nullable = false)
    private BigDecimal unitPrice;

    // 金额 (数量 * 单价)
    @Column(nullable = false)
    private BigDecimal amount;

    // 成本单价 (用于出库)
    private BigDecimal costUnitPrice;

    // 成本金额 (用于出库)
    private BigDecimal costAmount;

    // 关联的采购单或销售单ID (支持Long和String UUID)
    private String referenceId;

    // 备注
    private String remarks;

    // 仓库/位置
    private String warehouse;

    public enum TransactionType {
        INBOUND,  // 入库
        OUTBOUND  // 出库
    }
}