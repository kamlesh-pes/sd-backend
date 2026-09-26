package com.sahastra.backend.security.filter;

import com.sahastra.backend.security.ratelimit.RateLimitDecision;
import com.sahastra.backend.security.ratelimit.RateLimitService;
import com.sahastra.backend.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String API_PREFIX = "/api/v1/";
    private final RateLimitService rateLimitService;
    private final SystemSettingService settingService;

    public RateLimitFilter(RateLimitService rateLimitService, SystemSettingService settingService) {
        this.rateLimitService = rateLimitService;
        this.settingService = settingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(API_PREFIX) || path.startsWith("/api/v1/health/")) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitRule rule = ruleFor(path);
        String key = "rate-limit:" + rule.name + ":" + clientAddress(request);
        RateLimitDecision decision = rateLimitService.tryAcquire(key, settingService.getLong(rule.settingKey), Duration.ofMinutes(1));
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private LimitRule ruleFor(String path) {
        if (path.equals("/api/v1/auth/login")) {
            return new LimitRule("login", SystemSettingService.LOGIN_RATE_LIMIT_PER_MINUTE);
        }
        if (path.equals("/api/v1/auth/verify-otp") || path.equals("/api/v1/auth/request-password-reset")) {
            return new LimitRule("otp", SystemSettingService.OTP_RATE_LIMIT_PER_MINUTE);
        }
        return new LimitRule("api", SystemSettingService.API_RATE_LIMIT_PER_MINUTE);
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record LimitRule(String name, String settingKey) {
    }
}
