package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Order;
import com.yantai.superinventory.model.OrderStatus;
import com.yantai.superinventory.model.OrderItem;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.repository.OrderRepository;
import com.yantai.superinventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(String id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("订单不存在: " + id));
    }

    @Transactional
    public Order save(Order order) {
        if (order.getItems() == null || ((List<?>)order.getItems()).isEmpty()) {
            throw new RuntimeException("订单项不能为空");
        }
        
        // 保存订单
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        items.forEach(item -> {
            Product product = productRepository.findById(((Product) item.getProduct()).getId())
                .orElseThrow(() -> new RuntimeException("商品不存在: " + ((Product) item.getProduct()).getId()));
            
            // 检查库存
            if (product.getStock() < item.getQuantity().intValue()) {
                throw new RuntimeException("库存不足: " + product.getName());
            }
            
            item.setOrder(order); // 确保订单项关联订单
        });
        
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);
        
        // 处理出库交易和库存扣减
        items.forEach(item -> {
            Product product = item.getProduct();
            
            // 记录出库交易（使用产品的当前价格作为成本单价）
            inventoryService.recordOutboundTransaction(
                product,
                item.getQuantity(),
                item.getCostUnitPrice() != null ? item.getCostUnitPrice() : product.getPrice(),
                savedOrder.getId(),
                null // 可以添加仓库信息
            );
        });
        
        return savedOrder;
    }

    @Transactional
    public Order update(Order order) {
        Order existingOrder = findById(order.getId());
        
        // 更新订单项
        if (order.getItems() != null) {
            List<OrderItem> items = (List<OrderItem>) order.getItems();
            items.forEach(item -> item.setOrder(order));
            existingOrder.setItems(items);
        }
        
        // 更新其他字段
        if (order.getCustomer() != null) {
            existingOrder.setCustomer(order.getCustomer());
        }
        if (order.getStatus() != null) {
            existingOrder.setStatus(order.getStatus());
        }
        
        return orderRepository.save(existingOrder);
    }

    @Transactional
    public Order updateStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    public void deleteById(String id) {
        orderRepository.deleteById(id);
    }
}
