package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Reconciliation;
import com.yantai.superinventory.model.Reconciliation.ReconciliationType;
import com.yantai.superinventory.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 对账控制器
 * 提供供应商和客户对账的REST API
 */
@RestController
@RequestMapping("/api/reconciliations")
@CrossOrigin(origins = "*")
public class ReconciliationController {

    @Autowired
    private ReconciliationService reconciliationService;

    /**
     * 创建供应商对账单
     */
    @PostMapping("/supplier")
    public ResponseEntity<Reconciliation> createSupplierReconciliation(
            @RequestParam Long supplierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        Reconciliation reconciliation = reconciliationService.createSupplierReconciliation(
            supplierId, periodStart, periodEnd);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 创建客户对账单
     */
    @PostMapping("/customer")
    public ResponseEntity<Reconciliation> createCustomerReconciliation(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        Reconciliation reconciliation = reconciliationService.createCustomerReconciliation(
            customerId, periodStart, periodEnd);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 获取所有对账单
     */
    @GetMapping
    public ResponseEntity<List<Reconciliation>> getAllReconciliations() {
        List<Reconciliation> reconciliations = reconciliationService.getAllReconciliations();
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * 根据类型获取对账单
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Reconciliation>> getReconciliationsByType(
            @PathVariable ReconciliationType type) {
        List<Reconciliation> reconciliations = reconciliationService.getReconciliationsByType(type);
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * 获取供应商的对账单列表
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<Reconciliation>> getSupplierReconciliations(
            @PathVariable Long supplierId) {
        List<Reconciliation> reconciliations = reconciliationService.getSupplierReconciliations(supplierId);
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * 获取客户的对账单列表
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Reconciliation>> getCustomerReconciliations(
            @PathVariable Long customerId) {
        List<Reconciliation> reconciliations = reconciliationService.getCustomerReconciliations(customerId);
        return ResponseEntity.ok(reconciliations);
    }

    /**
     * 获取对账单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reconciliation> getReconciliationById(@PathVariable Long id) {
        Reconciliation reconciliation = reconciliationService.getReconciliationById(id);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 更新对账单
     */
    @PutMapping("/{id}")
    public ResponseEntity<Reconciliation> updateReconciliation(
            @PathVariable Long id,
            @RequestBody Reconciliation updates) {
        Reconciliation reconciliation = reconciliationService.updateReconciliation(id, updates);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 提交对账单
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<Reconciliation> submitReconciliation(@PathVariable Long id) {
        Reconciliation reconciliation = reconciliationService.submitReconciliation(id);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 确认对账单
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Reconciliation> confirmReconciliation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String confirmedBy = request.get("confirmedBy");
        Reconciliation reconciliation = reconciliationService.confirmReconciliation(id, confirmedBy);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 标记对账单为有异议
     */
    @PostMapping("/{id}/dispute")
    public ResponseEntity<Reconciliation> disputeReconciliation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String disputeRemark = request.get("disputeRemark");
        Reconciliation reconciliation = reconciliationService.disputeReconciliation(id, disputeRemark);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 解决对账差异
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Reconciliation> resolveReconciliation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String resolutionRemark = request.get("resolutionRemark");
        Reconciliation reconciliation = reconciliationService.resolveReconciliation(id, resolutionRemark);
        return ResponseEntity.ok(reconciliation);
    }

    /**
     * 删除对账单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReconciliation(@PathVariable Long id) {
        reconciliationService.deleteReconciliation(id);
        return ResponseEntity.ok().build();
    }
}