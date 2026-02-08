package com.sentry.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * API Key Authentication Filter for AI Engine internal communication.
 */
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    
    @Value("${ai.api-key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null && apiKey.equals(validApiKey)) {
            // Create authentication for API key access
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "ai-service",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_AI_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("API key authentication successful for request: {}", request.getRequestURI());
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to alert endpoints
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/alerts");
    }
}
