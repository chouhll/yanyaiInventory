package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Purchase;
import com.yantai.superinventory.service.PurchaseService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<Purchase> getAll() {
        return purchaseService.findAll();
    }

    @PostMapping
    public Purchase create(@RequestBody Purchase purchase) {
        return purchaseService.save(purchase);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        purchaseService.deleteById(id);
    }
}
