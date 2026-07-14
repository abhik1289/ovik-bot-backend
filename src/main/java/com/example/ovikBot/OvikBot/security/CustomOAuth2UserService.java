package com.example.ovikBot.OvikBot.security;

import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import com.example.ovikBot.OvikBot.service.UserProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the Google profile after Spring Security has exchanged the
 * authorization code, delegates persistence to
 * {@link UserProvisioningService}, and produces an {@link OAuth2User} whose
 * principal name is the Google {@code sub} claim (so it can be looked up
 * again by {@link OAuth2AuthenticationSuccessHandler}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String NAME_ATTRIBUTE_KEY = "sub";

    private final UserProvisioningService userProvisioningService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        try {
            return processOAuth2User(oauth2User);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process OAuth2 user", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = stringOf(attributes.get("sub"));
        String email = stringOf(attributes.get("email"));
        String name = stringOf(attributes.get("name"));
        String picture = stringOf(attributes.get("picture"));
        log.warn("THE NAME AND EMAIL IS" + email + name);

        Boolean emailVerified = (Boolean) attributes.get("email_verified");
        if (emailVerified == null || !emailVerified) {
            throw new OAuth2AuthenticationException("Google email is not verified");
        }

        // Persist (or refresh) the user record. The role comes back from the DB.
        AuthenticatedUser principal = userProvisioningService.upsertFromGoogle(googleId, email, name, picture);

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (var granted : oauth2User.getAuthorities()) {
            if (granted instanceof SimpleGrantedAuthority sga)
                authorities.add(sga);
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));

        return new DefaultOAuth2User(
                List.copyOf(authorities),
                attributes,
                NAME_ATTRIBUTE_KEY);
    }

    private static String stringOf(Object value) {
        return value == null ? null : value.toString();
    }
}
