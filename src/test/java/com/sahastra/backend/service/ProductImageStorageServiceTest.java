package com.sahastra.backend.service;

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductImageStorageServiceTest {
    @TempDir
    Path imageDirectory;

    private ProductImageStorageService storageService() {
        return new ProductImageStorageService(imageDirectory.toString(), "20MB");
    }

    @Test
    void storesOriginalAndGenerates1200And400PixelPngs() throws IOException {
        byte[] png = pngImage();

        ProductImageStorageService.StoredImage stored = storageService().store(
                new MockMultipartFile("image", "product.png", "image/png", png));

        assertEquals("/media/products/" + stored.path().getFileName(), stored.url());
        assertEquals("/media/products/" + stored.thumbnailPath().getFileName(), stored.thumbnailUrl());
        assertArrayEquals(png, Files.readAllBytes(stored.originalPath()));
        assertEquals(1200, ImageIO.read(stored.path().toFile()).getWidth());
        assertEquals(600, ImageIO.read(stored.path().toFile()).getHeight());
        assertEquals(400, ImageIO.read(stored.thumbnailPath().toFile()).getWidth());
        assertEquals(200, ImageIO.read(stored.thumbnailPath().toFile()).getHeight());
    }

    @Test
    void storesOriginalAndGenerates1200And400PixelWebps() throws IOException {
        byte[] webp = webpImage();

        ProductImageStorageService.StoredImage stored = storageService().store(
                new MockMultipartFile("image", "product.webp", "image/webp", webp));

        assertEquals("webp", stored.path().getFileName().toString().substring(
                stored.path().getFileName().toString().lastIndexOf('.') + 1));
        assertArrayEquals(webp, Files.readAllBytes(stored.originalPath()));
        assertEquals(1200, ImageIO.read(stored.path().toFile()).getWidth());
        assertEquals(600, ImageIO.read(stored.path().toFile()).getHeight());
        assertEquals(400, ImageIO.read(stored.thumbnailPath().toFile()).getWidth());
        assertEquals(200, ImageIO.read(stored.thumbnailPath().toFile()).getHeight());
    }

    @Test
    void rejectsFilesLargerThanConfiguredSize() {
        byte[] tooLarge = new byte[(int) ProductImageStorageService.MAX_IMAGE_SIZE_BYTES + 1];

        assertThrows(ValidationException.class, () -> storageService().store(
                new MockMultipartFile("image", "large.png", "image/png", tooLarge)));
    }

    @Test
    void honorsConfiguredUploadSize() throws IOException {
        ProductImageStorageService service = new ProductImageStorageService(imageDirectory.toString(), "1B");

        assertThrows(ValidationException.class, () -> service.store(
                new MockMultipartFile("image", "product.png", "image/png", pngImage())));
    }

    @Test
    void rejectsNonImageContentEvenWhenMimeTypeClaimsPng() {
        assertThrows(ValidationException.class, () -> storageService().store(
                new MockMultipartFile("image", "fake.png", "image/png", new byte[]{1, 2, 3})));
    }

    @Test
    void deletesStoredImage() throws IOException {
        ProductImageStorageService service = storageService();
        ProductImageStorageService.StoredImage stored = service.store(
                new MockMultipartFile("image", "product.png", "image/png", pngImage()));

        service.delete(stored);

        assertFalse(Files.exists(stored.path()));
        assertFalse(Files.exists(stored.thumbnailPath()));
        assertFalse(Files.exists(stored.originalPath()));
    }

    private byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] webpImage() throws IOException {
        BufferedImage image = new BufferedImage(1600, 800, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "webp", output)) {
            throw new IOException("WebP ImageIO writer is unavailable");
        }
        return output.toByteArray();
    }
}
