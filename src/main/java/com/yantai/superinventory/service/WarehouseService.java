package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Warehouse;
import com.yantai.superinventory.model.WarehouseLocation;
import com.yantai.superinventory.repository.WarehouseLocationRepository;
import com.yantai.superinventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓库管理服务
 * Warehouse management service
 */
@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository locationRepository;

    /**
     * 获取所有仓库
     */
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    /**
     * 获取所有启用的仓库
     */
    public List<Warehouse> getEnabledWarehouses() {
        return warehouseRepository.findByEnabledTrue();
    }

    /**
     * 根据ID获取仓库
     */
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("仓库不存在，ID: " + id));
    }

    /**
     * 根据编码获取仓库
     */
    public Warehouse getWarehouseByCode(String code) {
        return warehouseRepository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("仓库不存在，编码: " + code));
    }

    /**
     * 创建仓库
     */
    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse) {
        // 检查编码唯一性
        if (warehouseRepository.existsByCode(warehouse.getCode())) {
            throw new RuntimeException("仓库编码已存在: " + warehouse.getCode());
        }
        
        warehouse.setCreatedAt(LocalDateTime.now());
        warehouse.setUpdatedAt(LocalDateTime.now());
        return warehouseRepository.save(warehouse);
    }

    /**
     * 更新仓库
     */
    @Transactional
    public Warehouse updateWarehouse(Long id, Warehouse warehouse) {
        Warehouse existing = getWarehouseById(id);
        
        // 检查编码唯一性（如果编码被修改）
        if (!existing.getCode().equals(warehouse.getCode()) && 
            warehouseRepository.existsByCode(warehouse.getCode())) {
            throw new RuntimeException("仓库编码已存在: " + warehouse.getCode());
        }
        
        existing.setCode(warehouse.getCode());
        existing.setName(warehouse.getName());
        existing.setAddress(warehouse.getAddress());
        existing.setType(warehouse.getType());
        existing.setManager(warehouse.getManager());
        existing.setPhone(warehouse.getPhone());
        existing.setEnabled(warehouse.getEnabled());
        existing.setRemarks(warehouse.getRemarks());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return warehouseRepository.save(existing);
    }

    /**
     * 删除仓库
     */
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getWarehouseById(id);
        
        // 检查是否有关联的库位
        List<WarehouseLocation> locations = locationRepository.findByWarehouse(warehouse);
        if (!locations.isEmpty()) {
            throw new RuntimeException("仓库下存在库位，无法删除");
        }
        
        warehouseRepository.deleteById(id);
    }

    /**
     * 启用/禁用仓库
     */
    @Transactional
    public Warehouse toggleWarehouseStatus(Long id) {
        Warehouse warehouse = getWarehouseById(id);
        warehouse.setEnabled(!warehouse.getEnabled());
        warehouse.setUpdatedAt(LocalDateTime.now());
        return warehouseRepository.save(warehouse);
    }

    // ========== 库位管理 ==========

    /**
     * 获取所有库位
     */
    public List<WarehouseLocation> getAllLocations() {
        return locationRepository.findAll();
    }

    /**
     * 根据仓库获取库位
     */
    public List<WarehouseLocation> getLocationsByWarehouse(Long warehouseId) {
        return locationRepository.findByWarehouseId(warehouseId);
    }

    /**
     * 根据ID获取库位
     */
    public WarehouseLocation getLocationById(Long id) {
        return locationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("库位不存在，ID: " + id));
    }

    /**
     * 创建库位
     */
    @Transactional
    public WarehouseLocation createLocation(WarehouseLocation location) {
        // 检查编码唯一性
        if (locationRepository.existsByCode(location.getCode())) {
            throw new RuntimeException("库位编码已存在: " + location.getCode());
        }
        
        // 验证仓库存在
        Warehouse warehouse = warehouseRepository.findById(location.getWarehouse().getId())
            .orElseThrow(() -> new RuntimeException("仓库不存在"));
        location.setWarehouse(warehouse);
        
        location.setCreatedAt(LocalDateTime.now());
        location.setUpdatedAt(LocalDateTime.now());
        return locationRepository.save(location);
    }

    /**
     * 更新库位
     */
    @Transactional
    public WarehouseLocation updateLocation(Long id, WarehouseLocation location) {
        WarehouseLocation existing = getLocationById(id);
        
        // 检查编码唯一性（如果编码被修改）
        if (!existing.getCode().equals(location.getCode()) && 
            locationRepository.existsByCode(location.getCode())) {
            throw new RuntimeException("库位编码已存在: " + location.getCode());
        }
        
        existing.setCode(location.getCode());
        existing.setName(location.getName());
        existing.setZone(location.getZone());
        existing.setRackNumber(location.getRackNumber());
        existing.setLevel(location.getLevel());
        existing.setType(location.getType());
        existing.setCapacity(location.getCapacity());
        existing.setEnabled(location.getEnabled());
        existing.setRemarks(location.getRemarks());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return locationRepository.save(existing);
    }

    /**
     * 删除库位
     */
    @Transactional
    public void deleteLocation(Long id) {
        WarehouseLocation location = getLocationById(id);
        
        // 检查库位是否被占用
        if (location.getOccupied() != null && location.getOccupied() > 0) {
            throw new RuntimeException("库位已被占用，无法删除");
        }
        
        locationRepository.deleteById(id);
    }

    /**
     * 更新库位占用量
     */
    @Transactional
    public WarehouseLocation updateLocationOccupancy(Long locationId, Integer quantity, boolean isInbound) {
        WarehouseLocation location = getLocationById(locationId);
        
        Integer currentOccupied = location.getOccupied() != null ? location.getOccupied() : 0;
        Integer newOccupied = isInbound ? currentOccupied + quantity : currentOccupied - quantity;
        
        // 验证容量
        if (location.getCapacity() != null && newOccupied > location.getCapacity()) {
            throw new RuntimeException("库位容量不足");
        }
        
        if (newOccupied < 0) {
            throw new RuntimeException("库位占用量不能为负数");
        }
        
        location.setOccupied(newOccupied);
        location.setUpdatedAt(LocalDateTime.now());
        return locationRepository.save(location);
    }
}