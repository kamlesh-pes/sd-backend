package com.sahastra.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web MVC configuration for CORS, interceptors, and other web-related settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Path productImageDirectory;

    public WebConfig(@Value("${app.media.product-image-directory:./data/product-images}") String imageDirectory) {
        this.productImageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:4200", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = productImageDirectory.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        registry.addResourceHandler("/media/products/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);
    }
}
