package com.yantai.superinventory.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id; // 改为String类型存储UUID

    @Column(unique = true, nullable = false)
    private String orderNumber; // 订单号

    private LocalDateTime orderDate;

    @ManyToOne
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Invoice invoice;  // 发票信息

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.orderNumber == null) {
            // 生成订单号：格式 ORD-YYYYMMDD-XXXX
            String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            this.orderNumber = "ORD-" + datePart + "-" + randomPart;
        }
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}