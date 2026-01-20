package com.yantai.superinventory.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联订单
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;

    // 关联产品
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // 销售数量
    private BigDecimal quantity;

    // 销售单价
    private BigDecimal unitPrice;

    // 成本单价
    private BigDecimal costUnitPrice;

    // 小计金额
    private BigDecimal subtotal;

    // 成本小计
    private BigDecimal costSubtotal;

    public Product getProduct() {
        return product;
    }
}
