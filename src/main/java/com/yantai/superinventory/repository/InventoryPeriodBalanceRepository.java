package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.InventoryPeriodBalance;
import com.yantai.superinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryPeriodBalanceRepository extends JpaRepository<InventoryPeriodBalance, Long> {
    
    // 根据产品和期间查询余额
    Optional<InventoryPeriodBalance> findByProductAndPeriod(Product product, String period);
    
    // 根据期间查询所有产品的余额
    List<InventoryPeriodBalance> findByPeriod(String period);
    
    // 根据产品查询所有期间的余额
    List<InventoryPeriodBalance> findByProduct(Product product);
    
    // 根据期间排序查询所有余额
    List<InventoryPeriodBalance> findByPeriodOrderByProductInventoryCodeAsc(String period);
    
    // 根据仓库和期间查询
    List<InventoryPeriodBalance> findByWarehouseAndPeriod(String warehouse, String period);
}