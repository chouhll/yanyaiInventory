package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.InventoryCheck;
import com.yantai.superinventory.model.InventoryCheckItem;
import com.yantai.superinventory.service.InventoryCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存盘点控制器
 * Inventory check/counting REST controller
 */
@RestController
@RequestMapping("/api/inventory-checks")
@CrossOrigin(origins = "*")
public class InventoryCheckController {

    @Autowired
    private InventoryCheckService checkService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllChecks() {
        try {
            List<InventoryCheck> checks = checkService.getAllChecks();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", checks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCheckById(@PathVariable Long id) {
        try {
            InventoryCheck check = checkService.getCheckById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", check);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getChecksByStatus(@PathVariable String status) {
        try {
            InventoryCheck.CheckStatus checkStatus = InventoryCheck.CheckStatus.valueOf(status);
            List<InventoryCheck> checks = checkService.getChecksByStatus(checkStatus);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", checks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCheck(@RequestBody InventoryCheck check) {
        try {
            InventoryCheck created = checkService.createCheck(check);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "盘点单创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建盘点单失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCheck(@PathVariable Long id, @RequestBody InventoryCheck check) {
        try {
            InventoryCheck updated = checkService.updateCheck(id, check);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "盘点单更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新盘点单失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCheck(@PathVariable Long id) {
        try {
            checkService.deleteCheck(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "盘点单删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除盘点单失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startCheck(@PathVariable Long id) {
        try {
            InventoryCheck check = checkService.startCheck(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", check);
            response.put("message", "盘点已开始");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeCheck(@PathVariable Long id) {
        try {
            InventoryCheck check = checkService.completeCheck(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", check);
            response.put("message", "盘点完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCheck(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String approver = request.get("approver");
            InventoryCheck check = checkService.approveCheck(id, approver);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", check);
            response.put("message", "盘点单审批成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "审批盘点单失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/process-discrepancies")
    public ResponseEntity<Map<String, Object>> processDiscrepancies(@PathVariable Long id) {
        try {
            checkService.processDiscrepancies(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "差异处理成功，库存已调整");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "处理差异失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{checkId}/items")
    public ResponseEntity<Map<String, Object>> getCheckItems(@PathVariable Long checkId) {
        try {
            List<InventoryCheckItem> items = checkService.getCheckItems(checkId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", items);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{checkId}/discrepancies")
    public ResponseEntity<Map<String, Object>> getDiscrepancyItems(@PathVariable Long checkId) {
        try {
            List<InventoryCheckItem> items = checkService.getDiscrepancyItems(checkId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", items);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{checkId}/items")
    public ResponseEntity<Map<String, Object>> addCheckItem(@PathVariable Long checkId, @RequestBody InventoryCheckItem item) {
        try {
            InventoryCheckItem created = checkService.addCheckItem(checkId, item);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "盘点明细添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "添加明细失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> updateCheckItem(@PathVariable Long itemId, @RequestBody InventoryCheckItem item) {
        try {
            InventoryCheckItem updated = checkService.updateCheckItem(itemId, item);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "盘点明细更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新明细失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}