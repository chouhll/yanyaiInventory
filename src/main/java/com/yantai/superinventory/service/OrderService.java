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
        
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        boolean hasStockShortage = false;
        StringBuilder stockMessage = new StringBuilder();
        
        // 检查所有商品的库存
        for (OrderItem item : items) {
            Product product = productRepository.findById(((Product) item.getProduct()).getId())
                .orElseThrow(() -> new RuntimeException("商品不存在: " + ((Product) item.getProduct()).getId()));
            
            Integer currentStock = product.getStock() != null ? product.getStock() : 0;
            int requiredQty = item.getQuantity().intValue();
            
            // 检查库存是否充足
            if (currentStock < requiredQty) {
                hasStockShortage = true;
                stockMessage.append(String.format("商品[%s]库存不足，当前库存：%d，需求数量：%d；",
                    product.getName(), currentStock, requiredQty));
            }
            
            item.setOrder(order); // 确保订单项关联订单
        }
        
        // 创建订单时设置为合同拟定中状态
        // 不再立即检查库存，等待合同确认和付款后再进行后续流程
        order.setStatus(OrderStatus.CONTRACT_DRAFT);
        Order savedOrder = orderRepository.save(order);
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
    
    /**
     * 订单出库（发货）
     * 验证库存并扣减库存
     */
    @Transactional
    public Order shipOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        // 检查订单状态，只有PAID或INVOICED状态才能出库
        if (order.getStatus() != OrderStatus.PAID && 
            order.getStatus() != OrderStatus.INVOICED &&
            order.getStatus() != OrderStatus.PENDING_STOCK) {
            throw new RuntimeException("订单状态不允许出库，当前状态：" + order.getStatus());
        }
        
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        StringBuilder stockMessage = new StringBuilder();
        boolean hasStockShortage = false;
        
        // 再次检查库存是否充足
        for (OrderItem item : items) {
            Product product = productRepository.findById(((Product) item.getProduct()).getId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            Integer currentStock = product.getStock() != null ? product.getStock() : 0;
            int requiredQty = item.getQuantity().intValue();
            
            if (currentStock < requiredQty) {
                hasStockShortage = true;
                stockMessage.append(String.format("商品[%s]库存不足，当前库存：%d，需求数量：%d；",
                    product.getName(), currentStock, requiredQty));
            }
        }
        
        // 如果库存不足，不允许出库
        if (hasStockShortage) {
            // 更新订单状态为待备货
            order.setStatus(OrderStatus.PENDING_STOCK);
            orderRepository.save(order);
            throw new RuntimeException("库存不足，无法出库。" + stockMessage.toString());
        }
        
        // 库存充足，执行出库操作
        items.forEach(item -> {
            Product product = productRepository.findById(((Product) item.getProduct()).getId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            // 记录出库交易并扣减库存
            inventoryService.recordOutboundTransaction(
                product,
                item.getQuantity(),
                item.getCostUnitPrice() != null ? item.getCostUnitPrice() : product.getPrice(),
                order.getId(),
                null // 可以添加仓库信息
            );
        });
        
        // 更新订单状态为已发货
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }
    
    /**
     * 检查订单库存是否充足
     */
    public boolean checkStockAvailability(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        
        for (OrderItem item : items) {
            Product product = productRepository.findById(((Product) item.getProduct()).getId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            Integer currentStock = product.getStock() != null ? product.getStock() : 0;
            int requiredQty = item.getQuantity().intValue();
            
            if (currentStock < requiredQty) {
                return false; // 有任何商品库存不足
            }
        }
        
        return true; // 所有商品库存充足
    }
    
    public void deleteById(String id) {
        orderRepository.deleteById(id);
    }
}
