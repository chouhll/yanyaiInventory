package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}