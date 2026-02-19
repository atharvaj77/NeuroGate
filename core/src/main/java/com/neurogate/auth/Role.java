package com.neurogate.auth;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum Role {
    VIEWER(1),
    DEVELOPER(2),
    ADMIN(3),
    OWNER(4);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public boolean hasAtLeast(Role required) {
        return this.level >= required.level;
    }

    public String asAuthority() {
        return "ROLE_" + name();
    }

    public static Optional<Role> fromAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return Optional.empty();
        }

        String normalized = authority.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        try {
            return Optional.of(Role.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Role> highestFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return Optional.empty();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(Role::fromAuthority)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Enum::compareTo);
    }

    public static Role fromClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return VIEWER;
        }

        Object orgRole = claims.get("org_role");
        if (orgRole instanceof String roleValue && !roleValue.isBlank()) {
            return fromString(roleValue);
        }

        Object role = claims.get("role");
        if (role instanceof String roleValue && !roleValue.isBlank()) {
            return fromString(roleValue);
        }

        Object roles = claims.get("roles");
        if (roles instanceof Collection<?> roleValues && !roleValues.isEmpty()) {
            return roleValues.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(Role::fromString)
                    .max(Enum::compareTo)
                    .orElse(VIEWER);
        }

        return VIEWER;
    }

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return VIEWER;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("ORG:", "");
        normalized = normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;

        if (normalized.contains("OWNER")) {
            return OWNER;
        }
        if (normalized.contains("ADMIN")) {
            return ADMIN;
        }
        if (normalized.contains("DEVELOPER") || normalized.contains("DEV")) {
            return DEVELOPER;
        }
        return VIEWER;
    }
}
