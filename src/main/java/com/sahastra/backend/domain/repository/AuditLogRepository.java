package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
	@Query("select a from AuditLog a where a.createdAt >= :from and a.createdAt < :to order by a.createdAt desc")
	Page<AuditLog> findForReport(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
