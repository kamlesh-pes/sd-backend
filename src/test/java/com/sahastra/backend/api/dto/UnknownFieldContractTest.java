package com.sahastra.backend.api.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UnknownFieldContractTest {
    @Test
    void requestDeserializationRejectsUnknownFields() {
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        assertThrows(Exception.class, () -> mapper.readValue(
                "{\"shippingAddress\":\"123 Main St\",\"city\":\"Seattle\",\"postalCode\":\"98101\",\"country\":\"US\",\"unexpected\":true}",
                OrderRequest.class));
    }
}
