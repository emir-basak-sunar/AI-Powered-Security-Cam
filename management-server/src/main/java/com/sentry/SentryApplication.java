package com.sentry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Sentry Management Server.
 * Provides user management, alert persistence, and real-time notifications.
 */
@SpringBootApplication
public class SentryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentryApplication.class, args);
    }
}
