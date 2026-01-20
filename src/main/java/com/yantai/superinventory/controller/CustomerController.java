package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Customer;
import com.yantai.superinventory.service.CustomerService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/customers")
public class CustomerController {
    private final CustomerService customerService;

     public CustomerController(CustomerService customerService) {
         this.customerService = customerService;
     }

     @GetMapping
     public List<Customer> getAll() {
         return customerService.findAll();
     }

     @PostMapping
     public Customer create(@RequestBody Customer customer) {
         return customerService.save(customer);
     }
     
     @PutMapping("/{id}")
     public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
         customer.setId(id);
         return customerService.save(customer);
     }
     
     @DeleteMapping("/{id}")
     public void delete(@PathVariable Long id) {
         customerService.deleteById(id);
     }
}
