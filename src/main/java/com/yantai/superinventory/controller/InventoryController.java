package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.InventoryPeriodBalance;
import com.yantai.superinventory.model.InventoryTransaction;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.repository.ProductRepository;
import com.yantai.superinventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 生成月度库存报表
     * GET /api/inventory/report/generate?period=2025-12
     */
    @GetMapping("/report/generate")
    public ResponseEntity<List<InventoryPeriodBalance>> generateMonthlyReport(
            @RequestParam String period) {
        try {
            YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
            List<InventoryPeriodBalance> report = inventoryService.generateMonthlyReport(yearMonth);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取月度库存报表
     * GET /api/inventory/report?period=2025-12
     */
    @GetMapping("/report")
    public ResponseEntity<List<InventoryPeriodBalance>> getMonthlyReport(
            @RequestParam String period) {
        List<InventoryPeriodBalance> report = inventoryService.getMonthlyReport(period);
        return ResponseEntity.ok(report);
    }

    /**
     * 获取产品的库存交易记录
     * GET /api/inventory/transactions/{productId}
     */
    @GetMapping("/transactions/{productId}")
    public ResponseEntity<List<InventoryTransaction>> getProductTransactions(
            @PathVariable Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        List<InventoryTransaction> transactions = inventoryService.getProductTransactions(product);
        return ResponseEntity.ok(transactions);
    }

    /**
     * 获取指定期间的所有交易
     * GET /api/inventory/transactions?startDate=2025-12-01T00:00:00&endDate=2025-12-31T23:59:59
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<InventoryTransaction>> getTransactionsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<InventoryTransaction> transactions = inventoryService.getTransactionsByPeriod(startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    /**
     * 获取所有库存报表期间
     * GET /api/inventory/report/periods
     */
    @GetMapping("/report/periods")
    public ResponseEntity<List<String>> getAllPeriods() {
        // 这里可以实现获取所有已生成报表的期间
        // 简化实现，返回最近12个月
        List<String> periods = new java.util.ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            periods.add(current.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        return ResponseEntity.ok(periods);
    }
}