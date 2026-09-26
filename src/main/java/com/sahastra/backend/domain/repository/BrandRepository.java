package com.sahastra.backend.domain.repository;

import com.sahastra.backend.domain.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    boolean existsBySlug(String slug);
}
