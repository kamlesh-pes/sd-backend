package com.sahastra.backend.security;

import com.sahastra.backend.api.controller.ProductController;
import com.sahastra.backend.api.dto.ProductResponse;
import com.sahastra.backend.config.SecurityConfig;
import com.sahastra.backend.security.filter.JwtAuthenticationFilter;
import com.sahastra.backend.security.filter.RateLimitFilter;
import com.sahastra.backend.security.ratelimit.RateLimitDecision;
import com.sahastra.backend.security.ratelimit.RateLimitService;
import com.sahastra.backend.service.ProductService;
import com.sahastra.backend.service.SystemSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RateLimitFilter.class})
class SecurityAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private com.sahastra.backend.security.util.JwtTokenUtil jwtTokenUtil;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private SystemSettingService systemSettingService;

    @BeforeEach
    void setUpRateLimitCollaborators() {
        when(systemSettingService.getLong(any(String.class))).thenReturn(120L);
        when(rateLimitService.tryAcquire(any(String.class), anyLong(), any(java.time.Duration.class)))
                .thenReturn(new RateLimitDecision(true, 120, 119, 0));
    }

    @Test
    void publicProductEndpointIsAccessibleAndHasHardeningHeaders() throws Exception {
        when(productService.list(eq(null), eq(null), any(Pageable.class), eq(false)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void unauthenticatedAdminEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}
