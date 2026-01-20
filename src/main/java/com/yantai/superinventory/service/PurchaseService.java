package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Purchase;
import com.yantai.superinventory.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;

    @Autowired
    private InventoryService inventoryService;

    public PurchaseService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<Purchase> findAll() {
        return purchaseRepository.findAll();
    }

    @Transactional
    public Purchase save(Purchase purchase) {
        // 保存采购单
        Purchase savedPurchase = purchaseRepository.save(purchase);
        
        // 如果采购单状态为已完成，记录入库交易
        if (purchase.getStatus() == Purchase.PurchaseStatus.COMPLETED) {
            inventoryService.recordInboundTransaction(
                purchase.getProduct(),
                purchase.getQuantity(),
                purchase.getUnitPrice(),
                savedPurchase.getId(),
                purchase.getWarehouse()
            );
        }
        
        return savedPurchase;
    }

    /**
     * 完成采购入库
     */
    @Transactional
    public Purchase completePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new RuntimeException("Purchase not found"));
        
        if (purchase.getStatus() == Purchase.PurchaseStatus.COMPLETED) {
            throw new RuntimeException("Purchase already completed");
        }
        
        // 更新状态为已完成
        purchase.setStatus(Purchase.PurchaseStatus.COMPLETED);
        Purchase updatedPurchase = purchaseRepository.save(purchase);
        
        // 记录入库交易
        inventoryService.recordInboundTransaction(
            purchase.getProduct(),
            purchase.getQuantity(),
            purchase.getUnitPrice(),
            purchase.getId(),
            purchase.getWarehouse()
        );
        
        return updatedPurchase;
    }
    
    public void deleteById(Long id) {
        purchaseRepository.deleteById(id);
    }
}
