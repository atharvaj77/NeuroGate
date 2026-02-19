package com.neurogate.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByKeyPrefixAndIsActiveTrue(String keyPrefix);

    List<ApiKey> findByOrgIdAndIsActiveTrueOrderByCreatedAtDesc(String orgId);

    Optional<ApiKey> findByIdAndOrgId(UUID id, String orgId);
}
