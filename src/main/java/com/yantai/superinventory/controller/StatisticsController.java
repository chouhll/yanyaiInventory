package com.yantai.superinventory.controller;

import com.yantai.superinventory.service.StatisticsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Map;

@CrossOrigin(origins = "*")
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/statistics")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        return statisticsService.getDashboardStatistics(start, end);
    }
    
    /**
     * 获取资产负债表
     * GET /api/statistics/balance-sheet?asOfDate=2026-01-08
     */
    @GetMapping("/balance-sheet")
    public Map<String, Object> getBalanceSheet(
            @RequestParam(required = false) String asOfDate) {
        
        LocalDate date = asOfDate != null ? LocalDate.parse(asOfDate) : LocalDate.now();
        
        return statisticsService.getBalanceSheet(date);
    }
    
    /**
     * 获取利润表
     * GET /api/statistics/income-statement?startDate=2026-01-01&endDate=2026-01-31
     */
    @GetMapping("/income-statement")
    public Map<String, Object> getIncomeStatement(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        return statisticsService.getIncomeStatement(start, end);
    }
    
    /**
     * 获取现金流量表
     * GET /api/statistics/cash-flow?startDate=2026-01-01&endDate=2026-01-31
     */
    @GetMapping("/cash-flow")
    public Map<String, Object> getCashFlowStatement(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        return statisticsService.getCashFlowStatement(start, end);
    }
}
