package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 供应商数据访问层
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    /**
     * 根据供应商名称查找
     * @param name 供应商名称
     * @return 供应商列表
     */
    List<Supplier> findByNameContaining(String name);
    
    /**
     * 查找所有启用的供应商
     * @param active 是否启用
     * @return 供应商列表
     */
    List<Supplier> findByActive(Boolean active);
    
    /**
     * 根据税号查找供应商
     * @param taxNumber 税号
     * @return 供应商
     */
    Supplier findByTaxNumber(String taxNumber);
}