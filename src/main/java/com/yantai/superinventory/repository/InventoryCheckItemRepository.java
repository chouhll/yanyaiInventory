package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.InventoryCheck;
import com.yantai.superinventory.model.InventoryCheckItem;
import com.yantai.superinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 库存盘点明细数据访问层
 * Inventory check item repository for database operations
 */
@Repository
public interface InventoryCheckItemRepository extends JpaRepository<InventoryCheckItem, Long> {
    
    /**
     * 根据盘点单查找所有明细
     */
    List<InventoryCheckItem> findByInventoryCheck(InventoryCheck inventoryCheck);
    
    /**
     * 根据盘点单ID查找所有明细
     */
    List<InventoryCheckItem> findByInventoryCheckId(Long inventoryCheckId);
    
    /**
     * 根据产品查找盘点明细
     */
    List<InventoryCheckItem> findByProduct(Product product);
    
    /**
     * 查找有差异的盘点明细
     */
    @Query("SELECT item FROM InventoryCheckItem item " +
           "WHERE item.inventoryCheck = :check AND item.discrepancyQuantity <> 0")
    List<InventoryCheckItem> findDiscrepancyItems(@Param("check") InventoryCheck check);
    
    /**
     * 查找盘盈的明细
     */
    @Query("SELECT item FROM InventoryCheckItem item " +
           "WHERE item.inventoryCheck = :check AND item.discrepancyQuantity > 0")
    List<InventoryCheckItem> findSurplusItems(@Param("check") InventoryCheck check);
    
    /**
     * 查找盘亏的明细
     */
    @Query("SELECT item FROM InventoryCheckItem item " +
           "WHERE item.inventoryCheck = :check AND item.discrepancyQuantity < 0")
    List<InventoryCheckItem> findShortageItems(@Param("check") InventoryCheck check);
    
    /**
     * 查找未处理的差异明细
     */
    @Query("SELECT item FROM InventoryCheckItem item " +
           "WHERE item.inventoryCheck = :check AND item.discrepancyQuantity <> 0 " +
           "AND item.processed = false")
    List<InventoryCheckItem> findUnprocessedDiscrepancies(@Param("check") InventoryCheck check);
    
    /**
     * 查找已处理的差异明细
     */
    @Query("SELECT item FROM InventoryCheckItem item " +
           "WHERE item.inventoryCheck = :check AND item.processed = true")
    List<InventoryCheckItem> findProcessedItems(@Param("check") InventoryCheck check);
}