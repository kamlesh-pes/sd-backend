package com.sahastra.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;

import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for audit logging of sensitive actions.
 * Every administrative action and security event is logged here.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_audit_log_target", columnList = "target_entity, target_id"),
        @Index(name = "idx_audit_log_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_log_action", columnList = "action")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 255)
    private String actorId;

    @Column(nullable = false, length = 50)
    private String actorType;

    @Column(nullable = false, length = 100)
    private String targetEntity;

    @Column(nullable = false, length = 255)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String changes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 36)
    private String correlationId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
