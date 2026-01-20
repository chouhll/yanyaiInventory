package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Invoice;
import com.yantai.superinventory.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    /**
     * 为订单开具发票
     */
    @PostMapping("/issue")
    public ResponseEntity<?> issueInvoice(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            String invoiceType = (String) request.get("invoiceType");
            BigDecimal taxRate = new BigDecimal(request.get("taxRate").toString());
            
            Invoice invoice = invoiceService.issueInvoice(orderId, invoiceType, taxRate);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 获取订单的发票
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Invoice> getInvoiceByOrder(@PathVariable String orderId) {
        Invoice invoice = invoiceService.getInvoiceByOrder(orderId);
        if (invoice != null) {
            return ResponseEntity.ok(invoice);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 作废发票
     */
    @PostMapping("/{invoiceId}/void")
    public ResponseEntity<?> voidInvoice(@PathVariable Long invoiceId, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            invoiceService.voidInvoice(invoiceId, reason);
            return ResponseEntity.ok().body(Map.of("message", "发票已作废"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 获取所有发票
     */
    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    /**
     * 根据发票号查询
     */
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<Invoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        if (invoice != null) {
            return ResponseEntity.ok(invoice);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 创建进项发票
     */
    @PostMapping("/input")
    public ResponseEntity<?> createInputInvoice(@RequestBody Map<String, Object> request) {
        try {
            Long purchaseId = Long.valueOf(request.get("purchaseId").toString());
            String invoiceNumber = (String) request.get("invoiceNumber");
            String invoiceType = (String) request.get("invoiceType");
            BigDecimal taxRate = new BigDecimal(request.get("taxRate").toString());
            
            LocalDateTime invoiceDate = null;
            if (request.containsKey("invoiceDate") && request.get("invoiceDate") != null) {
                invoiceDate = LocalDateTime.parse(request.get("invoiceDate").toString());
            }
            
            Invoice invoice = invoiceService.createInputInvoice(purchaseId, invoiceNumber, 
                                                                invoiceType, taxRate, invoiceDate);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 认证进项发票
     */
    @PostMapping("/{invoiceId}/authenticate")
    public ResponseEntity<?> authenticateInputInvoice(@PathVariable Long invoiceId, 
                                                      @RequestBody Map<String, String> request) {
        try {
            String remark = request.get("remark");
            Invoice invoice = invoiceService.authenticateInputInvoice(invoiceId, remark);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 获取进项发票列表
     */
    @GetMapping("/input")
    public ResponseEntity<List<Invoice>> getInputInvoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to last 6 months if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Invoice> invoices = invoiceService.getInputInvoicesByPeriod(startDate, endDate);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 获取销项发票列表
     */
    @GetMapping("/output")
    public ResponseEntity<List<Invoice>> getOutputInvoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to last 6 months if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Invoice> invoices = invoiceService.getOutputInvoicesByPeriod(startDate, endDate);
        return ResponseEntity.ok(invoices);
    }

    /**
     * 获取税务申报数据
     */
    @GetMapping("/tax-declaration")
    public ResponseEntity<Map<String, Object>> getTaxDeclarationData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to last 6 months if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        Map<String, Object> data = invoiceService.getTaxDeclarationData(startDate, endDate);
        return ResponseEntity.ok(data);
    }

    /**
     * 按客户统计发票
     */
    @GetMapping("/statistics/by-customer")
    public ResponseEntity<List<Map<String, Object>>> getInvoiceStatisticsByCustomer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to last 6 months if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> statistics = invoiceService.getInvoiceStatisticsByCustomer(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
}
