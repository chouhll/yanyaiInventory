package com.yantai.superinventory.controller;

import com.yantai.superinventory.service.InventoryAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存预警控制器
 * Inventory alert REST controller
 */
@RestController
@RequestMapping("/api/inventory/alerts")
@CrossOrigin(origins = "*")
public class InventoryAlertController {

    @Autowired
    private InventoryAlertService alertService;

    /**
     * 获取所有预警
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getAllAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("message", "获取预警列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取预警列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取预警统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        try {
            Map<String, Integer> stats = alertService.getAlertStatistics();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 按严重程度获取预警
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<Map<String, Object>> getAlertsBySeverity(@PathVariable String severity) {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getAlertsBySeverity(severity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 按产品获取预警
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getAlertsByProduct(@PathVariable Long productId) {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getAlertsByProduct(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取低库存预警
     */
    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getLowStockAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取缺货预警
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<Map<String, Object>> getOutOfStockAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getOutOfStockAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取即将过期预警
     */
    @GetMapping("/expiring-soon")
    public ResponseEntity<Map<String, Object>> getExpiringSoonAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getExpiringSoonAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取已过期预警
     */
    @GetMapping("/expired")
    public ResponseEntity<Map<String, Object>> getExpiredAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getExpiredAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取滞销品预警
     */
    @GetMapping("/slow-moving")
    public ResponseEntity<Map<String, Object>> getSlowMovingAlerts() {
        try {
            List<InventoryAlertService.InventoryAlert> alerts = alertService.getSlowMovingAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}