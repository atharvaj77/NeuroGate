package com.neurogate.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Manage organization API keys")
@RequiresRole(Role.ADMIN)
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(summary = "Create API key", description = "Create a new API key for the authenticated organization")
    @ApiResponse(responseCode = "200", description = "API key created")
    @PostMapping
    public ResponseEntity<CreateKeyResponse> createKey(@RequestBody CreateKeyRequest request, Authentication authentication) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        ApiKeyService.CreatedApiKey createdKey = apiKeyService.createKey(orgId, request.getName(), request.getRole());
        return ResponseEntity.ok(new CreateKeyResponse(
                createdKey.keyId(),
                createdKey.name(),
                createdKey.role(),
                createdKey.rawKey(),
                createdKey.createdAt(),
                createdKey.expiresAt()));
    }

    @Operation(summary = "List API keys", description = "List active API keys for the authenticated organization")
    @ApiResponse(responseCode = "200", description = "API keys listed")
    @GetMapping
    public ResponseEntity<List<ApiKeyService.ApiKeyView>> listKeys(Authentication authentication) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        return ResponseEntity.ok(apiKeyService.listKeys(orgId));
    }

    @Operation(summary = "Revoke API key", description = "Deactivate an API key in the authenticated organization")
    @ApiResponse(responseCode = "204", description = "API key revoked")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable UUID id, Authentication authentication) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        apiKeyService.revokeKey(id, orgId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rotate API key", description = "Create a replacement key and deactivate the previous key")
    @ApiResponse(responseCode = "200", description = "API key rotated")
    @PostMapping("/{id}/rotate")
    public ResponseEntity<CreateKeyResponse> rotateKey(@PathVariable UUID id, Authentication authentication) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        ApiKeyService.CreatedApiKey rotated = apiKeyService.rotateKey(id, orgId);
        return ResponseEntity.ok(new CreateKeyResponse(
                rotated.keyId(),
                rotated.name(),
                rotated.role(),
                rotated.rawKey(),
                rotated.createdAt(),
                rotated.expiresAt()));
    }

    @Data
    public static class CreateKeyRequest {
        private String name;
        private Role role = Role.DEVELOPER;
    }

    public record CreateKeyResponse(
            UUID id,
            String name,
            Role role,
            String key,
            java.time.Instant createdAt,
            java.time.Instant expiresAt) {
    }
}
