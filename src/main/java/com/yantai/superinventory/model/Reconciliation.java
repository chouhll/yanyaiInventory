package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账单实体
 * 用于与供应商和客户进行账务核对
 */
@Data
@Entity
public class Reconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 对账单号
    @Column(unique = true, nullable = false)
    private String reconciliationNumber;

    // 对账类型：SUPPLIER-供应商对账, CUSTOMER-客户对账
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationType type;

    // 关联供应商（供应商对账）
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // 关联客户（客户对账）
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // 对账期间
    @Column(nullable = false)
    private LocalDate periodStart;
    
    @Column(nullable = false)
    private LocalDate periodEnd;

    // 对账金额
    private BigDecimal ourAmount;        // 我方金额
    private BigDecimal theirAmount;      // 对方金额
    private BigDecimal differenceAmount; // 差异金额

    // 对账状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status = ReconciliationStatus.DRAFT;

    // 对账日期
    private LocalDate reconciliationDate;

    // 确认日期
    private LocalDate confirmedDate;

    // 确认人
    private String confirmedBy;

    // 差异说明
    @Column(columnDefinition = "TEXT")
    private String differenceRemark;

    // 对账明细（JSON格式存储）
    @Column(columnDefinition = "TEXT")
    private String details;

    // 附件路径
    private String attachmentPath;

    // 备注
    private String remark;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reconciliationNumber == null) {
            String prefix = (type == ReconciliationType.SUPPLIER) ? "REC-SUP-" : "REC-CUS-";
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            reconciliationNumber = prefix + dateStr + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 对账类型枚举
     */
    public enum ReconciliationType {
        SUPPLIER,  // 供应商对账
        CUSTOMER   // 客户对账
    }

    /**
     * 对账状态枚举
     */
    public enum ReconciliationStatus {
        DRAFT,       // 草稿
        SUBMITTED,   // 已提交
        CONFIRMED,   // 已确认
        DISPUTED,    // 有异议
        RESOLVED     // 已解决
    }
}