package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Payment;
import com.yantai.superinventory.model.Payment.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收付款数据访问层
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 根据类型查找付款记录
     * @param type 付款类型
     * @return 付款记录列表
     */
    List<Payment> findByType(PaymentType type);
    
    /**
     * 根据订单ID查找收款记录
     * @param orderId 订单ID
     * @return 收款记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    List<Payment> findByOrderId(@Param("orderId") String orderId);
    
    /**
     * 根据采购单ID查找付款记录
     * @param purchaseId 采购单ID
     * @return 付款记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.purchase.id = :purchaseId")
    List<Payment> findByPurchaseId(@Param("purchaseId") Long purchaseId);
    
    /**
     * 查找日期范围内的付款记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 付款记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * 查找日期范围内指定类型的付款记录
     * @param type 付款类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 付款记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.type = :type AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByTypeAndDateRange(@Param("type") PaymentType type,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}