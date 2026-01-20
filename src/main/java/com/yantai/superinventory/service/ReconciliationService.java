package com.yantai.superinventory.service;

import com.yantai.superinventory.model.*;
import com.yantai.superinventory.model.Reconciliation.ReconciliationType;
import com.yantai.superinventory.model.Reconciliation.ReconciliationStatus;
import com.yantai.superinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对账服务
 * 处理供应商和客户对账逻辑
 */
@Service
public class ReconciliationService {

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * 创建供应商对账单
     */
    @Transactional
    public Reconciliation createSupplierReconciliation(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("供应商不存在"));

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setType(ReconciliationType.SUPPLIER);
        reconciliation.setSupplier(supplier);
        reconciliation.setPeriodStart(periodStart);
        reconciliation.setPeriodEnd(periodEnd);
        reconciliation.setReconciliationDate(LocalDate.now());
        reconciliation.setStatus(ReconciliationStatus.DRAFT);

        // 计算期间采购金额
        BigDecimal purchaseAmount = calculateSupplierPurchaseAmount(supplierId, periodStart, periodEnd);
        reconciliation.setOurAmount(purchaseAmount);

        // 对方金额初始为我方金额（后续可由用户修改）
        reconciliation.setTheirAmount(purchaseAmount);
        reconciliation.setDifferenceAmount(BigDecimal.ZERO);

