package com.yantai.superinventory.service;

import com.yantai.superinventory.model.InventoryBatch;
import com.yantai.superinventory.model.Product;
import com.yantai.superinventory.model.Warehouse;
import com.yantai.superinventory.repository.InventoryBatchRepository;
import com.yantai.superinventory.repository.ProductRepository;
import com.yantai.superinventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 批次管理服务
 * Inventory batch management service with FIFO support
 */
@Service
public class InventoryBatchService {

    @Autowired
    private InventoryBatchRepository batchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    /**
     * 获取所有批次
     */
    public List<InventoryBatch> getAllBatches() {
        return batchRepository.findAll();
    }

    /**
     * 根据ID获取批次
     */
    public InventoryBatch getBatchById(Long id) {
        return batchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("批次不存在，ID: " + id));
    }

    /**
     * 根据产品获取批次
     */
    public List<InventoryBatch> getBatchesByProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("产品不存在"));
        return batchRepository.findByProduct(product);
    }

    /**
     * 获取可用批次（FIFO排序）
     */
    public List<InventoryBatch> getAvailableBatchesByProductFIFO(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("产品不存在"));
        return batchRepository.findAvailableBatchesByProductFIFO(product);
    }

    /**
     * 获取即将过期的批次
     */
    public List<InventoryBatch> getExpiringSoonBatches() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        return batchRepository.findExpiringSoonBatches(today, thirtyDaysLater);
    }

    /**
     * 获取已过期的批次
     */
    public List<InventoryBatch> getExpiredBatches() {
        return batchRepository.findExpiredBatches(LocalDate.now());
    }

    /**
     * 创建批次
     */
    @Transactional
    public InventoryBatch createBatch(InventoryBatch batch) {
        // 验证产品存在
        Product product = productRepository.findById(batch.getProduct().getId())
            .orElseThrow(() -> new RuntimeException("产品不存在"));
        batch.setProduct(product);

        // 验证仓库存在（如果提供）
        if (batch.getWarehouse() != null && batch.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(batch.getWarehouse().getId())
                .orElseThrow(() -> new RuntimeException("仓库不存在"));
            batch.setWarehouse(warehouse);
        }

        // 生成批次号（如果未提供）
        if (batch.getBatchNumber() == null || batch.getBatchNumber().isEmpty()) {
            batch.setBatchNumber(generateBatchNumber());
        } else {
            // 检查批次号唯一性
            if (batchRepository.existsByBatchNumber(batch.getBatchNumber())) {
                throw new RuntimeException("批次号已存在: " + batch.getBatchNumber());
            }
        }

        // 设置初始数据
        if (batch.getInboundDate() == null) {
            batch.setInboundDate(LocalDate.now());
        }
        
        if (batch.getRemainingQuantity() == null) {
            batch.setRemainingQuantity(batch.getInitialQuantity());
        }

        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    /**
     * 更新批次
     */
    @Transactional
    public InventoryBatch updateBatch(Long id, InventoryBatch batch) {
        InventoryBatch existing = getBatchById(id);

        // 检查批次号唯一性（如果批次号被修改）
        if (!existing.getBatchNumber().equals(batch.getBatchNumber()) && 
            batchRepository.existsByBatchNumber(batch.getBatchNumber())) {
            throw new RuntimeException("批次号已存在: " + batch.getBatchNumber());
        }

        existing.setBatchNumber(batch.getBatchNumber());
        existing.setProductionDate(batch.getProductionDate());
        existing.setExpirationDate(batch.getExpirationDate());
        existing.setUnitCost(batch.getUnitCost());
        existing.setStatus(batch.getStatus());
        existing.setRemarks(batch.getRemarks());
        existing.setUpdatedAt(LocalDateTime.now());

        return batchRepository.save(existing);
    }

    /**
     * 删除批次
     */
    @Transactional
    public void deleteBatch(Long id) {
        InventoryBatch batch = getBatchById(id);
        
        // 检查批次是否还有剩余数量
        if (batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("批次还有剩余数量，无法删除");
        }

        batchRepository.deleteById(id);
    }

    /**
     * FIFO出库（从最早批次扣除库存）
     */
    @Transactional
    public void deductFromBatchesFIFO(Long productId, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("出库数量必须大于0");
        }

        List<InventoryBatch> batches = getAvailableBatchesByProductFIFO(productId);
        
        BigDecimal remainingQty = quantity;
        
        for (InventoryBatch batch : batches) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // 检查批次是否过期
            if (batch.isExpired()) {
                batch.setStatus(InventoryBatch.BatchStatus.EXPIRED);
                batchRepository.save(batch);
                continue;
            }

            BigDecimal batchRemaining = batch.getRemainingQuantity();
            
            if (batchRemaining.compareTo(remainingQty) >= 0) {
                // 当前批次足够
                batch.setRemainingQuantity(batchRemaining.subtract(remainingQty));
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前批次不够，全部扣除
                batch.setRemainingQuantity(BigDecimal.ZERO);
                remainingQty = remainingQty.subtract(batchRemaining);
            }

            // 更新批次状态
            if (batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                batch.setStatus(InventoryBatch.BatchStatus.DEPLETED);
            }

            batch.setUpdatedAt(LocalDateTime.now());
            batchRepository.save(batch);
        }

        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("批次库存不足，还需要: " + remainingQty);
        }
    }

    /**
     * 生成批次号（BATCH-YYYYMMDD-XXXX）
     */
    private String generateBatchNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "BATCH-" + dateStr + "-";
        
        // 查找当天最大序号
        int maxSeq = 1;
        List<InventoryBatch> todayBatches = batchRepository.findAll().stream()
            .filter(b -> b.getBatchNumber().startsWith(prefix))
            .toList();
        
        for (InventoryBatch batch : todayBatches) {
            try {
                String seqStr = batch.getBatchNumber().substring(prefix.length());
                int seq = Integer.parseInt(seqStr);
                if (seq >= maxSeq) {
                    maxSeq = seq + 1;
                }
            } catch (Exception e) {
                // 忽略格式错误的批次号
            }
        }
        
        return prefix + String.format("%04d", maxSeq);
    }

    /**
     * 获取产品的总可用批次数量
     */
    public BigDecimal getTotalAvailableQuantity(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("产品不存在"));
        return batchRepository.getTotalAvailableQuantityByProduct(product);
    }

    /**
     * 更新过期批次状态
     */
    @Transactional
    public int updateExpiredBatchStatus() {
        List<InventoryBatch> expiredBatches = getExpiredBatches();
        int count = 0;
        
        for (InventoryBatch batch : expiredBatches) {
            if (batch.getStatus() != InventoryBatch.BatchStatus.EXPIRED) {
                batch.setStatus(InventoryBatch.BatchStatus.EXPIRED);
                batch.setUpdatedAt(LocalDateTime.now());
                batchRepository.save(batch);
                count++;
            }
        }
        
        return count;
    }
}