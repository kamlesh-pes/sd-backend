package com.sahastra.backend.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class RateLimitService {
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return current;", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitDecision tryAcquire(String key, long limit, Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit and window must be positive");
        }

        try {
            Long current = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
            long count = current == null ? limit + 1 : current;
            Long ttl = redisTemplate.getExpire(key);
            long retryAfter = ttl == null || ttl < 0 ? window.toSeconds() : ttl;
            return new RateLimitDecision(count <= limit, limit, Math.max(0, limit - count), retryAfter);
        } catch (RuntimeException exception) {
            log.warn("Rate-limit store unavailable; allowing request: {}", exception.getMessage());
            return new RateLimitDecision(true, limit, limit, 0);
        }
    }
}
