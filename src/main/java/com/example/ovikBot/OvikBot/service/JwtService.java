package com.example.ovikBot.OvikBot.service;

import com.example.ovikBot.OvikBot.config.AuthProperties;
import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and parses HS256 JWTs. The secret is read from
 * {@code app.auth.jwt-secret} (env var {@code JWT_SECRET}) and may be
 * either base64 or raw text — the implementation prefers base64 when valid.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PICTURE = "picture";
    private static final String CLAIM_ROLE = "role";

    private final AuthProperties authProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = authProperties.jwtSecret();
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32)
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(authProperties.jwtExpiration());
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_UID, user.getId() == null ? null : user.getId().toString())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_PICTURE, user.getPicture())
                .claim(CLAIM_ROLE, user.getRole())
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String uidStr = claims.get(CLAIM_UID, String.class);
        UUID uid = StringUtils.hasText(uidStr) ? UUID.fromString(uidStr) : null;

        return new AuthenticatedUser(
                uid,
                claims.getSubject(),
                claims.get(CLAIM_NAME, String.class),
                claims.get(CLAIM_PICTURE, String.class),
                claims.get(CLAIM_ROLE, String.class));
    }
}
