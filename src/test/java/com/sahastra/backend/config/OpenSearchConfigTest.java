package com.sahastra.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahastra.backend.domain.document.ProductDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenSearchConfigTest {

    @Test
    void openSearchMapperSerializesProductTimestampsAsIso8601() throws Exception {
        Instant timestamp = Instant.parse("2026-10-21T16:00:00Z");
        ProductDocument document = ProductDocument.builder()
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();

        String json = OpenSearchConfig.createOpenSearchObjectMapper(new ObjectMapper())
                .writeValueAsString(document);

        assertTrue(json.contains("\"created_at\":\"2026-10-21T16:00:00Z\""));
        assertTrue(json.contains("\"updated_at\":\"2026-10-21T16:00:00Z\""));
    }
}
