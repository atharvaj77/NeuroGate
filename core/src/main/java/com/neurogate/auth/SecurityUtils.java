package com.neurogate.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<String> resolveOrgId(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ApiPrincipal apiPrincipal) {
            return Optional.ofNullable(apiPrincipal.orgId());
        }
        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaims().get("org_id");
            if (claim == null) {
                claim = jwt.getClaims().get("orgId");
            }
            if (claim instanceof String orgId && !orgId.isBlank()) {
                return Optional.of(orgId);
            }
            return Optional.ofNullable(jwt.getSubject())
                    .map(subject -> "org_" + subject.toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    public static Optional<String> resolveUserId(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ApiPrincipal apiPrincipal) {
            return Optional.ofNullable(apiPrincipal.userId());
        }
        if (principal instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject());
        }
        return Optional.ofNullable(authentication.getName());
    }

    public static Optional<Role> resolveRole(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ApiPrincipal apiPrincipal) {
            return Optional.of(apiPrincipal.role());
        }

        if (principal instanceof Jwt jwt) {
            return Optional.of(Role.fromClaims(jwt.getClaims()));
        }

        return Role.highestFromAuthorities(authentication.getAuthorities());
    }

    public static Optional<ApiPrincipal> getApiPrincipal(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof ApiPrincipal apiPrincipal) {
            return Optional.of(apiPrincipal);
        }
        return Optional.empty();
    }

    public static Optional<UUID> resolveApiKeyId(Authentication authentication) {
        return getApiPrincipal(authentication).map(ApiPrincipal::apiKeyId);
    }
}
