package com.sahastra.backend.api.controller;

import com.sahastra.backend.service.ProductService;
import com.sahastra.backend.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductControllerTest {
    @Test
    void rejectsNegativePage() {
        assertThrows(ValidationException.class, () -> ProductController.pageRequest(-1, 20));
    }

    @Test
    void rejectsPageSizeAboveMaximum() {
        assertThrows(ValidationException.class, () -> ProductController.pageRequest(0, 101));
    }

    @Test
    void acceptsMaximumPageSize() {
        ProductController.pageRequest(0, 100);
    }
}
