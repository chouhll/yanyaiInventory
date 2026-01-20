package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;
    private String address;
    
    // 开票信息
    private String taxNumber;        // 税号
    private String companyName;      // 公司抬头
    private String companyAddress;   // 公司地址
    private String bankName;         // 开户银行
    private String bankAccount;      // 银行账号

}
