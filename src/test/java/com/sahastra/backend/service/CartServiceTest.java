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

import com.sahastra.backend.domain.entity.Cart;
import com.sahastra.backend.domain.entity.CartItem;
import com.sahastra.backend.domain.entity.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
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

    @Test
    void getCartMergesGuestCartIntoUserCartWhenUserIsAuthenticated() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String guestSessionId = "guest-123";

        User user = User.builder().id(userId).email("user@example.com").build();
        Product product = Product.builder()
                .id(productId)
                .sku("SKU-200")
                .name("Notebook")
                .description("Travel notebook")
                .basePrice(new BigDecimal("20.00"))
                .stock(10)
                .active(true)
                .category(Category.builder().id(UUID.randomUUID()).name("Office").slug("office").build())
                .brand(Brand.builder().id(UUID.randomUUID()).name("Sahastra").slug("sahastra").build())
                .build();

        Cart guestCart = Cart.builder()
                .id(UUID.randomUUID())
                .guestSessionId(guestSessionId)
                .items(new ArrayList<>())
                .active(true)
                .build();
        CartItem guestItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(guestCart)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("20.00"))
                .build();
        guestCart.getItems().add(guestItem);

        Cart userCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(user)
                .items(new ArrayList<>())
                .active(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(userCart));
        when(cartRepository.findByGuestSessionId(guestSessionId)).thenReturn(Optional.of(guestCart));
        when(cartItemRepository.findByCartIdAndProductId(userCart.getId(), productId)).thenReturn(Optional.empty());

        var result = cartService.getCart(userId, guestSessionId);

        assertEquals(userId, result.getUserId());
        assertEquals(1, result.getItems().size());
        assertEquals(productId, result.getItems().get(0).getProductId());
        assertEquals(2, result.getItems().get(0).getQuantity());
        verify(cartRepository).delete(guestCart);
    }
}
