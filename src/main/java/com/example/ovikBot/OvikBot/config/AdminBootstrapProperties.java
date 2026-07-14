package com.example.ovikBot.OvikBot.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the admin bootstrap mechanism. Any Google-authenticated
 * user whose email is listed here is auto-promoted to {@code ADMIN} on first
 * login. This avoids needing to grant roles by hand-editing the database.
 *
 * <p>
 * The {@code emails} value is intentionally a single comma-separated string
 * so it works cleanly with both:
 *
 * <ul>
 * <li>YAML / properties files: {@code app.admin.emails: alice@x.com, bob@y.com}
 * <li>Environment variables: {@code ADMIN_EMAILS=alice@x.com,bob@y.com}
 * </ul>
 *
 * <p>
 * Whitespace around each entry is trimmed and duplicates are removed. An
 * empty / blank value yields an empty set, meaning no admins are bootstrapped.
 */
@ConfigurationProperties(prefix = "app.admin")
public class AdminBootstrapProperties {

        /**
         * Comma-separated list of admin emails. May be {@code null} or blank.
         */
        private String emails = "";

        public String getEmails() {
                return emails;
        }

        public void setEmails(String emails) {
                this.emails = emails == null ? "" : emails;
        }

        /**
         * Parse the comma-separated {@link #getEmails()} value into a clean,
         * immutable, ordered {@link Set} of lower-cased entries.
         */
        public Set<String> emailSet() {
                if (emails == null || emails.isBlank()) {
                        return Collections.emptySet();
                }
                Set<String> out = new LinkedHashSet<>();
                for (String raw : emails.split(",")) {
                        String trimmed = raw.trim();
                        if (!trimmed.isEmpty()) {
                                out.add(trimmed.toLowerCase());
                        }
                }
                return out;
        }

        /**
         * Convenience for {@link #emailSet()} — returns the underlying array form
         * for diagnostics / logging.
         */
        public String[] emailArray() {
                return emailSet().toArray(String[]::new);
        }

        @Override
        public String toString() {
                return "AdminBootstrapProperties{emails=" + Arrays.toString(emailArray()) + "}";
        }
}