package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import com.sahastra.backend.api.dto.ProductRequest;
import com.sahastra.backend.api.dto.ProductResponse;
import com.sahastra.backend.common.CorrelationIdContext;
import com.sahastra.backend.service.ProductCreationService;
import com.sahastra.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService productService;
    private final ProductCreationService productCreationService;

    public AdminProductController(ProductService productService, ProductCreationService productCreationService) {
        this.productService = productService;
        this.productCreationService = productCreationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestPart("product") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response("Product created",
                productCreationService.create(request, image, authentication.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody ProductRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(response("Product updated", productService.update(id, request, authentication.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication authentication) {
        productService.delete(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Product deactivated")
                .correlationId(CorrelationIdContext.getCorrelationId()).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> list(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        Page<ProductResponse> products = productService.list(null, null, ProductController.pageRequest(page, size), true);
        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder().success(true).data(products)
                .message("Products retrieved").correlationId(CorrelationIdContext.getCorrelationId()).build());
    }

    private ApiResponse<ProductResponse> response(String message, ProductResponse product) {
        return ApiResponse.<ProductResponse>builder().success(true).data(product).message(message)
                .correlationId(CorrelationIdContext.getCorrelationId()).build();
    }
}
