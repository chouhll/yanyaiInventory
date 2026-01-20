package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 供应商实体
 * 管理供应商基本信息和开票信息
 */
@Data
@Entity
@Table(name = "supplier")
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 供应商名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 联系人
     */
    @Column(length = 50)
    private String contactPerson;

    /**
     * 联系电话
     */
    @Column(length = 20)
    private String phone;

    /**
     * 联系地址
     */
    @Column(length = 200)
    private String address;

    /**
     * 电子邮箱
     */
    @Column(length = 100)
    private String email;

    /**
     * 公司名称（用于开票）
     */
    @Column(length = 200)
    private String companyName;

    /**
     * 纳税人识别号
     */
    @Column(length = 50)
    private String taxNumber;

    /**
     * 公司地址（用于开票）
     */
    @Column(length = 200)
    private String companyAddress;

    /**
     * 开户银行
     */
    @Column(length = 100)
    private String bankName;

    /**
     * 银行账号
     */
    @Column(length = 50)
    private String bankAccount;

    /**
     * 信用等级: A(优秀), B(良好), C(一般), D(较差)
     */
    @Column(length = 10)
    private String creditRating;

    /**
     * 结算方式: CASH(现金), TRANSFER(转账), CHECK(支票), CREDIT(赊账)
     */
    @Column(length = 20)
    private String paymentMethod;

    /**
     * 账期（天数）
     */
    private Integer paymentTermDays;

    /**
     * 备注
     */
    @Column(length = 500)
    private String remarks;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean active = true;
}