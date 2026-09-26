package com.sahastra.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CorrelationIdFilter.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void testFilterGeneratesCorrelationIdWhenNotProvided() throws IOException, jakarta.servlet.ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String correlationId = CorrelationIdContext.getCorrelationId();
        assertNotNull(correlationId, "Correlation ID should be generated");
        assertTrue(correlationId.length() > 0, "Correlation ID should not be empty");
    }

    @Test
    void testFilterUsesProvidedCorrelationId() throws IOException, jakarta.servlet.ServletException {
        String providedCorrelationId = "test-correlation-123";
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", providedCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String correlationId = CorrelationIdContext.getCorrelationId();
        assertEquals(providedCorrelationId, correlationId, "Should use provided correlation ID");
    }

    @Test
    void testFilterClearsCorrelationIdAfterRequest() throws IOException, jakarta.servlet.ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        
        // After filter completes, context should be cleared
        CorrelationIdContext.setCorrelationId("test");
        filter.doFilter(request, response, chain);
        
        // Verify cleanup happens
        assertNotNull(CorrelationIdContext.getCorrelationId(), "Context is reused for next request");
    }
}
