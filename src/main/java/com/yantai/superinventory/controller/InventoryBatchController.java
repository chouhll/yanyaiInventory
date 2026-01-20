package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.InventoryBatch;
import com.yantai.superinventory.service.InventoryBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批次管理控制器
 * Inventory batch management REST controller
 */
@RestController
@RequestMapping("/api/batches")
@CrossOrigin(origins = "*")
public class InventoryBatchController {

    @Autowired
    private InventoryBatchService batchService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBatches() {
        try {
            List<InventoryBatch> batches = batchService.getAllBatches();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batches);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBatchById(@PathVariable Long id) {
        try {
            InventoryBatch batch = batchService.getBatchById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batch);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getBatchesByProduct(@PathVariable Long productId) {
        try {
            List<InventoryBatch> batches = batchService.getBatchesByProduct(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batches);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/product/{productId}/fifo")
    public ResponseEntity<Map<String, Object>> getAvailableBatchesFIFO(@PathVariable Long productId) {
        try {
            List<InventoryBatch> batches = batchService.getAvailableBatchesByProductFIFO(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batches);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<Map<String, Object>> getExpiringSoonBatches() {
        try {
            List<InventoryBatch> batches = batchService.getExpiringSoonBatches();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batches);
            response.put("message", "即将过期批次（30天内）");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/expired")
    public ResponseEntity<Map<String, Object>> getExpiredBatches() {
        try {
            List<InventoryBatch> batches = batchService.getExpiredBatches();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batches);
            response.put("message", "已过期批次");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBatch(@RequestBody InventoryBatch batch) {
        try {
            InventoryBatch created = batchService.createBatch(batch);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "批次创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建批次失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBatch(@PathVariable Long id, @RequestBody InventoryBatch batch) {
        try {
            InventoryBatch updated = batchService.updateBatch(id, batch);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "批次更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新批次失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBatch(@PathVariable Long id) {
        try {
            batchService.deleteBatch(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批次删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除批次失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/product/{productId}/deduct-fifo")
    public ResponseEntity<Map<String, Object>> deductFromBatchesFIFO(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
            batchService.deductFromBatchesFIFO(productId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "FIFO出库成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "FIFO出库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/product/{productId}/available-quantity")
    public ResponseEntity<Map<String, Object>> getTotalAvailableQuantity(@PathVariable Long productId) {
        try {
            BigDecimal quantity = batchService.getTotalAvailableQuantity(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", quantity);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update-expired-status")
    public ResponseEntity<Map<String, Object>> updateExpiredBatchStatus() {
        try {
            int count = batchService.updateExpiredBatchStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", count);
            response.put("message", "更新了 " + count + " 个过期批次的状态");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}