package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Warehouse;
import com.yantai.superinventory.model.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 库位数据访问层
 * Warehouse location repository for database operations
 */
@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    
    /**
     * 根据库位编码查找
     */
    Optional<WarehouseLocation> findByCode(String code);
    
    /**
     * 根据仓库查找所有库位
     */
    List<WarehouseLocation> findByWarehouse(Warehouse warehouse);
    
    /**
     * 根据仓库ID查找所有库位
     */
    List<WarehouseLocation> findByWarehouseId(Long warehouseId);
    
    /**
     * 查找所有启用的库位
     */
    List<WarehouseLocation> findByEnabledTrue();
    
    /**
     * 根据仓库和区域查找
     */
    List<WarehouseLocation> findByWarehouseAndZone(Warehouse warehouse, String zone);
    
    /**
     * 根据库位类型查找
     */
    List<WarehouseLocation> findByType(String type);
    
    /**
     * 检查库位编码是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 查找有剩余容量的库位
     */
    @Query("SELECT wl FROM WarehouseLocation wl WHERE wl.warehouse = :warehouse " +
           "AND wl.enabled = true AND wl.occupied < wl.capacity")
    List<WarehouseLocation> findAvailableLocationsByWarehouse(@Param("warehouse") Warehouse warehouse);
}