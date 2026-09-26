package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.AuditReportResponse;
import com.sahastra.backend.api.dto.CancellationReportResponse;
import com.sahastra.backend.api.dto.OrderReportResponse;
import com.sahastra.backend.api.dto.SummaryMetricsResponse;
import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.entity.OrderStatusHistory;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.OrderStatusHistoryRepository;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class ReportService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportService(OrderRepository orderRepository,
                         OrderStatusHistoryRepository orderStatusHistoryRepository,
                         AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Page<OrderReportResponse> orders(Instant from, Instant to, OrderStatus status,
                                            Pageable pageable, String actorId) {
        validateRange(from, to);
        auditAccess("ORDER_REPORT_VIEWED", actorId, from, to);
        return orderRepository.findForReport(from, to, status, pageable).map(this::toOrderReport);
    }

    @Transactional
    public List<CancellationReportResponse> cancellations(Instant from, Instant to, String actorId) {
        validateRange(from, to);
        auditAccess("CANCELLATION_REPORT_VIEWED", actorId, from, to);
        return orderStatusHistoryRepository.findCancellations(from, to).stream()
                .map(this::toCancellationReport)
                .toList();
    }

    @Transactional
    public SummaryMetricsResponse metrics(Instant from, Instant to, String actorId) {
        validateRange(from, to);
        auditAccess("SUMMARY_REPORT_VIEWED", actorId, from, to);
        List<Order> orders = orderRepository.findForMetrics(from, to);
        List<Order> billableOrders = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        BigDecimal revenue = billableOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long unitsSold = billableOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .mapToLong(item -> item.getQuantity())
                .sum();
        BigDecimal average = billableOrders.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(billableOrders.size()), 2, RoundingMode.HALF_UP);
        return SummaryMetricsResponse.builder()
                .from(from)
                .to(to)
                .orderCount(orders.size())
                .cancelledOrderCount(orders.stream().filter(order -> order.getStatus() == OrderStatus.CANCELLED).count())
                .unitsSold(unitsSold)
                .revenue(revenue)
                .averageOrderValue(average)
                .build();
    }

    @Transactional
    public Page<AuditReportResponse> audit(Instant from, Instant to, Pageable pageable, String actorId) {
        validateRange(from, to);
        auditAccess("AUDIT_REPORT_VIEWED", actorId, from, to);
        return auditLogRepository.findForReport(from, to, pageable).map(this::toAuditReport);
    }

    @Transactional
    public String ordersCsv(Instant from, Instant to, OrderStatus status, String actorId) {
        validateRange(from, to);
        auditAccess("ORDER_REPORT_EXPORTED", actorId, from, to);
        List<OrderReportResponse> rows = orderRepository.findForReport(from, to, status, Pageable.unpaged())
                .getContent().stream().map(this::toOrderReport).toList();
        StringBuilder csv = new StringBuilder("order_id,user_id,status,subtotal,tax,shipping,total,created_at\n");
        for (OrderReportResponse row : rows) {
            csv.append(csv(row.getOrderId())).append(',')
                    .append(csv(row.getUserId())).append(',')
                    .append(csv(row.getStatus())).append(',')
                    .append(csv(row.getSubtotal())).append(',')
                    .append(csv(row.getTax())).append(',')
                    .append(csv(row.getShipping())).append(',')
                    .append(csv(row.getTotal())).append(',')
                    .append(csv(row.getCreatedAt())).append('\n');
        }
        return csv.toString();
    }

    private OrderReportResponse toOrderReport(Order order) {
        return OrderReportResponse.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shipping(order.getShipping())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private CancellationReportResponse toCancellationReport(OrderStatusHistory history) {
        return CancellationReportResponse.builder()
                .orderId(history.getOrder().getId())
                .reason(history.getReason())
                .actorId(history.getActorId())
                .actorType(history.getActorType())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private AuditReportResponse toAuditReport(AuditLog audit) {
        return AuditReportResponse.builder()
                .id(audit.getId())
                .action(audit.getAction())
                .actorId(audit.getActorId())
                .actorType(audit.getActorType())
                .targetEntity(audit.getTargetEntity())
                .targetId(audit.getTargetId())
                .changes(audit.getChanges())
                .createdAt(audit.getCreatedAt())
                .build();
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ValidationException("Report 'from' must be before 'to'");
        }
    }

    private void auditAccess(String action, String actorId, Instant from, Instant to) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorType("ADMIN")
                .targetEntity("REPORT")
                .targetId(action)
                .changes("{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}")
                .correlationId(CorrelationIdContext.getCorrelationId())
                .build());
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
