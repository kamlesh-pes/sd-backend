package com.sahastra.backend.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void deniesRequestWhenRedisCounterExceedsLimit() {
        RateLimitService service = new RateLimitService(redisTemplate);
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(java.util.List.of("rate-key")), eq("60")))
                .thenReturn(6L);
        when(redisTemplate.getExpire("rate-key")).thenReturn(42L);

        RateLimitDecision decision = service.tryAcquire("rate-key", 5, Duration.ofMinutes(1));

        assertFalse(decision.allowed());
        assertEquals(0, decision.remaining());
        assertEquals(42, decision.retryAfterSeconds());
    }

    @Test
    void allowsRequestWithinLimit() {
        RateLimitService service = new RateLimitService(redisTemplate);
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(java.util.List.of("rate-key")), eq("60")))
                .thenReturn(2L);
        when(redisTemplate.getExpire("rate-key")).thenReturn(59L);

        RateLimitDecision decision = service.tryAcquire("rate-key", 5, Duration.ofMinutes(1));

        assertTrue(decision.allowed());
        assertEquals(3, decision.remaining());
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        RateLimitService service = new RateLimitService(redisTemplate);
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(java.util.List.of("rate-key")), eq("60")))
                .thenThrow(new IllegalStateException("redis down"));

        RateLimitDecision decision = service.tryAcquire("rate-key", 5, Duration.ofMinutes(1));

        assertTrue(decision.allowed());
        assertEquals(5, decision.remaining());
    }
}
