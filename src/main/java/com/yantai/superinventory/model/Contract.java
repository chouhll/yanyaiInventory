package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true)
    private String contractNumber;  // 合同编号
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;            // 关联订单
    
    // 卖方信息（固定）
    private String sellerName = "上海燕泰实业有限公司";
    private String sellerAddress;   // 卖方地址
    private String sellerContact;   // 卖方联系人
    private String sellerPhone;     // 卖方电话
    private String sellerTaxNumber; // 卖方税号
    
    // 买方信息（从客户表获取）
    private String buyerName;       // 买方名称
    private String buyerAddress;    // 买方地址
    private String buyerContact;    // 买方联系人
    private String buyerPhone;      // 买方电话
    private String buyerTaxNumber;  // 买方税号
    
    // 合同信息
    private LocalDateTime contractDate;    // 合同日期
    private LocalDateTime deliveryDate;    // 交货日期
    private String paymentTerms;           // 付款条款
    private String deliveryTerms;          // 交货条款
    private String specialTerms;           // 特殊条款
    
    // 状态
    private String status;          // DRAFT(草稿), CONFIRMED(已确认), SIGNED(已签署)
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (contractDate == null) {
            contractDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "DRAFT";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
