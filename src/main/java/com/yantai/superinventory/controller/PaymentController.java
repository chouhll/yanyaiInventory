package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Payment;
import com.yantai.superinventory.model.Payment.PaymentType;
import com.yantai.superinventory.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收付款管理控制器
 */
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 获取所有付款记录
     * @param type 付款类型（可选）
     * @return 付款记录列表
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(required = false) PaymentType type) {
        List<Payment> payments;
        if (type != null) {
            payments = paymentService.getPaymentsByType(type);
        } else {
            payments = paymentService.getAllPayments();
        }
        return ResponseEntity.ok(payments);
    }

    /**
     * 根据ID获取付款记录
     * @param id 付款ID
     * @return 付款记录
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        try {
            return paymentService.getPaymentById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取付款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 根据订单ID获取收款记录
     * @param orderId 订单ID
     * @return 收款记录列表
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    /**
     * 根据采购单ID获取付款记录
     * @param purchaseId 采购单ID
     * @return 付款记录列表
     */
    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<List<Payment>> getPaymentsByPurchaseId(@PathVariable Long purchaseId) {
        List<Payment> payments = paymentService.getPaymentsByPurchaseId(purchaseId);
        return ResponseEntity.ok(payments);
    }

    /**
     * 获取日期范围内的付款记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param type 付款类型（可选）
     * @return 付款记录列表
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Payment>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) PaymentType type) {
        List<Payment> payments;
        if (type != null) {
            payments = paymentService.getPaymentsByTypeAndDateRange(type, startDate, endDate);
        } else {
            payments = paymentService.getPaymentsByDateRange(startDate, endDate);
        }
        return ResponseEntity.ok(payments);
    }

    /**
     * 创建收款记录（订单收款）
     * @param orderId 订单ID
     * @param payment 收款信息
     * @return 创建的收款记录
     */
    @PostMapping("/receivable/{orderId}")
    public ResponseEntity<?> createReceivablePayment(
            @PathVariable String orderId,
            @RequestBody Payment payment) {
        try {
            Payment createdPayment = paymentService.createReceivablePayment(orderId, payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "创建收款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 创建付款记录（采购付款）
     * @param purchaseId 采购单ID
     * @param payment 付款信息
     * @return 创建的付款记录
     */
    @PostMapping("/payable/{purchaseId}")
    public ResponseEntity<?> createPayablePayment(
            @PathVariable Long purchaseId,
            @RequestBody Payment payment) {
        try {
            Payment createdPayment = paymentService.createPayablePayment(purchaseId, payment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "创建付款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新付款记录
     * @param id 付款ID
     * @param paymentDetails 付款详细信息
     * @return 更新后的付款记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long id,
            @RequestBody Payment paymentDetails) {
        try {
            Payment updatedPayment = paymentService.updatePayment(id, paymentDetails);
            return ResponseEntity.ok(updatedPayment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "更新付款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取消付款记录
     * @param id 付款ID
     * @return 更新后的付款记录
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPayment(@PathVariable Long id) {
        try {
            Payment cancelledPayment = paymentService.cancelPayment(id);
            return ResponseEntity.ok(cancelledPayment);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "取消付款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除付款记录
     * @param id 付款ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "付款记录已删除");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "删除付款记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取订单的总收款金额
     * @param orderId 订单ID
     * @return 总收款金额
     */
    @GetMapping("/order/{orderId}/total")
    public ResponseEntity<?> getTotalReceivedAmount(@PathVariable String orderId) {
        try {
            BigDecimal total = paymentService.getTotalReceivedAmount(orderId);
            Map<String, BigDecimal> response = new HashMap<>();
            response.put("totalReceived", total);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取收款总额失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取采购单的总付款金额
     * @param purchaseId 采购单ID
     * @return 总付款金额
     */
    @GetMapping("/purchase/{purchaseId}/total")
    public ResponseEntity<?> getTotalPaidAmount(@PathVariable Long purchaseId) {
        try {
            BigDecimal total = paymentService.getTotalPaidAmount(purchaseId);
            Map<String, BigDecimal> response = new HashMap<>();
            response.put("totalPaid", total);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取付款总额失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}