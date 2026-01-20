package com.yantai.superinventory.controller;

import com.yantai.superinventory.service.CostAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成本分析控制器
 * 提供产品利润分析、客户利润分析、供应商成本分析的REST API
 */
@RestController
@RequestMapping("/api/cost-analysis")
@CrossOrigin(origins = "*")
public class CostAnalysisController {

    @Autowired
    private CostAnalysisService costAnalysisService;

    /**
     * 获取产品利润分析
     * 分析每个产品的销售额、成本、毛利润和毛利率
     */
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getProductProfitAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> analysis = costAnalysisService.getProductProfitAnalysis(startDate, endDate);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取客户利润分析
     * 分析每个客户的销售额、毛利贡献和客户价值
     */
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getCustomerProfitAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> analysis = costAnalysisService.getCustomerProfitAnalysis(startDate, endDate);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取供应商成本分析
     * 分析每个供应商的采购额、采购次数和价格趋势
     */
    @GetMapping("/suppliers")
    public ResponseEntity<List<Map<String, Object>>> getSupplierCostAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> analysis = costAnalysisService.getSupplierCostAnalysis(startDate, endDate);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取产品销售排行榜
     * 返回销售额最高的前N个产品
     */
    @GetMapping("/products/top")
    public ResponseEntity<List<Map<String, Object>>> getProductSalesRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int topN) {
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> ranking = costAnalysisService.getProductSalesRanking(startDate, endDate, topN);
        return ResponseEntity.ok(ranking);
    }

    /**
     * 获取客户价值排行榜
     * 返回价值最高的前N个客户
     */
    @GetMapping("/customers/top")
    public ResponseEntity<List<Map<String, Object>>> getCustomerValueRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int topN) {
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        }
        List<Map<String, Object>> ranking = costAnalysisService.getCustomerValueRanking(startDate, endDate, topN);
        return ResponseEntity.ok(ranking);
    }
}