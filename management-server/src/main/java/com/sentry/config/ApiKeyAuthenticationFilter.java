package com.sentry.config;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Key Authentication Filter with rate limiting and IP blocking.
 * <p>
 * Security features:
 * - Rate limiting: max N requests per minute per IP
 * - Failed attempt tracking: blocks IP after M failed API key attempts
 * - IP blocking: blocked IPs are rejected for configurable duration
 * - Detailed security logging for all events
 */
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${ai.api-key}")
    private String validApiKey;

    private final Cache<String, AtomicInteger> failedAttemptsCache;
    private final Cache<String, Boolean> blockedIpsCache;
    private final Cache<String, AtomicInteger> rateLimitCache;
    private final RateLimitConfig rateLimitConfig;

    public ApiKeyAuthenticationFilter(
            Cache<String, AtomicInteger> failedAttemptsCache,
            Cache<String, Boolean> blockedIpsCache,
            Cache<String, AtomicInteger> rateLimitCache,
            RateLimitConfig rateLimitConfig
    ) {
        this.failedAttemptsCache = failedAttemptsCache;
        this.blockedIpsCache = blockedIpsCache;
        this.rateLimitCache = rateLimitCache;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // 1. Check if IP is blocked
        if (isIpBlocked(clientIp)) {
            log.warn("SECURITY: Blocked IP {} attempted access to {}", clientIp, request.getRequestURI());
            rejectRequest(response, "IP address is temporarily blocked due to too many failed attempts");
            return;
        }

        // 2. Check rate limit
        if (isRateLimitExceeded(clientIp)) {
            log.warn("SECURITY: Rate limit exceeded for IP {} on {}", clientIp, request.getRequestURI());
            rejectRequest(response, "Rate limit exceeded. Try again later.");
            return;
        }

        // 3. Validate API key
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null) {
            if (apiKey.equals(validApiKey)) {
                // Successful authentication — reset failed attempts
                resetFailedAttempts(clientIp);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "ai-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AI_SERVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("API key authentication successful for IP {} on {}", clientIp, request.getRequestURI());
            } else {
                // Invalid API key — track failed attempt
                handleFailedAttempt(clientIp, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the given IP is currently blocked.
     */
    private boolean isIpBlocked(String ip) {
        return blockedIpsCache.getIfPresent(ip) != null;
    }

    /**
     * Check and increment the rate limit counter for the given IP.
     */
    private boolean isRateLimitExceeded(String ip) {
        AtomicInteger counter = rateLimitCache.get(ip, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();
        return currentCount > rateLimitConfig.getMaxRequestsPerMinute();
    }

    /**
     * Handle a failed API key attempt: increment counter and block if threshold exceeded.
     */
    private void handleFailedAttempt(String ip, String uri) {
        AtomicInteger attempts = failedAttemptsCache.get(ip, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        log.warn("SECURITY: Invalid API key attempt #{} from IP {} on {}",
                currentAttempts, ip, uri);

        if (currentAttempts >= rateLimitConfig.getMaxFailedAttempts()) {
            blockedIpsCache.put(ip, Boolean.TRUE);
            log.error("SECURITY: IP {} BLOCKED after {} failed API key attempts. " +
                      "Block duration: {} minutes",
                    ip, currentAttempts, rateLimitConfig.getBlockDurationMinutes());
        }
    }

    /**
     * Reset failed attempts counter after successful authentication.
     */
    private void resetFailedAttempts(String ip) {
        failedAttemptsCache.invalidate(ip);
    }

    /**
     * Send a 429 Too Many Requests response.
     */
    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"" + message + "\"}"
        );
    }

    /**
     * Extract the real client IP, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/alerts");
    }
}

