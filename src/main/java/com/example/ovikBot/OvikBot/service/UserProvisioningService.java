package com.example.ovikBot.OvikBot.service;

import com.example.ovikBot.OvikBot.config.AdminBootstrapProperties;
import com.example.ovikBot.OvikBot.model.User;
import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import com.example.ovikBot.OvikBot.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Look up-or-create a {@link User} row for every authenticated Google login.
 * The role stored in the database is the source of truth for authorization
 * decisions — the {@code ADMIN_EMAILS} env var is only consulted when
 * creating a brand-new user (or promoting a USER).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private static final User.Role DEFAULT_ROLE = User.Role.USER;
    private static final User.Role ADMIN_ROLE = User.Role.ADMIN;

    private final UserRepository userRepository;
    private final AdminBootstrapProperties adminBootstrapProperties;

    private Set<String> adminEmails;

    @PostConstruct
    void init() {
        Set<String> parsed = adminBootstrapProperties.emailSet();
        this.adminEmails = parsed == null ? Set.of() : Collections.unmodifiableSet(parsed);
        log.info("Admin bootstrap emails loaded: {}", adminEmails);
    }

    /**
     * Idempotent upsert called by the OAuth2 success flow. Returns the
     * persisted principal so the caller can mint a JWT with the
     * database-sourced role.
     */
    @Transactional
    public AuthenticatedUser upsertFromGoogle(String googleId, String email, String name, String picture) {
        if (!StringUtils.hasText(googleId)) {
            throw new IllegalArgumentException("googleId is required");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("email is required");
        }

        // Always normalise email to lowercase so admin bootstrap lookups match
        // regardless of how Google sends the address (mixed case is allowed).
        String normalisedEmail = email.toLowerCase(Locale.ROOT);
        String normalisedPicture = StringUtils.hasText(picture) ? picture : null;

        Instant now = Instant.now();

        // First look up by googleId, then by email as a fallback (in case
        // the googleId changed for an existing email-based account).
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(normalisedEmail).orElse(null));

        if (user == null) {
            user = User.builder()
                    .googleId(googleId)
                    .email(normalisedEmail)
                    .name(name)
                    .picture(normalisedPicture)
                    .role(adminEmails.contains(normalisedEmail) ? ADMIN_ROLE : DEFAULT_ROLE)
                    .provider(User.Provider.GOOGLE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            log.info("Provisioning new user: email={} role={}", normalisedEmail, user.getRole());
        } else {
            user.setGoogleId(googleId);
            user.setEmail(normalisedEmail);
            user.setName(name);
            user.setPicture(normalisedPicture);
            user.setUpdatedAt(now);
            // Promote USER -> ADMIN if the email landed in the admin list.
            if (user.getRole() == User.Role.USER && adminEmails.contains(normalisedEmail)) {
                user.setRole(ADMIN_ROLE);
            }
        }

        User saved = userRepository.save(user);
        return toPrincipal(saved);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser findPrincipalByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId)
                .map(UserProvisioningService::toPrincipal)
                .orElseThrow(() -> new IllegalStateException("User not found for googleId=" + googleId));
    }

    private static AuthenticatedUser toPrincipal(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPicture(),
                user.getRole().name());
    }
}
