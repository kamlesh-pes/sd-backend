package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {
    Optional<SupportRequest> findByOrderIdAndUserId(UUID orderId, UUID userId);
}
