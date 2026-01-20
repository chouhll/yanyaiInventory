package com.yantai.superinventory.service;

import com.yantai.superinventory.model.*;
import com.yantai.superinventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private InventoryPeriodBalanceRepository balanceRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 记录入库交易（采购）
     */
    @Transactional
    public InventoryTransaction recordInboundTransaction(
            Product product, 
            BigDecimal quantity, 
            BigDecimal unitPrice, 
            Object referenceId, 
            String warehouse) {
        
        // 从数据库重新加载完整的product对象
        Product fullProduct = productRepository.findById(product.getId())
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(fullProduct);
        transaction.setType(InventoryTransaction.TransactionType.INBOUND);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(unitPrice);
        transaction.setAmount(quantity.multiply(unitPrice));
        transaction.setReferenceId(referenceId != null ? referenceId.toString() : null);
        transaction.setWarehouse(warehouse);
        
        // 更新产品库存（处理null值）
        Integer currentStock = fullProduct.getStock();
        if (currentStock == null) {
            currentStock = 0;
        }
        fullProduct.setStock(currentStock + quantity.intValue());
        productRepository.save(fullProduct);
        
        return transactionRepository.save(transaction);
    }

    /**
     * 记录出库交易（销售）
     */
    @Transactional
    public InventoryTransaction recordOutboundTransaction(
            Product product, 
            BigDecimal quantity, 
            BigDecimal costUnitPrice, 
            Object referenceId, 
            String warehouse) {
        
        // 从数据库重新加载完整的product对象
        Product fullProduct = productRepository.findById(product.getId())
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(fullProduct);
        transaction.setType(InventoryTransaction.TransactionType.OUTBOUND);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(fullProduct.getPrice()); // 销售价
        transaction.setAmount(quantity.multiply(fullProduct.getPrice()));
        transaction.setCostUnitPrice(costUnitPrice);
        transaction.setCostAmount(quantity.multiply(costUnitPrice));
        transaction.setReferenceId(referenceId != null ? referenceId.toString() : null);
        transaction.setWarehouse(warehouse);
        
        // 更新产品库存（处理null值）
        Integer currentStock = fullProduct.getStock();
        if (currentStock == null) {
            currentStock = 0;
        }
        fullProduct.setStock(currentStock - quantity.intValue());
        productRepository.save(fullProduct);
        
        return transactionRepository.save(transaction);
    }

    /**
     * 生成月度库存报表
     */
    @Transactional
    public List<InventoryPeriodBalance> generateMonthlyReport(YearMonth period) {
        String periodStr = period.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 获取所有产品
        List<Product> products = productRepository.findAll();
        
        for (Product product : products) {
            generateProductPeriodBalance(product, periodStr);
        }
        
        return balanceRepository.findByPeriod(periodStr);
    }

    /**
     * 为单个产品生成期间余额
     */
    private void generateProductPeriodBalance(Product product, String period) {
        // 解析期间
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        // 检查是否已存在该期间的余额记录
        InventoryPeriodBalance balance = balanceRepository
            .findByProductAndPeriod(product, period)
            .orElse(new InventoryPeriodBalance());
        
        balance.setProduct(product);
        balance.setPeriod(period);
        
        // 计算期初余额（从上期结存或初始值）
        YearMonth previousMonth = yearMonth.minusMonths(1);
        String previousPeriod = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        InventoryPeriodBalance previousBalance = balanceRepository
            .findByProductAndPeriod(product, previousPeriod)
            .orElse(null);
        
        if (previousBalance != null) {
            balance.setBeginningQuantity(previousBalance.getEndingQuantity());
            balance.setBeginningUnitPrice(previousBalance.getEndingUnitPrice());
            balance.setBeginningAmount(previousBalance.getEndingAmount());
        } else {
            balance.setBeginningQuantity(BigDecimal.ZERO);
            balance.setBeginningUnitPrice(BigDecimal.ZERO);
            balance.setBeginningAmount(BigDecimal.ZERO);
        }
        
        // 计算入库
        List<InventoryTransaction> inboundTransactions = transactionRepository
            .findInboundTransactions(product, startDate, endDate);
        
        BigDecimal inboundQty = BigDecimal.ZERO;
        BigDecimal inboundAmt = BigDecimal.ZERO;
        
        for (InventoryTransaction trans : inboundTransactions) {
            inboundQty = inboundQty.add(trans.getQuantity());
            inboundAmt = inboundAmt.add(trans.getAmount());
        }
        
        balance.setInboundQuantity(inboundQty);
        balance.setInboundAmount(inboundAmt);
        if (inboundQty.compareTo(BigDecimal.ZERO) > 0) {
            balance.setInboundUnitPrice(inboundAmt.divide(inboundQty, 2, RoundingMode.HALF_UP));
        } else {
            balance.setInboundUnitPrice(BigDecimal.ZERO);
        }
        
        // 计算出库
        List<InventoryTransaction> outboundTransactions = transactionRepository
            .findOutboundTransactions(product, startDate, endDate);
        
        BigDecimal outboundQty = BigDecimal.ZERO;
        BigDecimal outboundCostAmt = BigDecimal.ZERO;
        
        for (InventoryTransaction trans : outboundTransactions) {
            outboundQty = outboundQty.add(trans.getQuantity());
            outboundCostAmt = outboundCostAmt.add(trans.getCostAmount());
        }
        
        balance.setOutboundQuantity(outboundQty);
        balance.setOutboundCostAmount(outboundCostAmt);
        if (outboundQty.compareTo(BigDecimal.ZERO) > 0) {
            balance.setOutboundCostUnitPrice(outboundCostAmt.divide(outboundQty, 2, RoundingMode.HALF_UP));
        } else {
            balance.setOutboundCostUnitPrice(BigDecimal.ZERO);
        }
        
        // 计算结存
        BigDecimal endingQty = balance.getBeginningQuantity()
            .add(balance.getInboundQuantity())
            .subtract(balance.getOutboundQuantity());
        
        BigDecimal endingAmt = balance.getBeginningAmount()
            .add(balance.getInboundAmount())
            .subtract(balance.getOutboundCostAmount());
        
        balance.setEndingQuantity(endingQty);
        balance.setEndingAmount(endingAmt);
        if (endingQty.compareTo(BigDecimal.ZERO) > 0) {
            balance.setEndingUnitPrice(endingAmt.divide(endingQty, 2, RoundingMode.HALF_UP));
        } else {
            balance.setEndingUnitPrice(BigDecimal.ZERO);
        }
        
        balanceRepository.save(balance);
    }

    /**
     * 获取月度报表
     */
    public List<InventoryPeriodBalance> getMonthlyReport(String period) {
        return balanceRepository.findByPeriodOrderByProductInventoryCodeAsc(period);
    }

    /**
     * 获取产品的库存交易记录
     */
    public List<InventoryTransaction> getProductTransactions(Product product) {
        return transactionRepository.findByProduct(product);
    }

    /**
     * 获取指定期间的所有交易
     */
    public List<InventoryTransaction> getTransactionsByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }
}