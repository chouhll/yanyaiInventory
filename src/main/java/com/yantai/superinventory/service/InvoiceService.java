package com.yantai.superinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantai.superinventory.model.*;
import com.yantai.superinventory.repository.InvoiceRepository;
import com.yantai.superinventory.repository.OrderRepository;
import com.yantai.superinventory.repository.PurchaseRepository;
import com.yantai.superinventory.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为订单开具发票
     */
    @Transactional
    public Invoice issueInvoice(String orderId, String invoiceType, BigDecimal taxRate) {
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查是否已开票
        if (order.getInvoice() != null) {
            throw new RuntimeException("该订单已开具发票");
        }

        // 检查订单状态
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.INVOICED) {
            throw new RuntimeException("只有已付款的订单才能开具发票");
        }

        Customer customer = order.getCustomer();
        
        // 检查客户开票信息
        if (customer.getTaxNumber() == null || customer.getTaxNumber().trim().isEmpty()) {
            throw new RuntimeException("客户开票信息不完整，请先完善客户的税号等信息");
        }

        // 创建发票
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setDirection(Invoice.InvoiceDirection.OUTPUT);
        invoice.setInvoiceType(Invoice.InvoiceType.valueOf(invoiceType));
        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);

        // 复制客户开票信息
        invoice.setTaxNumber(customer.getTaxNumber());
        invoice.setCompanyName(customer.getCompanyName());
        invoice.setCompanyAddress(customer.getCompanyAddress());
        invoice.setBankName(customer.getBankName());
        invoice.setBankAccount(customer.getBankAccount());

        // 计算发票金额
        BigDecimal totalAmount = order.getItems().stream()
            .map(item -> item.getSubtotal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount = totalAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountWithTax = totalAmount.add(taxAmount);

        invoice.setTotalAmount(totalAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setAmountWithTax(amountWithTax);

        // 保存发票明细为JSON
        try {
            List<Map<String, Object>> itemList = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productName", item.getProduct().getName());
                    itemMap.put("specification", item.getProduct().getSpecification());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("amount", item.getSubtotal());
                    return itemMap;
                })
                .collect(Collectors.toList());
            invoice.setItems(objectMapper.writeValueAsString(itemList));
        } catch (Exception e) {
            throw new RuntimeException("发票明细序列化失败", e);
        }

        // 保存发票
        invoice = invoiceRepository.save(invoice);

        // 更新订单状态为已开票
        order.setStatus(OrderStatus.INVOICED);
        orderRepository.save(order);

        return invoice;
    }

    /**
     * 获取订单的发票
     */
    public Invoice getInvoiceByOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        return invoiceRepository.findByOrder(order).orElse(null);
    }

    /**
     * 为采购单创建进项发票
     */
    @Transactional
    public Invoice createInputInvoice(Long purchaseId, String invoiceNumber, 
                                     String invoiceType, BigDecimal taxRate, 
                                     LocalDateTime invoiceDate) {
        // 获取采购单
        Purchase purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new RuntimeException("采购单不存在"));

        Supplier supplier = purchase.getSupplier();
        if (supplier == null) {
            throw new RuntimeException("采购单未关联供应商");
        }

        // 检查供应商开票信息
        if (supplier.getTaxNumber() == null || supplier.getTaxNumber().trim().isEmpty()) {
            throw new RuntimeException("供应商开票信息不完整");
        }

        // 创建进项发票
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setDirection(Invoice.InvoiceDirection.INPUT);
        invoice.setPurchase(purchase);
        invoice.setSupplier(supplier);
        invoice.setInvoiceDate(invoiceDate != null ? invoiceDate : LocalDateTime.now());
        invoice.setInvoiceType(Invoice.InvoiceType.valueOf(invoiceType));
        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);

        // 复制供应商开票信息
        invoice.setTaxNumber(supplier.getTaxNumber());
        invoice.setCompanyName(supplier.getCompanyName());
        invoice.setCompanyAddress(supplier.getAddress());
        invoice.setBankName(supplier.getBankName());
        invoice.setBankAccount(supplier.getBankAccount());

        // 计算发票金额
        BigDecimal totalAmount = purchase.getTotalAmount();
        BigDecimal taxAmount = totalAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountWithTax = totalAmount.add(taxAmount);

        invoice.setTotalAmount(totalAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setAmountWithTax(amountWithTax);

        // 保存发票明细
        try {
            List<Map<String, Object>> itemList = new ArrayList<>();
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productName", purchase.getProduct().getName());
            itemMap.put("specification", purchase.getProduct().getSpecification());
            itemMap.put("quantity", purchase.getQuantity());
            itemMap.put("unitPrice", purchase.getUnitPrice());
            itemMap.put("amount", totalAmount);
            itemList.add(itemMap);
            invoice.setItems(objectMapper.writeValueAsString(itemList));
        } catch (Exception e) {
            throw new RuntimeException("发票明细序列化失败", e);
        }

        return invoiceRepository.save(invoice);
    }

    /**
     * 认证进项发票
     */
    @Transactional
    public Invoice authenticateInputInvoice(Long invoiceId, String remark) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("发票不存在"));

        if (invoice.getDirection() != Invoice.InvoiceDirection.INPUT) {
            throw new RuntimeException("只能认证进项发票");
        }

        if (invoice.getIsAuthenticated()) {
            throw new RuntimeException("该发票已认证");
        }

        invoice.setIsAuthenticated(true);
        invoice.setAuthenticationDate(LocalDate.now());
        invoice.setAuthenticationRemark(remark);

        return invoiceRepository.save(invoice);
    }

    /**
     * 获取进项发票列表（按期间）
     */
    public List<Invoice> getInputInvoicesByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.findAll().stream()
            .filter(inv -> inv.getDirection() == Invoice.InvoiceDirection.INPUT)
            .filter(inv -> !inv.getInvoiceDate().isBefore(startDate) && !inv.getInvoiceDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    /**
     * 获取销项发票列表（按期间）
     */
    public List<Invoice> getOutputInvoicesByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.findAll().stream()
            .filter(inv -> inv.getDirection() == Invoice.InvoiceDirection.OUTPUT)
            .filter(inv -> !inv.getInvoiceDate().isBefore(startDate) && !inv.getInvoiceDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    /**
     * 获取税务申报数据
     */
    public Map<String, Object> getTaxDeclarationData(LocalDateTime startDate, LocalDateTime endDate) {
        List<Invoice> outputInvoices = getOutputInvoicesByPeriod(startDate, endDate);
        List<Invoice> inputInvoices = getInputInvoicesByPeriod(startDate, endDate);

        // 销项税额（只统计已开具的专用发票）
        BigDecimal outputTaxAmount = outputInvoices.stream()
            .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
            .filter(inv -> inv.getInvoiceType() == Invoice.InvoiceType.SPECIAL)
            .map(Invoice::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 进项税额（只统计已认证的专用发票）
        BigDecimal inputTaxAmount = inputInvoices.stream()
            .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
            .filter(inv -> inv.getInvoiceType() == Invoice.InvoiceType.SPECIAL)
            .filter(Invoice::getIsAuthenticated)
            .map(Invoice::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 应纳税额 = 销项税额 - 进项税额
        BigDecimal payableTaxAmount = outputTaxAmount.subtract(inputTaxAmount);

        // 销售额（不含税）
        BigDecimal outputAmount = outputInvoices.stream()
            .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 进项金额（不含税）
        BigDecimal inputAmount = inputInvoices.stream()
            .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("outputTaxAmount", outputTaxAmount);
        result.put("inputTaxAmount", inputTaxAmount);
        result.put("payableTaxAmount", payableTaxAmount);
        result.put("outputAmount", outputAmount);
        result.put("inputAmount", inputAmount);
        result.put("outputInvoiceCount", outputInvoices.size());
        result.put("inputInvoiceCount", inputInvoices.size());
        result.put("authenticatedInputCount", inputInvoices.stream()
            .filter(Invoice::getIsAuthenticated).count());

        return result;
    }

    /**
     * 按客户统计发票
     */
    public List<Map<String, Object>> getInvoiceStatisticsByCustomer(LocalDateTime startDate, LocalDateTime endDate) {
        List<Invoice> outputInvoices = getOutputInvoicesByPeriod(startDate, endDate);
        
        Map<Long, List<Invoice>> groupedByCustomer = outputInvoices.stream()
            .filter(inv -> inv.getCustomer() != null)
            .collect(Collectors.groupingBy(inv -> inv.getCustomer().getId()));

        List<Map<String, Object>> statistics = new ArrayList<>();
        for (Map.Entry<Long, List<Invoice>> entry : groupedByCustomer.entrySet()) {
            List<Invoice> invoices = entry.getValue();
            Customer customer = invoices.get(0).getCustomer();

            BigDecimal totalAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalTax = invoices.stream()
                .map(Invoice::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> stat = new HashMap<>();
            stat.put("customerId", customer.getId());
            stat.put("customerName", customer.getName());
            stat.put("invoiceCount", invoices.size());
            stat.put("totalAmount", totalAmount);
            stat.put("totalTax", totalTax);
            stat.put("totalAmountWithTax", totalAmount.add(totalTax));
            statistics.add(stat);
        }

        return statistics;
    }

    /**
     * 作废发票
     */
    @Transactional
    public void voidInvoice(Long invoiceId, String reason) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("发票不存在"));

        if (invoice.getStatus() != Invoice.InvoiceStatus.ISSUED) {
            throw new RuntimeException("只能作废已开具的发票");
        }

        invoice.setStatus(Invoice.InvoiceStatus.VOIDED);
        invoice.setRemark(reason);
        invoiceRepository.save(invoice);

        // 如果是销项发票，更新订单状态回到已付款
        if (invoice.getDirection() == Invoice.InvoiceDirection.OUTPUT && invoice.getOrder() != null) {
            Order order = invoice.getOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }
    }

    /**
     * 获取所有发票
     */
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    /**
     * 根据发票号查询
     */
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber).orElse(null);
    }
}