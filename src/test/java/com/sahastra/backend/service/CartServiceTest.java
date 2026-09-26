package com.sahastra.backend.service;

import com.sahastra.backend.domain.entity.Brand;
import com.sahastra.backend.domain.entity.Category;
import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.domain.repository.CartItemRepository;
import com.sahastra.backend.domain.repository.CartRepository;
import com.sahastra.backend.domain.repository.ProductRepository;
import com.sahastra.backend.domain.repository.UserRepository;
import com.sahastra.backend.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemRejectsQuantityAboveAvailableStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .sku("SKU-100")
                .name("Poster")
                .description("Desk poster")
                .basePrice(new BigDecimal("49.99"))
                .stock(2)
                .active(true)
                .category(Category.builder().id(UUID.randomUUID()).name("Decor").slug("decor").build())
                .brand(Brand.builder().id(UUID.randomUUID()).name("Sahastra").slug("sahastra").build())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(ValidationException.class,
                () -> cartService.addItem(null, "guest-123", productId, 3));
    }
}
