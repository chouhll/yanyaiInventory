package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收付款记录实体
 * 用于记录订单收款和采购付款
 */
@Data
@Entity
@Table(name = "payment")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 付款类型：RECEIVABLE(应收/收款), PAYABLE(应付/付款)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type;

    /**
     * 关联订单（收款时使用）
     */
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * 关联采购单（付款时使用）
     */
    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    /**
     * 付款金额
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 付款方式：CASH(现金), TRANSFER(银行转账), CHECK(支票), CREDIT_CARD(信用卡), OTHER(其他)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    /**
     * 付款日期
     */
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    /**
     * 付款流水号/凭证号
     */
    @Column(length = 100)
    private String transactionNumber;

    /**
     * 付款账户
     */
    @Column(length = 100)
    private String accountFrom;

    /**
     * 收款账户
     */
    @Column(length = 100)
    private String accountTo;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remarks;

    /**
     * 状态：PENDING(待确认), COMPLETED(已完成), CANCELLED(已取消)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.COMPLETED;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 付款类型枚举
     */
    public enum PaymentType {
        RECEIVABLE,  // 应收款/收款（来自订单）
        PAYABLE      // 应付款/付款（支付给供应商）
    }

    /**
     * 付款方式枚举
     */
    public enum PaymentMethod {
        CASH,         // 现金
        TRANSFER,     // 银行转账
        CHECK,        // 支票
        CREDIT_CARD,  // 信用卡
        OTHER         // 其他
    }

    /**
     * 付款状态枚举
     */
    public enum PaymentStatus {
        PENDING,    // 待确认
        COMPLETED,  // 已完成
        CANCELLED   // 已取消
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}