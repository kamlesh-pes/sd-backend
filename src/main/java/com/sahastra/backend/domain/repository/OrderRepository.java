package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.Order;
import com.sahastra.backend.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKeyAndUserId(String idempotencyKey, UUID userId);

    @Query("select o from Order o where o.createdAt >= :from and o.createdAt < :to "
            + "and (:status is null or o.status = :status) order by o.createdAt desc")
    Page<Order> findForReport(@Param("from") Instant from, @Param("to") Instant to,
                              @Param("status") OrderStatus status, Pageable pageable);

    @Query("select o from Order o where o.createdAt >= :from and o.createdAt < :to")
    List<Order> findForMetrics(@Param("from") Instant from, @Param("to") Instant to);
}
