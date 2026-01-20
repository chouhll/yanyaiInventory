package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Customer;
import com.yantai.superinventory.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }
    
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }
}
