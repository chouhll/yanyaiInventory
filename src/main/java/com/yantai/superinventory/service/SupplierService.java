package com.yantai.superinventory.service;

import com.yantai.superinventory.model.Supplier;
import com.yantai.superinventory.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 供应商业务逻辑层
 */
@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    /**
     * 获取所有供应商
     * @return 供应商列表
     */
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    /**
     * 获取所有启用的供应商
     * @return 启用的供应商列表
     */
    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findByActive(true);
    }

    /**
     * 根据ID获取供应商
     * @param id 供应商ID
     * @return 供应商
     */
    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    /**
     * 根据名称搜索供应商
     * @param name 供应商名称关键字
     * @return 供应商列表
     */
    public List<Supplier> searchSuppliersByName(String name) {
        return supplierRepository.findByNameContaining(name);
    }

    /**
     * 创建供应商
     * @param supplier 供应商信息
     * @return 创建的供应商
     */
    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        // 验证必填字段
        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("供应商名称不能为空");
        }

        // 检查税号是否已存在
        if (supplier.getTaxNumber() != null && !supplier.getTaxNumber().trim().isEmpty()) {
            Supplier existingSupplier = supplierRepository.findByTaxNumber(supplier.getTaxNumber());
            if (existingSupplier != null) {
                throw new IllegalArgumentException("该税号已存在");
            }
        }

        // 设置默认值
        if (supplier.getActive() == null) {
            supplier.setActive(true);
        }

        return supplierRepository.save(supplier);
    }

    /**
     * 更新供应商信息
     * @param id 供应商ID
     * @param supplierDetails 供应商详细信息
     * @return 更新后的供应商
     */
    @Transactional
    public Supplier updateSupplier(Long id, Supplier supplierDetails) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在: " + id));

        // 更新基本信息
        supplier.setName(supplierDetails.getName());
        supplier.setContactPerson(supplierDetails.getContactPerson());
        supplier.setPhone(supplierDetails.getPhone());
        supplier.setAddress(supplierDetails.getAddress());
        supplier.setEmail(supplierDetails.getEmail());

        // 更新开票信息
        supplier.setCompanyName(supplierDetails.getCompanyName());
        supplier.setTaxNumber(supplierDetails.getTaxNumber());
        supplier.setCompanyAddress(supplierDetails.getCompanyAddress());
        supplier.setBankName(supplierDetails.getBankName());
        supplier.setBankAccount(supplierDetails.getBankAccount());

        // 更新业务信息
        supplier.setCreditRating(supplierDetails.getCreditRating());
        supplier.setPaymentMethod(supplierDetails.getPaymentMethod());
        supplier.setPaymentTermDays(supplierDetails.getPaymentTermDays());
        supplier.setRemarks(supplierDetails.getRemarks());
        supplier.setActive(supplierDetails.getActive());

        return supplierRepository.save(supplier);
    }

    /**
     * 删除供应商
     * @param id 供应商ID
     */
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在: " + id));
        
        // 软删除：设置为不启用
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    /**
     * 启用/禁用供应商
     * @param id 供应商ID
     * @param active 是否启用
     * @return 更新后的供应商
     */
    @Transactional
    public Supplier toggleSupplierStatus(Long id, Boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在: " + id));
        
        supplier.setActive(active);
        return supplierRepository.save(supplier);
    }
}