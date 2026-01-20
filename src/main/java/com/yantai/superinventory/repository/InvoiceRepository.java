package com.yantai.superinventory.repository;

import com.yantai.superinventory.model.Invoice;
import com.yantai.superinventory.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrder(Order order);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}