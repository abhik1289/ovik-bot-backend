package com.example.ovikBot.OvikBot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Externalised authentication settings.
 *
 * <p>
 * The OAuth2 client-id / client-secret are picked up directly by Spring
 * Security from {@code spring.security.oauth2.client.registration.google.*}.
 * This record carries only what we mint and consume ourselves: the JWT
 * settings, frontend redirect URLs, and CORS configuration.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
                String jwtSecret,
                Duration jwtExpiration,
                String frontendSuccessUrl,
                String frontendFailureUrl,
                String cookieName,
                List<String> allowedOrigins,
                boolean secureCookie) {
}
