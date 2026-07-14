package com.example.ovikBot.OvikBot.security;

import com.example.ovikBot.OvikBot.config.AuthProperties;
import com.example.ovikBot.OvikBot.repository.AuthenticatedUser;
import com.example.ovikBot.OvikBot.service.JwtService;
import com.example.ovikBot.OvikBot.service.UserProvisioningService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called after Spring Security has produced an {@link Authentication} via
 * the {@link CustomOAuth2UserService}. We:
 * <ol>
 * <li>load the persisted {@link AuthenticatedUser} by Google {@code sub},</li>
 * <li>mint an HS256 JWT,</li>
 * <li>set it as an httpOnly cookie, and</li>
 * <li>redirect the browser to {@code app.auth.frontend-success-url} with
 * {@code ?token=<jwt>} as a query parameter.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserProvisioningService userProvisioningService;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            log.warn("OAuth2 principal is not an OAuth2User: {}", authentication.getPrincipal());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth2 principal");
            return;
        }

        String googleId = oauth2User.getAttribute("sub");
        if (googleId == null || googleId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Google subject");
            return;
        }

        // Ensure the user has been provisioned (create if missing) so the
        // subsequent principal lookup always succeeds for new accounts.
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        AuthenticatedUser principal = userProvisioningService.upsertFromGoogle(googleId, email, name, picture);
        String token = jwtService.generateToken(principal);

        // 1. Set httpOnly session cookie (used by the SPA's same-origin API calls).
        ResponseCookie cookie = ResponseCookie.from(authProperties.cookieName(), token)
                .httpOnly(true)
                .secure(authProperties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(authProperties.jwtExpiration())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 2. Redirect to the frontend with the token in the query string.
        String target = UriComponentsBuilder.fromUriString(authProperties.frontendSuccessUrl())
                .queryParam("token", token)
                .build(true)
                .toUriString();

        log.info("OAuth2 login success for {} -> redirecting to {}", principal.getEmail(), target);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}