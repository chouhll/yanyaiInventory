package com.yantai.superinventory.service;

import com.yantai.superinventory.model.*;
import com.yantai.superinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 库存盘点服务
 * Inventory check/counting service
 */
@Service
public class InventoryCheckService {

    @Autowired
    private InventoryCheckRepository checkRepository;

    @Autowired
    private InventoryCheckItemRepository checkItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    /**
     * 获取所有盘点单
     */
    public List<InventoryCheck> getAllChecks() {
        return checkRepository.findAll();
    }

    /**
     * 根据ID获取盘点单
     */
    public InventoryCheck getCheckById(Long id) {
        return checkRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("盘点单不存在，ID: " + id));
    }

    /**
     * 根据状态获取盘点单
     */
    public List<InventoryCheck> getChecksByStatus(InventoryCheck.CheckStatus status) {
        return checkRepository.findByStatus(status);
    }

    /**
     * 获取有差异的盘点单
     */
    public List<InventoryCheck> getChecksWithDiscrepancies(InventoryCheck.CheckStatus status) {
        return checkRepository.findChecksWithDiscrepancies(status);
    }

    /**
     * 创建盘点单
     */
    @Transactional
    public InventoryCheck createCheck(InventoryCheck check) {
        // 验证仓库存在（如果提供）
        if (check.getWarehouse() != null && check.getWarehouse().getId() != null) {
            Warehouse warehouse = warehouseRepository.findById(check.getWarehouse().getId())
                .orElseThrow(() -> new RuntimeException("仓库不存在"));
            check.setWarehouse(warehouse);
        }

        // 生成盘点单号
        if (check.getCheckNumber() == null || check.getCheckNumber().isEmpty()) {
            check.setCheckNumber(generateCheckNumber());
        } else {
            // 检查单号唯一性
            if (checkRepository.existsByCheckNumber(check.getCheckNumber())) {
                throw new RuntimeException("盘点单号已存在: " + check.getCheckNumber());
            }
        }

        // 设置初始数据
        if (check.getCheckDate() == null) {
            check.setCheckDate(LocalDateTime.now());
        }

        check.setCreatedAt(LocalDateTime.now());
        check.setUpdatedAt(LocalDateTime.now());

        return checkRepository.save(check);
    }

    /**
     * 更新盘点单
     */
    @Transactional
    public InventoryCheck updateCheck(Long id, InventoryCheck check) {
        InventoryCheck existing = getCheckById(id);

        // 只允许更新草稿或进行中的盘点单
        if (existing.getStatus() == InventoryCheck.CheckStatus.COMPLETED ||
            existing.getStatus() == InventoryCheck.CheckStatus.APPROVED) {
            throw new RuntimeException("已完成或已审批的盘点单不能修改");
        }

        existing.setCheckType(check.getCheckType());
        existing.setChecker(check.getChecker());
        existing.setRemarks(check.getRemarks());
        existing.setUpdatedAt(LocalDateTime.now());

        return checkRepository.save(existing);
    }

    /**
     * 删除盘点单
     */
    @Transactional
    public void deleteCheck(Long id) {
        InventoryCheck check = getCheckById(id);

        // 只允许删除草稿状态的盘点单
        if (check.getStatus() != InventoryCheck.CheckStatus.DRAFT) {
            throw new RuntimeException("只能删除草稿状态的盘点单");
        }

        checkRepository.deleteById(id);
    }

    /**
     * 添加盘点明细
     */
    @Transactional
    public InventoryCheckItem addCheckItem(Long checkId, InventoryCheckItem item) {
        InventoryCheck check = getCheckById(checkId);

        // 只允许在草稿或进行中状态添加明细
        if (check.getStatus() == InventoryCheck.CheckStatus.COMPLETED ||
            check.getStatus() == InventoryCheck.CheckStatus.APPROVED) {
            throw new RuntimeException("已完成或已审批的盘点单不能添加明细");
        }

        // 验证产品存在
        Product product = productRepository.findById(item.getProduct().getId())
            .orElseThrow(() -> new RuntimeException("产品不存在"));
        item.setProduct(product);

        item.setInventoryCheck(check);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        // 差异会自动计算（通过@PrePersist）
        return checkItemRepository.save(item);
    }

    /**
     * 更新盘点明细
     */
    @Transactional
    public InventoryCheckItem updateCheckItem(Long itemId, InventoryCheckItem item) {
        InventoryCheckItem existing = checkItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("盘点明细不存在"));

        InventoryCheck check = existing.getInventoryCheck();
        if (check.getStatus() == InventoryCheck.CheckStatus.COMPLETED ||
            check.getStatus() == InventoryCheck.CheckStatus.APPROVED) {
            throw new RuntimeException("已完成或已审批的盘点单不能修改明细");
        }

        existing.setBookQuantity(item.getBookQuantity());
        existing.setActualQuantity(item.getActualQuantity());
        existing.setDiscrepancyReason(item.getDiscrepancyReason());
        existing.setProcessAction(item.getProcessAction());
        existing.setRemarks(item.getRemarks());
        existing.setUpdatedAt(LocalDateTime.now());

        // 差异会自动重新计算（通过@PreUpdate）
        return checkItemRepository.save(existing);
    }

    /**
     * 完成盘点（修改状态为已完成）
     */
    @Transactional
    public InventoryCheck completeCheck(Long id) {
        InventoryCheck check = getCheckById(id);

        if (check.getStatus() != InventoryCheck.CheckStatus.IN_PROGRESS) {
            throw new RuntimeException("只能完成进行中的盘点单");
        }

        // 检查是否所有明细都已录入实际数量
        List<InventoryCheckItem> items = checkItemRepository.findByInventoryCheck(check);
        if (items.isEmpty()) {
            throw new RuntimeException("盘点单没有明细");
        }

        for (InventoryCheckItem item : items) {
            if (item.getActualQuantity() == null) {
                throw new RuntimeException("存在未录入实际数量的明细");
            }
        }

        check.setStatus(InventoryCheck.CheckStatus.COMPLETED);
        check.setUpdatedAt(LocalDateTime.now());

        return checkRepository.save(check);
    }

    /**
     * 审批盘点单
     */
    @Transactional
    public InventoryCheck approveCheck(Long id, String approver) {
        InventoryCheck check = getCheckById(id);

        if (check.getStatus() != InventoryCheck.CheckStatus.COMPLETED) {
            throw new RuntimeException("只能审批已完成的盘点单");
        }

        check.setStatus(InventoryCheck.CheckStatus.APPROVED);
        check.setApprover(approver);
        check.setApprovalDate(LocalDateTime.now());
        check.setUpdatedAt(LocalDateTime.now());

        return checkRepository.save(check);
    }

    /**
     * 处理盘点差异（调整库存）
     */
    @Transactional
    public void processDiscrepancies(Long checkId) {
        InventoryCheck check = getCheckById(checkId);

        if (check.getStatus() != InventoryCheck.CheckStatus.APPROVED) {
            throw new RuntimeException("只能处理已审批的盘点单");
        }

        // 获取所有未处理的差异明细
        List<InventoryCheckItem> items = checkItemRepository.findUnprocessedDiscrepancies(check);

        for (InventoryCheckItem item : items) {
            if (item.getProcessAction() == InventoryCheckItem.ProcessAction.ADJUST) {
                // 调整库存
                Product product = item.getProduct();
                BigDecimal discrepancy = item.getDiscrepancyQuantity();

                // 更新产品库存
                Integer currentStock = product.getStock() != null ? product.getStock() : 0;
                int newStock = currentStock + discrepancy.intValue();
                
                if (newStock < 0) {
                    throw new RuntimeException("产品 " + product.getName() + " 调整后库存不能为负数");
                }

                product.setStock(newStock);
                productRepository.save(product);

                // 记录库存交易（盘盈或盘亏）
                if (discrepancy.compareTo(BigDecimal.ZERO) > 0) {
                    // 盘盈：入库
                    inventoryService.recordInboundTransaction(
                        product,
                        discrepancy,
                        item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO,
                        check.getCheckNumber(),
                        check.getWarehouse() != null ? check.getWarehouse().getName() : "默认仓库"
                    );
                } else if (discrepancy.compareTo(BigDecimal.ZERO) < 0) {
                    // 盘亏：出库
                    inventoryService.recordOutboundTransaction(
                        product,
                        discrepancy.abs(),
                        item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO,
                        check.getCheckNumber(),
                        check.getWarehouse() != null ? check.getWarehouse().getName() : "默认仓库"
                    );
                }

                // 标记明细为已处理
                item.setProcessed(true);
                item.setProcessedAt(LocalDateTime.now());
                checkItemRepository.save(item);
            }
        }
    }

    /**
     * 获取盘点明细
     */
    public List<InventoryCheckItem> getCheckItems(Long checkId) {
        InventoryCheck check = getCheckById(checkId);
        return checkItemRepository.findByInventoryCheck(check);
    }

    /**
     * 获取有差异的明细
     */
    public List<InventoryCheckItem> getDiscrepancyItems(Long checkId) {
        InventoryCheck check = getCheckById(checkId);
        return checkItemRepository.findDiscrepancyItems(check);
    }

    /**
     * 生成盘点单号（CHK-YYYYMMDD-XXXX）
     */
    private String generateCheckNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CHK-" + dateStr + "-";

        // 查找当天最大序号
        int maxSeq = 1;
        List<InventoryCheck> todayChecks = checkRepository.findAll().stream()
            .filter(c -> c.getCheckNumber().startsWith(prefix))
            .toList();

        for (InventoryCheck check : todayChecks) {
            try {
                String seqStr = check.getCheckNumber().substring(prefix.length());
                int seq = Integer.parseInt(seqStr);
                if (seq >= maxSeq) {
                    maxSeq = seq + 1;
                }
            } catch (Exception e) {
                // 忽略格式错误的单号
            }
        }

        return prefix + String.format("%04d", maxSeq);
    }

    /**
     * 开始盘点（修改状态为进行中）
     */
    @Transactional
    public InventoryCheck startCheck(Long id) {
        InventoryCheck check = getCheckById(id);

        if (check.getStatus() != InventoryCheck.CheckStatus.DRAFT) {
            throw new RuntimeException("只能开始草稿状态的盘点单");
        }

        check.setStatus(InventoryCheck.CheckStatus.IN_PROGRESS);
        check.setUpdatedAt(LocalDateTime.now());

        return checkRepository.save(check);
    }
}