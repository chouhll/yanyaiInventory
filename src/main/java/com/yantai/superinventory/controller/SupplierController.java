package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Supplier;
import com.yantai.superinventory.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商管理控制器
 */
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    /**
     * 获取所有供应商
     * @param activeOnly 是否只获取启用的供应商
     * @return 供应商列表
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        List<Supplier> suppliers;
        if (activeOnly) {
            suppliers = supplierService.getActiveSuppliers();
        } else {
            suppliers = supplierService.getAllSuppliers();
        }
        return ResponseEntity.ok(suppliers);
    }

    /**
     * 根据ID获取供应商
     * @param id 供应商ID
     * @return 供应商详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSupplierById(@PathVariable Long id) {
        try {
            return supplierService.getSupplierById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取供应商失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 搜索供应商
     * @param name 供应商名称关键字
     * @return 供应商列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<Supplier>> searchSuppliers(@RequestParam String name) {
        List<Supplier> suppliers = supplierService.searchSuppliersByName(name);
        return ResponseEntity.ok(suppliers);
    }

    /**
     * 创建供应商
     * @param supplier 供应商信息
     * @return 创建的供应商
     */
    @PostMapping
    public ResponseEntity<?> createSupplier(@RequestBody Supplier supplier) {
        try {
            Supplier createdSupplier = supplierService.createSupplier(supplier);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSupplier);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "创建供应商失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新供应商
     * @param id 供应商ID
     * @param supplierDetails 供应商详细信息
     * @return 更新后的供应商
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplier(
            @PathVariable Long id,
            @RequestBody Supplier supplierDetails) {
        try {
            Supplier updatedSupplier = supplierService.updateSupplier(id, supplierDetails);
            return ResponseEntity.ok(updatedSupplier);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "更新供应商失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除供应商（软删除）
     * @param id 供应商ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "供应商已删除");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "删除供应商失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 切换供应商启用状态
     * @param id 供应商ID
     * @param request 包含active字段的请求体
     * @return 更新后的供应商
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleSupplierStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean active = request.get("active");
            if (active == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "缺少active参数");
                return ResponseEntity.badRequest().body(error);
            }
            
            Supplier updatedSupplier = supplierService.toggleSupplierStatus(id, active);
            return ResponseEntity.ok(updatedSupplier);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "更新状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}