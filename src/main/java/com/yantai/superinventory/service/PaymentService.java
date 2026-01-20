package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Order;
import com.yantai.superinventory.model.Payment;
import com.yantai.superinventory.model.Payment.PaymentType;
import com.yantai.superinventory.model.Purchase;
import com.yantai.superinventory.repository.OrderRepository;
import com.yantai.superinventory.repository.PaymentRepository;
import com.yantai.superinventory.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 收付款业务逻辑层
 */
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    /**
     * 获取所有付款记录
     * @return 付款记录列表
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * 根据类型获取付款记录
     * @param type 付款类型
     * @return 付款记录列表
     */
    public List<Payment> getPaymentsByType(PaymentType type) {
        return paymentRepository.findByType(type);
    }

    /**
     * 根据ID获取付款记录
     * @param id 付款ID
     * @return 付款记录
     */
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * 根据订单ID获取收款记录
     * @param orderId 订单ID
     * @return 收款记录列表
     */
    public List<Payment> getPaymentsByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * 根据采购单ID获取付款记录
     * @param purchaseId 采购单ID
     * @return 付款记录列表
     */
    public List<Payment> getPaymentsByPurchaseId(Long purchaseId) {
        return paymentRepository.findByPurchaseId(purchaseId);
    }

    /**
     * 获取日期范围内的付款记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 付款记录列表
     */
    public List<Payment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByDateRange(startDate, endDate);
    }

    /**
     * 获取日期范围内指定类型的付款记录
     * @param type 付款类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 付款记录列表
     */
    public List<Payment> getPaymentsByTypeAndDateRange(PaymentType type, LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByTypeAndDateRange(type, startDate, endDate);
    }

    /**
     * 创建收款记录（订单收款）
     * @param orderId 订单ID
     * @param payment 收款信息
     * @return 创建的收款记录
     */
    @Transactional
    public Payment createReceivablePayment(String orderId, Payment payment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderId));

        // 验证金额
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("收款金额必须大于0");
        }

        // 设置付款类型和关联订单
        payment.setType(PaymentType.RECEIVABLE);
        payment.setOrder(order);
        payment.setPurchase(null);

        // 设置默认值
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }

    /**
     * 创建付款记录（采购付款）
     * @param purchaseId 采购单ID
     * @param payment 付款信息
     * @return 创建的付款记录
     */
    @Transactional
    public Payment createPayablePayment(Long purchaseId, Payment payment) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("采购单不存在: " + purchaseId));

        // 验证金额
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        // 设置付款类型和关联采购单
        payment.setType(PaymentType.PAYABLE);
        payment.setPurchase(purchase);
        payment.setOrder(null);

        // 设置默认值
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }

    /**
     * 更新付款记录
     * @param id 付款ID
     * @param paymentDetails 付款详细信息
     * @return 更新后的付款记录
     */
    @Transactional
    public Payment updatePayment(Long id, Payment paymentDetails) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("付款记录不存在: " + id));

        // 更新可修改的字段
        payment.setAmount(paymentDetails.getAmount());
        payment.setMethod(paymentDetails.getMethod());
        payment.setPaymentDate(paymentDetails.getPaymentDate());
        payment.setTransactionNumber(paymentDetails.getTransactionNumber());
        payment.setAccountFrom(paymentDetails.getAccountFrom());
        payment.setAccountTo(paymentDetails.getAccountTo());
        payment.setRemarks(paymentDetails.getRemarks());
        payment.setStatus(paymentDetails.getStatus());

        return paymentRepository.save(payment);
    }

    /**
     * 取消付款记录
     * @param id 付款ID
     * @return 更新后的付款记录
     */
    @Transactional
    public Payment cancelPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("付款记录不存在: " + id));

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        return paymentRepository.save(payment);
    }

    /**
     * 删除付款记录
     * @param id 付款ID
     */
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("付款记录不存在: " + id));
        
        paymentRepository.delete(payment);
    }

    /**
     * 计算订单的总收款金额
     * @param orderId 订单ID
     * @return 总收款金额
     */
    public BigDecimal getTotalReceivedAmount(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算采购单的总付款金额
     * @param purchaseId 采购单ID
     * @return 总付款金额
     */
    public BigDecimal getTotalPaidAmount(Long purchaseId) {
        List<Payment> payments = paymentRepository.findByPurchaseId(purchaseId);
        return payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}