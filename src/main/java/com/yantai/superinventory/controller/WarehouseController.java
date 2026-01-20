package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Warehouse;
import com.yantai.superinventory.model.WarehouseLocation;
import com.yantai.superinventory.service.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库管理控制器
 * Warehouse management REST controller
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    /**
     * 获取所有仓库
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllWarehouses() {
        try {
            List<Warehouse> warehouses = warehouseService.getAllWarehouses();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouses);
            response.put("message", "获取仓库列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取仓库列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取所有启用的仓库
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledWarehouses() {
        try {
            List<Warehouse> warehouses = warehouseService.getEnabledWarehouses();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据ID获取仓库
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWarehouseById(@PathVariable Long id) {
        try {
            Warehouse warehouse = warehouseService.getWarehouseById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 创建仓库
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWarehouse(@RequestBody Warehouse warehouse) {
        try {
            Warehouse created = warehouseService.createWarehouse(warehouse);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "仓库创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建仓库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新仓库
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        try {
            Warehouse updated = warehouseService.updateWarehouse(id, warehouse);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "仓库更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新仓库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "仓库删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除仓库失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 切换仓库状态
     */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleWarehouseStatus(@PathVariable Long id) {
        try {
            Warehouse warehouse = warehouseService.toggleWarehouseStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouse);
            response.put("message", "仓库状态更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========== 库位管理API ==========

    /**
     * 获取所有库位
     */
    @GetMapping("/locations")
    public ResponseEntity<Map<String, Object>> getAllLocations() {
        try {
            List<WarehouseLocation> locations = warehouseService.getAllLocations();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", locations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据仓库ID获取库位
     */
    @GetMapping("/{warehouseId}/locations")
    public ResponseEntity<Map<String, Object>> getLocationsByWarehouse(@PathVariable Long warehouseId) {
        try {
            List<WarehouseLocation> locations = warehouseService.getLocationsByWarehouse(warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", locations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 创建库位
     */
    @PostMapping("/locations")
    public ResponseEntity<Map<String, Object>> createLocation(@RequestBody WarehouseLocation location) {
        try {
            WarehouseLocation created = warehouseService.createLocation(location);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "库位创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建库位失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新库位
     */
    @PutMapping("/locations/{id}")
    public ResponseEntity<Map<String, Object>> updateLocation(@PathVariable Long id, @RequestBody WarehouseLocation location) {
        try {
            WarehouseLocation updated = warehouseService.updateLocation(id, location);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "库位更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新库位失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除库位
     */
    @DeleteMapping("/locations/{id}")
    public ResponseEntity<Map<String, Object>> deleteLocation(@PathVariable Long id) {
        try {
            warehouseService.deleteLocation(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "库位删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除库位失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}