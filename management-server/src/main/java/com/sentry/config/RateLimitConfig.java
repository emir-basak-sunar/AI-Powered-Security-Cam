package com.sentry.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting configuration using Caffeine in-memory cache.
 * Provides caches for tracking failed attempts, blocked IPs, and request rates.
 */
@Configuration
@Getter
public class RateLimitConfig {

    @Value("${security.rate-limit.max-requests-per-minute:30}")
    private int maxRequestsPerMinute;

    @Value("${security.rate-limit.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.rate-limit.block-duration-minutes:30}")
    private int blockDurationMinutes;

    /**
     * Cache for tracking failed API key attempts per IP.
     * Entries expire after 15 minutes of inactivity.
     */
    @Bean
    public Cache<String, AtomicInteger> failedAttemptsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(10_000)
                .build();
    }

    /**
     * Cache for blocked IP addresses.
     * Entries expire after the configured block duration.
     */
    @Bean
    public Cache<String, Boolean> blockedIpsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(blockDurationMinutes))
                .maximumSize(10_000)
                .build();
    }

    /**
     * Cache for rate limiting requests per IP.
     * Entries expire after 1 minute (sliding window).
     */
    @Bean
    public Cache<String, AtomicInteger> rateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(10_000)
                .build();
    }
}
