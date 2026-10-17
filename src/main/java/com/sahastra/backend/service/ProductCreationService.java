package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.ProductRequest;
import com.sahastra.backend.api.dto.ProductResponse;
import com.sahastra.backend.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ProductCreationService {
    private final ProductService productService;
    private final ProductImageStorageService imageStorageService;

    public ProductCreationService(ProductService productService, ProductImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    public ProductResponse create(ProductRequest request, MultipartFile image, String actorId) {
        if (request.getImages() != null) {
            throw new ValidationException("Upload the product image using the image multipart part");
        }
        if (image == null) {
            return productService.create(request, actorId);
        }

        ProductImageStorageService.StoredImage storedImage = imageStorageService.store(image);
        Map<String, Object> images = new HashMap<>();
        images.put("primary", storedImage.url());
        images.put("thumbnail", storedImage.thumbnailUrl());
        request.setImages(images);

        try {
            return productService.create(request, actorId);
        } catch (RuntimeException exception) {
            try {
                imageStorageService.delete(storedImage);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Could not clean up product image after product creation failed: {}",
                        storedImage.url(), cleanupException);
            }
            throw exception;
        }
    }
}
