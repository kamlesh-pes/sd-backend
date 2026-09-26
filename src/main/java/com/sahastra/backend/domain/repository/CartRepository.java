package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserId(UUID userId);
    Optional<Cart> findByGuestSessionId(String guestSessionId);
    List<Cart> findByUserIdIsNullAndUpdatedAtBefore(Instant cutoff);
}
