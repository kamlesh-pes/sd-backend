package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.CatalogNameRequest;
import com.sahastra.backend.domain.entity.Brand;
import com.sahastra.backend.domain.entity.Category;
import com.sahastra.backend.domain.repository.BrandRepository;
import com.sahastra.backend.domain.repository.CategoryRepository;
import com.sahastra.backend.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class AdminCatalogController {
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public AdminCatalogController(CategoryRepository categoryRepository, BrandRepository brandRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(@Valid @RequestBody CatalogNameRequest request) {
        String slug = normalize(request.getSlug());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("CATEGORY_SLUG_EXISTS", "Category slug is already in use");
        }
        Category category = categoryRepository.save(Category.builder().name(request.getName().trim()).slug(slug).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Category>builder().success(true).data(category)
                .message("Category created").build());
    }

    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@Valid @RequestBody CatalogNameRequest request) {
        String slug = normalize(request.getSlug());
        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessException("BRAND_SLUG_EXISTS", "Brand slug is already in use");
        }
        Brand brand = brandRepository.save(Brand.builder().name(request.getName().trim()).slug(slug).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Brand>builder().success(true).data(brand)
                .message("Brand created").build());
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> categories() {
        return ResponseEntity.ok(ApiResponse.<List<Category>>builder().success(true).data(categoryRepository.findAll())
                .message("Categories retrieved").build());
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<Brand>>> brands() {
        return ResponseEntity.ok(ApiResponse.<List<Brand>>builder().success(true).data(brandRepository.findAll())
                .message("Brands retrieved").build());
    }

    private String normalize(String slug) {
        return slug.trim().toLowerCase();
    }
}
