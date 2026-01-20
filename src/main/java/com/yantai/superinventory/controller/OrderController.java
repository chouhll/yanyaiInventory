package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Order;
import com.yantai.superinventory.model.OrderStatus;
import com.yantai.superinventory.service.OrderService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable String id) {
        return orderService.findById(id);
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        return orderService.save(order);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable String id, @RequestBody Order order) {
        order.setId(id);
        return orderService.update(order);
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id, @RequestParam OrderStatus status) {
        return orderService.updateStatus(id, status);
    }
    
    /**
     * 订单出库（发货）
     * POST /api/orders/{id}/ship
     * 验证库存并扣减库存，更新订单状态为已发货
     */
    @PostMapping("/{id}/ship")
    public Order shipOrder(@PathVariable String id) {
        return orderService.shipOrder(id);
    }
    
    /**
     * 检查订单库存是否充足
     * GET /api/orders/{id}/check-stock
     * 返回库存是否充足的状态
     */
    @GetMapping("/{id}/check-stock")
    public boolean checkStockAvailability(@PathVariable String id) {
        return orderService.checkStockAvailability(id);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        orderService.deleteById(id);
    }
}
