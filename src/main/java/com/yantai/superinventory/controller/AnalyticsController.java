package com.yantai.superinventory.controller;

import com.yantai.superinventory.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据分析控制器
 * 提供销售趋势、采购趋势、库存周转率等分析的REST API
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * 获取销售趋势分析
     * @param groupBy 分组方式: day, week, month
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<List<Map<String, Object>>> getSalesTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "day") String groupBy) {
        List<Map<String, Object>> trend = analyticsService.getSalesTrend(startDate, endDate, groupBy);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取采购趋势分析
     * @param groupBy 分组方式: day, week, month
     */
    @GetMapping("/purchase-trend")
    public ResponseEntity<List<Map<String, Object>>> getPurchaseTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "day") String groupBy) {
        List<Map<String, Object>> trend = analyticsService.getPurchaseTrend(startDate, endDate, groupBy);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取库存周转率分析
     */
    @GetMapping("/inventory-turnover")
    public ResponseEntity<List<Map<String, Object>>> getInventoryTurnoverAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Map<String, Object>> analysis = analyticsService.getInventoryTurnoverAnalysis(startDate, endDate);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取经营分析看板
     */
    @GetMapping("/business-dashboard")
    public ResponseEntity<Map<String, Object>> getBusinessAnalysisDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Map<String, Object> dashboard = analyticsService.getBusinessAnalysisDashboard(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 获取月度对比分析
     */
    @GetMapping("/monthly-comparison")
    public ResponseEntity<Map<String, Object>> getMonthlyComparison(
            @RequestParam(defaultValue = "2026") int year) {
        Map<String, Object> comparison = analyticsService.getMonthlyComparison(year);
        return ResponseEntity.ok(comparison);
    }
}