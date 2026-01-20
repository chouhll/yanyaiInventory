package com.yantai.superinventory.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 仓库实体
 * Warehouse entity for multi-warehouse management
 */
@Data
@Entity
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 仓库编码
    @Column(unique = true, nullable = false)
    private String code;

    // 仓库名称
    @Column(nullable = false)
    private String name;

    // 仓库地址
    private String address;

    // 仓库类型（主仓、分仓、临时仓等）
    private String type;

    // 负责人
    private String manager;

    // 联系电话
    private String phone;

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

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
