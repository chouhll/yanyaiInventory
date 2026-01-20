package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Contract;
import com.yantai.superinventory.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {
    Optional<Contract> findByOrder(Order order);
    Optional<Contract> findByContractNumber(String contractNumber);
}
