package com.yantai.superinventory.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

import lombok.Data;

@Data
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 库存编码
    @Column(unique = true, nullable = false)
    private String inventoryCode;

    // 存货名称
    @Column(nullable = false)
    private String name;

    // 规格型号
    private String specification;

    // 存货分类
    private String category;

    // 计量单位
    private String unit;

    // SKU
    private String sku;

    // 价格
    private BigDecimal price;

    // 当前库存数量
    private Integer stock;

    // 安全库存（最低库存预警线）
    private Integer safetyStock;

    // 最大库存
    private Integer maxStock;

    // 是否启用库存预警
    @Column(nullable = false)
    private Boolean alertEnabled = true;

    // 保质期（天数）- 用于过期预警
    private Integer shelfLifeDays;

    // 是否为滞销品
    @Column(nullable = false)
    private Boolean slowMoving = false;

    // 最后销售日期
    private java.time.LocalDate lastSaleDate;

    /**
     * 检查是否低库存
     */
    public boolean isLowStock() {
        if (safetyStock == null || stock == null) {
            return false;
        }
        return stock <= safetyStock;
    }

    /**
     * 检查是否超库存
     */
    public boolean isOverStock() {
        if (maxStock == null || stock == null) {
            return false;
        }
        return stock >= maxStock;
    }

    /**
     * 检查是否缺货
     */
    public boolean isOutOfStock() {
        return stock == null || stock <= 0;
    }
}
