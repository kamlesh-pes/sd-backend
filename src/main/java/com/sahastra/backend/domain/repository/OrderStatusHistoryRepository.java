package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
	@Query("select h from OrderStatusHistory h where h.toStatus = com.sahastra.backend.domain.enums.OrderStatus.CANCELLED "
			+ "and h.createdAt >= :from and h.createdAt < :to order by h.createdAt desc")
	List<OrderStatusHistory> findCancellations(@Param("from") Instant from, @Param("to") Instant to);
}
