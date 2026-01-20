package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓库数据访问层
 * Warehouse repository for database operations
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    /**
     * 根据仓库编码查找
     */
    Optional<Warehouse> findByCode(String code);
    
    /**
     * 查找所有启用的仓库
     */
    List<Warehouse> findByEnabledTrue();
    
    /**
     * 根据仓库类型查找
     */
    List<Warehouse> findByType(String type);
    
    /**
     * 根据负责人查找
     */
    List<Warehouse> findByManager(String manager);
    
    /**
     * 检查仓库编码是否存在
     */
    boolean existsByCode(String code);
}