package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.AuditLog;
import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.entity.OrderItem;
import com.sahastra.backend.domain.entity.User;
import com.sahastra.backend.domain.enums.OrderStatus;
import com.sahastra.backend.domain.repository.AuditLogRepository;
import com.sahastra.backend.domain.repository.OrderRepository;
import com.sahastra.backend.domain.repository.OrderStatusHistoryRepository;
import com.sahastra.backend.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void metricsExcludeCancelledOrdersFromRevenueAndUnits() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        Order completed = order(OrderStatus.DELIVERED, new BigDecimal("100.00"), 3);
        Order cancelled = order(OrderStatus.CANCELLED, new BigDecimal("50.00"), 2);
        ReportService service = new ReportService(orderRepository, orderStatusHistoryRepository, auditLogRepository);

        when(orderRepository.findForMetrics(from, to)).thenReturn(List.of(completed, cancelled));

        var metrics = service.metrics(from, to, "admin-1");

        assertEquals(2, metrics.getOrderCount());
        assertEquals(1, metrics.getCancelledOrderCount());
        assertEquals(3, metrics.getUnitsSold());
        assertEquals(new BigDecimal("100.00"), metrics.getRevenue());
        assertEquals(new BigDecimal("100.00"), metrics.getAverageOrderValue());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void exportsFilteredOrdersAsCsvAndAuditsExport() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        Order order = order(OrderStatus.CONFIRMED, new BigDecimal("25.00"), 1);
        ReportService service = new ReportService(orderRepository, orderStatusHistoryRepository, auditLogRepository);

        when(orderRepository.findForReport(from, to, OrderStatus.CONFIRMED, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(order)));

        String csv = service.ordersCsv(from, to, OrderStatus.CONFIRMED, "admin-1");

        assertEquals(true, csv.startsWith("order_id,user_id,status,subtotal,tax,shipping,total,created_at\n"));
        assertEquals(true, csv.contains(",CONFIRMED,"));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsReversedDateRange() {
        ReportService service = new ReportService(orderRepository, orderStatusHistoryRepository, auditLogRepository);
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-01T00:00:00Z");

        assertThrows(ValidationException.class, () -> service.metrics(from, to, "admin-1"));
    }

    private Order order(OrderStatus status, BigDecimal total, int quantity) {
        User user = User.builder().id(UUID.randomUUID()).email("buyer@example.com").firstName("Buyer")
                .lastName("User").passwordHash("hash").build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(status)
                .subtotal(total)
                .tax(BigDecimal.ZERO)
                .shipping(BigDecimal.ZERO)
                .total(total)
                .shippingAddress("123 Main St")
                .city("Seattle")
                .state("WA")
                .postalCode("98101")
                .country("US")
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(Instant.parse("2026-09-15T00:00:00Z"))
                .updatedAt(Instant.parse("2026-09-15T00:00:00Z"))
                .build();
        order.getItems().add(OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productId(UUID.randomUUID())
                .productName("Notebook")
                .sku("SKU-1")
                .quantity(quantity)
                .unitPrice(total)
                .lineTotal(total)
                .build());
        return order;
    }
}
