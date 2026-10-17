package com.sahastra.backend.service;

import com.sahastra.backend.api.dto.ProductRequest;
import com.sahastra.backend.api.dto.ProductResponse;
import com.sahastra.backend.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCreationServiceTest {
    @TempDir
    Path imageDirectory;

    @Test
    void uploadedImageBecomesThePrimaryProductImage() throws IOException {
        ProductService productService = mock(ProductService.class);
        ProductImageStorageService storage = new ProductImageStorageService(imageDirectory.toString(), "20MB");
        ProductCreationService creationService = new ProductCreationService(productService, storage);
        ProductRequest request = new ProductRequest();
        ProductResponse expected = mock(ProductResponse.class);
        when(productService.create(any(ProductRequest.class), eq("admin"))).thenReturn(expected);

        ProductResponse result = creationService.create(request, upload(), "admin");

        assertEquals(expected, result);
        String primaryUrl = (String) request.getImages().get("primary");
        assertEquals("/media/products/" + Path.of(primaryUrl).getFileName(), primaryUrl);
        String thumbnailUrl = (String) request.getImages().get("thumbnail");
        assertEquals("/media/products/" + Path.of(thumbnailUrl).getFileName(), thumbnailUrl);
        assertEquals(2, request.getImages().size());
        verify(productService).create(request, "admin");
    }

    @Test
    void rejectsImageUrlsInProductJson() {
        ProductService productService = mock(ProductService.class);
        ProductImageStorageService storage = new ProductImageStorageService(imageDirectory.toString(), "20MB");
        ProductCreationService creationService = new ProductCreationService(productService, storage);
        ProductRequest request = new ProductRequest();
        request.setImages(java.util.Map.of("primary", "https://example.com/product.png"));

        assertThrows(ValidationException.class, () -> creationService.create(request, null, "admin"));
    }

    @Test
    void removesImageIfProductCreationFails() throws IOException {
        ProductService productService = mock(ProductService.class);
        ProductImageStorageService storage = new ProductImageStorageService(imageDirectory.toString(), "20MB");
        ProductCreationService creationService = new ProductCreationService(productService, storage);
        when(productService.create(any(ProductRequest.class), eq("admin")))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class,
                () -> creationService.create(new ProductRequest(), upload(), "admin"));

        try (var files = Files.list(imageDirectory)) {
            assertFalse(files.findAny().isPresent());
        }
    }

    private MockMultipartFile upload() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("image", "product.png", "image/png", output.toByteArray());
    }
}
