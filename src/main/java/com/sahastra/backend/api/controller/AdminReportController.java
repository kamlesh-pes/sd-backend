package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.AuditReportResponse;
import com.sahastra.backend.api.dto.CancellationReportResponse;
import com.sahastra.backend.api.dto.OrderReportResponse;
import com.sahastra.backend.api.dto.SummaryMetricsResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderReportResponse>>> orders(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Page<OrderReportResponse> report = reportService.orders(fromOrDefault(from), toOrDefault(to), status,
                ProductController.pageRequest(page, size), authentication.getName());
        return ok("Order report retrieved", report);
    }

    @GetMapping("/orders.csv")
    public ResponseEntity<String> ordersCsv(@RequestParam(required = false) Instant from,
                                             @RequestParam(required = false) Instant to,
                                             @RequestParam(required = false) OrderStatus status,
                                             Authentication authentication) {
        String csv = reportService.ordersCsv(fromOrDefault(from), toOrDefault(to), status, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/cancellations")
    public ResponseEntity<ApiResponse<List<CancellationReportResponse>>> cancellations(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Authentication authentication) {
        return ok("Cancellation report retrieved",
                reportService.cancellations(fromOrDefault(from), toOrDefault(to), authentication.getName()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SummaryMetricsResponse>> metrics(@RequestParam(required = false) Instant from,
                                                                         @RequestParam(required = false) Instant to,
                                                                         Authentication authentication) {
        return ok("Summary metrics retrieved",
                reportService.metrics(fromOrDefault(from), toOrDefault(to), authentication.getName()));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<Page<AuditReportResponse>>> audit(@RequestParam(required = false) Instant from,
                                                                         @RequestParam(required = false) Instant to,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size,
                                                                         Authentication authentication) {
        return ok("Audit report retrieved", reportService.audit(fromOrDefault(from), toOrDefault(to),
                ProductController.pageRequest(page, size), authentication.getName()));
    }

    private Instant fromOrDefault(Instant from) {
        return from == null ? Instant.now().minus(30, ChronoUnit.DAYS) : from;
    }

    private Instant toOrDefault(Instant to) {
        return to == null ? Instant.now() : to;
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }
}
