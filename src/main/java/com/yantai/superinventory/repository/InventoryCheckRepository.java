package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.InventoryCheck;
import com.yantai.superinventory.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 库存盘点数据访问层
 * Inventory check repository for database operations
 */
@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
    
    /**
     * 根据盘点单号查找
     */
    Optional<InventoryCheck> findByCheckNumber(String checkNumber);
    
    /**
     * 根据仓库查找盘点记录
     */
    List<InventoryCheck> findByWarehouse(Warehouse warehouse);
    
    /**
     * 根据仓库ID查找盘点记录
     */
    List<InventoryCheck> findByWarehouseId(Long warehouseId);
    
    /**
     * 根据状态查找盘点记录
     */
    List<InventoryCheck> findByStatus(InventoryCheck.CheckStatus status);
    
    /**
     * 根据盘点类型查找
     */
    List<InventoryCheck> findByCheckType(InventoryCheck.CheckType checkType);
    
    /**
     * 根据盘点人查找
     */
    List<InventoryCheck> findByChecker(String checker);
    
    /**
     * 根据日期范围查找盘点记录
     */
    List<InventoryCheck> findByCheckDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 查找待审批的盘点单
     */
    List<InventoryCheck> findByStatusOrderByCheckDateDesc(InventoryCheck.CheckStatus status);
    
    /**
     * 查找最近的盘点记录
     */
    @Query("SELECT ic FROM InventoryCheck ic ORDER BY ic.checkDate DESC")
    List<InventoryCheck> findRecentChecks();
    
    /**
     * 根据仓库和状态查找
     */
    List<InventoryCheck> findByWarehouseAndStatus(Warehouse warehouse, InventoryCheck.CheckStatus status);
    
    /**
     * 检查盘点单号是否存在
     */
    boolean existsByCheckNumber(String checkNumber);
    
    /**
     * 查找有差异的盘点单
     */
    @Query("SELECT DISTINCT ic FROM InventoryCheck ic JOIN ic.items item " +
           "WHERE item.discrepancyQuantity <> 0 AND ic.status = :status")
    List<InventoryCheck> findChecksWithDiscrepancies(@Param("status") InventoryCheck.CheckStatus status);
}