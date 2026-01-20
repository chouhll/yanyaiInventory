package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
}