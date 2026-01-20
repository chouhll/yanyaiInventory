package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Reconciliation;
import com.yantai.superinventory.model.Reconciliation.ReconciliationType;
import com.yantai.superinventory.model.Reconciliation.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 对账单数据访问接口
 */
@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {
    
    /**
     * 根据对账单号查询
     */
    Optional<Reconciliation> findByReconciliationNumber(String reconciliationNumber);
    
    /**
     * 根据类型查询对账单
     */
    List<Reconciliation> findByType(ReconciliationType type);
    
    /**
     * 根据状态查询对账单
     */
    List<Reconciliation> findByStatus(ReconciliationStatus status);
    
    /**
     * 根据供应商ID查询对账单
     */
    List<Reconciliation> findBySupplierId(Long supplierId);
    
    /**
     * 根据客户ID查询对账单
     */
    List<Reconciliation> findByCustomerId(Long customerId);
}