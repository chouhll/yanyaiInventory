package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库位实体
 * Warehouse location entity for detailed inventory location management
 */
@Data
@Entity
public class WarehouseLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 库位编码（如：A-01-01）
    @Column(unique = true, nullable = false)
    private String code;

    // 库位名称
    @Column(nullable = false)
    private String name;

    // 所属仓库
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // 区域（如：A区、B区）
    private String zone;

    // 货架号
    private String rackNumber;

    // 层号
    private String level;

    // 库位类型（普通、冷藏、危险品等）
    private String type;

    // 容量限制
    private Integer capacity;

    // 当前占用量
    private Integer occupied = 0;

    // 是否启用
    @Column(nullable = false)
    private Boolean enabled = true;

    // 备注
    @Column(length = 500)
    private String remarks;

    // 创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 更新时间
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}