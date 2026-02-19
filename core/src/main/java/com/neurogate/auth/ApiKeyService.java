package com.neurogate.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String RAW_KEY_PREFIX = "ng_live_";
    private static final int RAW_KEY_RANDOM_LENGTH = 32;
    private static final int LOOKUP_PREFIX_LENGTH = 12;
    private static final String KEY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreatedApiKey createKey(String orgId, String name, Role role) {
        if (!organizationRepository.existsById(orgId)) {
            organizationRepository.save(Organization.builder()
                    .id(orgId)
                    .name("Organization " + orgId)
                    .plan(Organization.Plan.FREE)
                    .build());
        }

        String rawKey = generateRawKey();
        String keyPrefix = extractLookupPrefix(rawKey);
        String keyHash = passwordEncoder.encode(rawKey);

        ApiKey entity = ApiKey.builder()
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .name(name != null && !name.isBlank() ? name : "NeuroGate API Key")
                .orgId(orgId)
                .role(role != null ? role : Role.DEVELOPER)
                .rateLimit(null)
                .isActive(Boolean.TRUE)
                .build();

        ApiKey saved = apiKeyRepository.save(entity);
        return new CreatedApiKey(saved.getId(), rawKey, saved.getName(), saved.getRole(), saved.getCreatedAt(),
                saved.getExpiresAt());
    }

    @Transactional
    public Optional<ValidatedApiKey> validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(RAW_KEY_PREFIX)) {
            return Optional.empty();
        }

        String lookupPrefix = extractLookupPrefix(rawKey);
        List<ApiKey> candidates = apiKeyRepository.findByKeyPrefixAndIsActiveTrue(lookupPrefix);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        for (ApiKey candidate : candidates) {
            if (candidate.getExpiresAt() != null && candidate.getExpiresAt().isBefore(now)) {
                continue;
            }
            if (!Boolean.TRUE.equals(candidate.getIsActive())) {
                continue;
            }
            if (passwordEncoder.matches(rawKey, candidate.getKeyHash())) {
                candidate.setLastUsedAt(now);
                apiKeyRepository.save(candidate);
                return Optional.of(new ValidatedApiKey(
                        candidate.getId(),
                        candidate.getOrgId(),
                        candidate.getName(),
                        candidate.getRole(),
                        candidate.getRateLimit(),
                        candidate.getExpiresAt()));
            }
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<ApiKeyView> listKeys(String orgId) {
        return apiKeyRepository.findByOrgIdAndIsActiveTrueOrderByCreatedAtDesc(orgId).stream()
                .map(key -> new ApiKeyView(
                        key.getId(),
                        key.getName(),
                        maskKey(key.getKeyPrefix()),
                        key.getKeyPrefix(),
                        key.getRole(),
                        key.getLastUsedAt(),
                        key.getCreatedAt(),
                        key.getExpiresAt(),
                        key.getIsActive()))
                .toList();
    }

    @Transactional
    public void revokeKey(UUID keyId, String orgId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
        if (!key.getOrgId().equals(orgId)) {
            throw new AccessDeniedException("Cannot manage API keys outside your organization");
        }
        key.setIsActive(Boolean.FALSE);
        apiKeyRepository.save(key);
    }

    @Transactional
    public CreatedApiKey rotateKey(UUID keyId, String orgId) {
        ApiKey oldKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
        if (!oldKey.getOrgId().equals(orgId)) {
            throw new AccessDeniedException("Cannot manage API keys outside your organization");
        }
        oldKey.setIsActive(Boolean.FALSE);
        apiKeyRepository.save(oldKey);

        return createKey(orgId, oldKey.getName(), oldKey.getRole());
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> findById(UUID keyId) {
        return apiKeyRepository.findById(keyId);
    }

    private String generateRawKey() {
        StringBuilder sb = new StringBuilder(RAW_KEY_PREFIX.length() + RAW_KEY_RANDOM_LENGTH);
        sb.append(RAW_KEY_PREFIX);
        for (int i = 0; i < RAW_KEY_RANDOM_LENGTH; i++) {
            int idx = secureRandom.nextInt(KEY_ALPHABET.length());
            sb.append(KEY_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    private String extractLookupPrefix(String rawKey) {
        int prefixLength = Math.min(LOOKUP_PREFIX_LENGTH, rawKey.length());
        return rawKey.substring(0, prefixLength);
    }

    private String maskKey(String prefix) {
        return prefix + "..." + "********";
    }

    public record CreatedApiKey(
            UUID keyId,
            String rawKey,
            String name,
            Role role,
            Instant createdAt,
            Instant expiresAt) {
    }

    public record ValidatedApiKey(
            UUID keyId,
            String orgId,
            String name,
            Role role,
            Integer rateLimit,
            Instant expiresAt) {
    }

    public record ApiKeyView(
            UUID keyId,
            String name,
            String maskedKey,
            String keyPrefix,
            Role role,
            Instant lastUsedAt,
            Instant createdAt,
            Instant expiresAt,
            Boolean active) {
    }
}
