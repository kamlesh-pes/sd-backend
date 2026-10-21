package com.sahastra.backend.service;

import com.sahastra.backend.exception.ValidationException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductImageStorageService {
    public static final long MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 25_000_000;
    private static final int PRIMARY_IMAGE_SIZE = 1200;
    private static final int THUMBNAIL_IMAGE_SIZE = 400;
    private final Path imageDirectory;
    private final long maxImageSizeBytes;

    public ProductImageStorageService(
            @Value("${app.media.product-image-directory:./data/product-images}") String imageDirectory,
            @Value("${app.media.product-image-max-size:20MB}") String maxImageSize) {
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
        this.maxImageSizeBytes = org.springframework.util.unit.DataSize.parse(maxImageSize).toBytes();
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Product image must not be empty");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new ValidationException("Product image exceeds the configured upload size limit");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("image/jpeg")
                && !contentType.equals("image/png") && !contentType.equals("image/webp")
                && !contentType.equals("application/octet-stream")) {
            throw new ValidationException("Product image must be a JPEG, PNG, or WebP file");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded product image", exception);
        }

        ValidatedImage image = validateImage(bytes);
        String baseName = UUID.randomUUID().toString();
        Path original = imageDirectory.resolve(baseName + "-original." + image.extension());
        Path primary = imageDirectory.resolve(baseName + "-1200." + image.extension());
        Path thumbnail = imageDirectory.resolve(baseName + "-400." + image.extension());
        List<Path> temporaryFiles = new ArrayList<>();
        List<Path> storedFiles = List.of(original, primary, thumbnail);
        try {
            Files.createDirectories(imageDirectory);
            Path originalTemporary = createTemporaryFile(".original-");
            temporaryFiles.add(originalTemporary);
            Files.write(originalTemporary, bytes);

            Path primaryTemporary = createTemporaryFile(".1200-");
            temporaryFiles.add(primaryTemporary);
            try (var output = Files.newOutputStream(primaryTemporary)) {
                Thumbnails.of(image.bufferedImage())
                        .size(PRIMARY_IMAGE_SIZE, PRIMARY_IMAGE_SIZE)
                        .keepAspectRatio(true)
                        .outputFormat(image.extension())
                        .toOutputStream(output);
            }

            Path thumbnailTemporary = createTemporaryFile(".400-");
            temporaryFiles.add(thumbnailTemporary);
            try (var output = Files.newOutputStream(thumbnailTemporary)) {
                Thumbnails.of(image.bufferedImage())
                        .size(THUMBNAIL_IMAGE_SIZE, THUMBNAIL_IMAGE_SIZE)
                        .keepAspectRatio(true)
                        .outputFormat(image.extension())
                        .toOutputStream(output);
            }

            moveIntoPlace(originalTemporary, original);
            moveIntoPlace(primaryTemporary, primary);
            moveIntoPlace(thumbnailTemporary, thumbnail);
            return new StoredImage(
                    "/media/products/" + primary.getFileName(),
                    "/media/products/" + thumbnail.getFileName(),
                    primary,
                    thumbnail,
                    original);
        } catch (IOException exception) {
            cleanupFiles(temporaryFiles, storedFiles, exception);
            throw new IllegalStateException("Could not store uploaded product image", exception);
        }
    }

    public void delete(StoredImage image) {
        try {
            Files.deleteIfExists(image.path());
            Files.deleteIfExists(image.thumbnailPath());
            Files.deleteIfExists(image.originalPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove uncommitted product image", exception);
        }
    }

    private ValidatedImage validateImage(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension;
                if (format.equals("jpeg") || format.equals("jpg")) {
                    extension = "jpg";
                } else if (format.equals("png")) {
                    extension = "png";
                } else if (format.equals("webp")) {
                    extension = "webp";
                } else {
                    throw invalidImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new ValidationException("Product image dimensions exceed the supported limit");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw invalidImage();
                }
                return new ValidatedImage(extension, decoded);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private Path createTemporaryFile(String prefix) throws IOException {
        return Files.createTempFile(imageDirectory, ".product-image" + prefix, ".tmp");
    }

    private void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target);
        }
    }

    private void cleanupFiles(List<Path> temporaryFiles, List<Path> storedFiles, IOException exception) {
        for (Path path : temporaryFiles) {
            deleteAfterFailure(path, exception);
        }
        for (Path path : storedFiles) {
            deleteAfterFailure(path, exception);
        }
    }

    private void deleteAfterFailure(Path path, IOException exception) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupException) {
            exception.addSuppressed(cleanupException);
        }
    }

    private ValidationException invalidImage() {
        return new ValidationException("Product image must be a valid JPEG, PNG, or WebP file");
    }

    private record ValidatedImage(String extension, BufferedImage bufferedImage) {
    }

    public record StoredImage(String url, String thumbnailUrl, Path path, Path thumbnailPath, Path originalPath) {
    }
}
