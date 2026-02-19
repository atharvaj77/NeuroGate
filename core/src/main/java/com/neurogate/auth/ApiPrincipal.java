package com.neurogate.auth;

import java.util.UUID;

public record ApiPrincipal(
        String userId,
        String orgId,
        String email,
        String name,
        UUID apiKeyId,
        Role role) {
}
