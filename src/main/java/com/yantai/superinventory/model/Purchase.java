package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 采购日期
    private LocalDateTime purchaseDate;

    // 关联产品
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // 采购数量
    private BigDecimal quantity;

    // 采购单价
    private BigDecimal unitPrice;

    // 采购总额
    private BigDecimal totalAmount;

    // 供应商（关联Supplier实体）
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // 采购单号
    private String purchaseOrderNo;

    // 仓库
    private String warehouse;

    // 备注
    private String remarks;

    // 入库状态
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    public enum PurchaseStatus {
        PENDING,    // 待入库
        COMPLETED,  // 已入库
        CANCELLED   // 已取消
    }
}