        // 生成对账明细
        String details = generateSupplierReconciliationDetails(supplierId, periodStart, periodEnd);
        reconciliation.setDetails(details);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 创建客户对账单
     */
    @Transactional
    public Reconciliation createCustomerReconciliation(Long customerId, LocalDate periodStart, LocalDate periodEnd) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("客户不存在"));

        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setType(ReconciliationType.CUSTOMER);
        reconciliation.setCustomer(customer);
        reconciliation.setPeriodStart(periodStart);
        reconciliation.setPeriodEnd(periodEnd);
        reconciliation.setReconciliationDate(LocalDate.now());
        reconciliation.setStatus(ReconciliationStatus.DRAFT);

        // 计算期间销售金额
        BigDecimal salesAmount = calculateCustomerSalesAmount(customerId, periodStart, periodEnd);
        reconciliation.setOurAmount(salesAmount);

        // 对方金额初始为我方金额（后续可由用户修改）
        reconciliation.setTheirAmount(salesAmount);
        reconciliation.setDifferenceAmount(BigDecimal.ZERO);

        // 生成对账明细
        String details = generateCustomerReconciliationDetails(customerId, periodStart, periodEnd);
        reconciliation.setDetails(details);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 更新对账单
     */
    @Transactional
    public Reconciliation updateReconciliation(Long id, Reconciliation updates) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        if (updates.getTheirAmount() != null) {
            reconciliation.setTheirAmount(updates.getTheirAmount());
            // 重新计算差异
            BigDecimal difference = reconciliation.getOurAmount().subtract(reconciliation.getTheirAmount());
            reconciliation.setDifferenceAmount(difference);
        }

        if (updates.getDifferenceRemark() != null) {
            reconciliation.setDifferenceRemark(updates.getDifferenceRemark());
        }

        if (updates.getRemark() != null) {
            reconciliation.setRemark(updates.getRemark());
        }

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 提交对账单
     */
    @Transactional
    public Reconciliation submitReconciliation(Long id) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        if (reconciliation.getStatus() != ReconciliationStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的对账单可以提交");
        }

        reconciliation.setStatus(ReconciliationStatus.SUBMITTED);
        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 确认对账单
     */
    @Transactional
    public Reconciliation confirmReconciliation(Long id, String confirmedBy) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        if (reconciliation.getStatus() != ReconciliationStatus.SUBMITTED) {
            throw new RuntimeException("只有已提交的对账单可以确认");
        }

        reconciliation.setStatus(ReconciliationStatus.CONFIRMED);
        reconciliation.setConfirmedDate(LocalDate.now());
        reconciliation.setConfirmedBy(confirmedBy);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 标记对账单为有异议
     */
    @Transactional
    public Reconciliation disputeReconciliation(Long id, String disputeRemark) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        reconciliation.setStatus(ReconciliationStatus.DISPUTED);
        reconciliation.setDifferenceRemark(disputeRemark);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 解决对账差异
     */
    @Transactional
    public Reconciliation resolveReconciliation(Long id, String resolutionRemark) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        if (reconciliation.getStatus() != ReconciliationStatus.DISPUTED) {
            throw new RuntimeException("只有有异议的对账单可以解决");
        }

        reconciliation.setStatus(ReconciliationStatus.RESOLVED);
        reconciliation.setRemark(resolutionRemark);

        return reconciliationRepository.save(reconciliation);
    }

    /**
     * 获取所有对账单
     */
    public List<Reconciliation> getAllReconciliations() {
        return reconciliationRepository.findAll();
    }

    /**
     * 根据类型获取对账单
     */
    public List<Reconciliation> getReconciliationsByType(ReconciliationType type) {
        return reconciliationRepository.findByType(type);
    }

    /**
     * 根据供应商获取对账单
     */
    public List<Reconciliation> getSupplierReconciliations(Long supplierId) {
        return reconciliationRepository.findBySupplierId(supplierId);
    }

    /**
     * 根据客户获取对账单
     */
    public List<Reconciliation> getCustomerReconciliations(Long customerId) {
        return reconciliationRepository.findByCustomerId(customerId);
    }

    /**
     * 获取对账单详情
     */
    public Reconciliation getReconciliationById(Long id) {
        return reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));
    }

    /**
     * 删除对账单
     */
    @Transactional
    public void deleteReconciliation(Long id) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("对账单不存在"));

        if (reconciliation.getStatus() == ReconciliationStatus.CONFIRMED) {
            throw new RuntimeException("已确认的对账单不能删除");
        }

        reconciliationRepository.deleteById(id);
    }

    // 私有辅助方法

    /**
     * 计算供应商采购金额
     */
    private BigDecimal calculateSupplierPurchaseAmount(Long supplierId, LocalDate start, LocalDate end) {
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getSupplier() != null && p.getSupplier().getId().equals(supplierId))
            .filter(p -> p.getPurchaseDate() != null)
            .filter(p -> !p.getPurchaseDate().toLocalDate().isBefore(start) && 
                        !p.getPurchaseDate().toLocalDate().isAfter(end))
            .collect(Collectors.toList());

        return purchases.stream()
            .map(Purchase::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算客户销售金额
     */
    private BigDecimal calculateCustomerSalesAmount(Long customerId, LocalDate start, LocalDate end) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId))
            .filter(o -> !o.getOrderDate().toLocalDate().isBefore(start) && 
                        !o.getOrderDate().toLocalDate().isAfter(end))
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED || 
                        o.getStatus() == OrderStatus.PAID || 
                        o.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    total = total.add(item.getSubtotal());
                }
            }
        }
        return total;
    }

    /**
     * 生成供应商对账明细
     */
    private String generateSupplierReconciliationDetails(Long supplierId, LocalDate start, LocalDate end) {
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getSupplier() != null && p.getSupplier().getId().equals(supplierId))
            .filter(p -> p.getPurchaseDate() != null)
            .filter(p -> !p.getPurchaseDate().toLocalDate().isBefore(start) && 
                        !p.getPurchaseDate().toLocalDate().isAfter(end))
            .collect(Collectors.toList());

        StringBuilder details = new StringBuilder();
        details.append("采购明细:\n");
        for (Purchase purchase : purchases) {
            details.append(String.format("采购单号: %s, 日期: %s, 金额: %s\n",
                purchase.getId(),
                purchase.getPurchaseDate().toLocalDate(),
                purchase.getTotalAmount()));
        }

        return details.toString();
    }

    /**
     * 生成客户对账明细
     */
    private String generateCustomerReconciliationDetails(Long customerId, LocalDate start, LocalDate end) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId))
            .filter(o -> !o.getOrderDate().toLocalDate().isBefore(start) && 
                        !o.getOrderDate().toLocalDate().isAfter(end))
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED || 
                        o.getStatus() == OrderStatus.PAID || 
                        o.getStatus() == OrderStatus.INVOICED)
            .collect(Collectors.toList());

        StringBuilder details = new StringBuilder();
        details.append("销售明细:\n");
        for (Order order : orders) {
            BigDecimal orderTotal = BigDecimal.ZERO;
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    orderTotal = orderTotal.add(item.getSubtotal());
                }
            }
            details.append(String.format("订单号: %s, 日期: %s, 金额: %s\n",
                order.getOrderNumber(),
                order.getOrderDate().toLocalDate(),
                orderTotal));
        }

        return details.toString();
    }
}