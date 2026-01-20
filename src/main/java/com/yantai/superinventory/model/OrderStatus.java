package com.yantai.superinventory.model;

public enum OrderStatus {
    CREATED,        // 已创建/下单
    UNPAID,         // 未付款
    PAID,           // 付款完成
    INVOICED,       // 已开票
    SHIPPED,        // 已发货
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}