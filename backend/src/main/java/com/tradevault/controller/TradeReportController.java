package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.TradeReport;
import com.tradevault.entity.enums.ReportScope;
import com.tradevault.service.TradeReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'TREASURY', 'OPERATIONS')")
public class TradeReportController {

    private static final Logger logger = LoggerFactory.getLogger(TradeReportController.class);

    @Autowired
    private TradeReportService tradeReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TradeReport>>> getAllReports() {
        logger.info("Request received to fetch all trade reports");
        List<TradeReport> reports = tradeReportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully", reports));
    }

    @GetMapping("/scope/{scope}")
    public ResponseEntity<ApiResponse<List<TradeReport>>> getReportsByScope(@PathVariable String scope) {
        logger.info("Request received to fetch trade reports for scope: {}", scope);
        try {
            ReportScope reportScope = ReportScope.valueOf(scope.toUpperCase());
            List<TradeReport> reports = tradeReportService.getReportsByScope(reportScope);
            return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully", reports));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid report scope requested: {}", scope);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid report scope. Must be CLIENT, PRODUCT, BRANCH, or PERIOD"));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TradeReport>> generateReport(@RequestParam String scope) {
        logger.info("Request received to generate a new trade report for scope: {}", scope);
        try {
            ReportScope reportScope = ReportScope.valueOf(scope.toUpperCase());
            TradeReport report = tradeReportService.generateReport(reportScope);
            return ResponseEntity.ok(ApiResponse.success("Report generated successfully", report));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to generate report: invalid scope={}", scope);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid report scope. Must be CLIENT, PRODUCT, BRANCH, or PERIOD"));
        }
    }
}
