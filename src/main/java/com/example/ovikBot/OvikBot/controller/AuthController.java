package com.example.ovikBot.OvikBot.controller;

import com.example.ovikBot.OvikBot.config.AuthProperties;
import com.example.ovikBot.OvikBot.dto.AuthUserResponse;
import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Google login itself happens entirely through
 * the Spring Security OAuth2 redirect flow (see {@code SecurityConfig}),
 * so this controller only exposes the post-login helpers that the SPA
 * calls directly:
 *
 * <ul>
 * <li>{@code GET  /api/user/me} — current principal (JWT-protected).</li>
 * <li>{@code POST /api/auth/logout} — clears the session cookie.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthProperties authProperties;

    /**
     * Returns the currently authenticated principal. Protected by the
     * {@code JwtAuthenticationFilter}, so a missing / invalid JWT yields 401.
     */
    @GetMapping("/api/user/me")
    public ResponseEntity<AuthUserResponse> me(Authentication authentication,HttpHeaders httpHeaders) {
//        log.warn(httpHeaders.getA);
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(toResponse(user));
    }

    /**
     * Clears the session cookie. Idempotent and safe to call anonymously.
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildExpiredCookie().toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie buildExpiredCookie() {
        return ResponseCookie.from(authProperties.cookieName(), "")
                .httpOnly(true)
                .secure(authProperties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private AuthUserResponse toResponse(AuthenticatedUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPicture(),
                user.getRole());
    }
}