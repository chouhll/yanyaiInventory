package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.InventoryBatch;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 库存批次数据访问层
 * Inventory batch repository for database operations
 */
@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    
    /**
     * 根据批次号查找
     */
    Optional<InventoryBatch> findByBatchNumber(String batchNumber);
    
    /**
     * 根据产品查找所有批次
     */
    List<InventoryBatch> findByProduct(Product product);
    
    /**
     * 根据产品ID查找所有批次
     */
    List<InventoryBatch> findByProductId(Long productId);
    
    /**
     * 根据仓库查找所有批次
     */
    List<InventoryBatch> findByWarehouse(Warehouse warehouse);
    
    /**
     * 根据状态查找批次
     */
    List<InventoryBatch> findByStatus(InventoryBatch.BatchStatus status);
    
    /**
     * 查找可用批次（FIFO - 按入库日期排序）
     */
    @Query("SELECT b FROM InventoryBatch b WHERE b.product = :product " +
           "AND b.status = 'AVAILABLE' AND b.remainingQuantity > 0 " +
           "ORDER BY b.inboundDate ASC")
    List<InventoryBatch> findAvailableBatchesByProductFIFO(@Param("product") Product product);
    
    /**
     * 查找即将过期的批次（30天内）
     */
    @Query("SELECT b FROM InventoryBatch b WHERE b.expirationDate IS NOT NULL " +
           "AND b.expirationDate BETWEEN :startDate AND :endDate " +
           "AND b.status = 'AVAILABLE' AND b.remainingQuantity > 0")
    List<InventoryBatch> findExpiringSoonBatches(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * 查找已过期的批次
     */
    @Query("SELECT b FROM InventoryBatch b WHERE b.expirationDate IS NOT NULL " +
           "AND b.expirationDate < :today AND b.remainingQuantity > 0")
    List<InventoryBatch> findExpiredBatches(@Param("today") LocalDate today);
    
    /**
     * 根据产品和仓库查找可用批次
     */
    @Query("SELECT b FROM InventoryBatch b WHERE b.product = :product " +
           "AND b.warehouse = :warehouse AND b.status = 'AVAILABLE' " +
           "AND b.remainingQuantity > 0 ORDER BY b.inboundDate ASC")
    List<InventoryBatch> findAvailableBatchesByProductAndWarehouse(
        @Param("product") Product product, 
        @Param("warehouse") Warehouse warehouse);
    
    /**
     * 计算产品的总可用批次数量
     */
    @Query("SELECT COALESCE(SUM(b.remainingQuantity), 0) FROM InventoryBatch b " +
           "WHERE b.product = :product AND b.status = 'AVAILABLE'")
    BigDecimal getTotalAvailableQuantityByProduct(@Param("product") Product product);
    
    /**
     * 检查批次号是否存在
     */
    boolean existsByBatchNumber(String batchNumber);
}