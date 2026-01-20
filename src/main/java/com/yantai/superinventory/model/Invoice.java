package com.yantai.superinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;  // 发票号码
    
    // 发票类型枚举：销项发票(OUTPUT)和进项发票(INPUT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceDirection direction = InvoiceDirection.OUTPUT;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore  // 防止循环引用
    private Order order;  // 关联订单（销项发票）
    
    @ManyToOne
    @JoinColumn(name = "purchase_id")
    @JsonIgnore
    private Purchase purchase;  // 关联采购单（进项发票）
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;  // 客户（销项发票）
    
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;  // 供应商（进项发票）
    
    private LocalDateTime invoiceDate;  // 开票日期
    
    // 发票抬头信息（冗余存储，防止客户/供应商信息变更）
    private String taxNumber;       // 税号
    private String companyName;     // 公司抬头
    private String companyAddress;  // 公司地址
    private String bankName;        // 开户银行
    private String bankAccount;     // 银行账号
    
    // 进项发票认证信息
    private Boolean isAuthenticated = false;  // 是否已认证（仅进项发票）
    private LocalDate authenticationDate;     // 认证日期
    private String authenticationRemark;      // 认证备注
    
    // 发票金额信息
    private BigDecimal totalAmount;     // 总金额
    private BigDecimal taxAmount;       // 税额
    private BigDecimal amountWithTax;   // 价税合计
    
    @Column(columnDefinition = "TEXT")
    private String items;  // 发票明细（JSON格式存储）
    
    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType = InvoiceType.SPECIAL;  // 发票类型：NORMAL-普通发票, SPECIAL-专用发票
    
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.ISSUED;  // 状态：ISSUED-已开具, VOIDED-已作废, RETURNED-已红冲
    
    private String remark;  // 备注
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (invoiceDate == null) {
            invoiceDate = LocalDateTime.now();
        }
        // 自动生成发票号码
        if (invoiceNumber == null) {
            String prefix = (direction == InvoiceDirection.INPUT) ? "INV-IN-" : "INV-OUT-";
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            invoiceNumber = prefix + dateStr + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 发票方向枚举
     */
    public enum InvoiceDirection {
        OUTPUT,  // 销项发票（开给客户）
        INPUT    // 进项发票（从供应商收到）
    }
    
    /**
     * 发票类型枚举
     */
    public enum InvoiceType {
        NORMAL,   // 普通发票
        SPECIAL   // 专用发票（可抵扣）
    }
    
    /**
     * 发票状态枚举
     */
    public enum InvoiceStatus {
        ISSUED,    // 已开具
        VOIDED,    // 已作废
        RETURNED   // 已红冲
    }
}
