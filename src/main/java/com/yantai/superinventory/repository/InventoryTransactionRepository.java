package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.InventoryTransaction;
import com.yantai.superinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    // 根据产品查询交易记录
    List<InventoryTransaction> findByProduct(Product product);
    
    // 根据产品和交易类型查询
    List<InventoryTransaction> findByProductAndType(Product product, InventoryTransaction.TransactionType type);
    
    // 根据日期范围查询
    List<InventoryTransaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 根据产品和日期范围查询
    List<InventoryTransaction> findByProductAndTransactionDateBetween(
        Product product, LocalDateTime startDate, LocalDateTime endDate);
    
    // 查询特定产品在特定日期范围的入库记录
    @Query("SELECT t FROM InventoryTransaction t WHERE t.product = :product " +
           "AND t.type = 'INBOUND' AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findInboundTransactions(
        @Param("product") Product product,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // 查询特定产品在特定日期范围的出库记录
    @Query("SELECT t FROM InventoryTransaction t WHERE t.product = :product " +
           "AND t.type = 'OUTBOUND' AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findOutboundTransactions(
        @Param("product") Product product,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
}