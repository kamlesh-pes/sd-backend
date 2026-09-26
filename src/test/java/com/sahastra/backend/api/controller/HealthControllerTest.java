package com.sahastra.backend.api.controller;

import com.sahastra.backend.api.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HealthController.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController();
    }

    @Test
    void testPingEndpointReturnsUpStatus() {
        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.ping();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        Map<String, String> data = response.getBody().getData();
        assertEquals("UP", data.get("status"));
        assertEquals("sahastra-backend", data.get("service"));
        assertNotNull(data.get("message"));
    }

    @Test
    void testReadyEndpointReturnsReadyStatus() {
        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.ready();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        Map<String, String> data = response.getBody().getData();
        assertEquals("READY", data.get("status"));
        assertEquals("sahastra-backend", data.get("service"));
    }
}
