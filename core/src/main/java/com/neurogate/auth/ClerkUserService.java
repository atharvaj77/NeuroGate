package com.neurogate.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClerkUserService {

    private final OrganizationRepository organizationRepository;
    private final ApiKeyService apiKeyService;

    @Transactional
    public void syncOnLogin(Jwt jwt) {
        String orgId = resolveOrgId(jwt);
        if (organizationRepository.existsById(orgId)) {
            return;
        }

        String orgName = resolveOrgName(jwt);
        Organization organization = Organization.builder()
                .id(orgId)
                .name(orgName)
                .plan(Organization.Plan.FREE)
                .build();
        organizationRepository.save(organization);

        ApiKeyService.CreatedApiKey bootstrapKey = apiKeyService.createKey(orgId, "Owner Bootstrap Key", Role.OWNER);
        log.info("Provisioned new organization {} for Clerk user {} with bootstrap key {}",
                orgId,
                jwt.getSubject(),
                bootstrapKey.keyId());
    }

    private String resolveOrgId(Jwt jwt) {
        Object orgClaim = jwt.getClaims().get("org_id");
        if (orgClaim instanceof String orgId && !orgId.isBlank()) {
            return orgId;
        }
        Object orgClaimAlt = jwt.getClaims().get("orgId");
        if (orgClaimAlt instanceof String orgId && !orgId.isBlank()) {
            return orgId;
        }
        return "org_" + jwt.getSubject();
    }

    private String resolveOrgName(Jwt jwt) {
        Object orgName = jwt.getClaims().get("org_name");
        if (orgName instanceof String value && !value.isBlank()) {
            return value;
        }

        Object name = jwt.getClaims().get("name");
        if (name instanceof String value && !value.isBlank()) {
            return value + "'s Organization";
        }

        Object email = jwt.getClaims().get("email");
        if (email instanceof String value && value.contains("@")) {
            return value.substring(0, value.indexOf('@')) + " Organization";
        }

        return "NeuroGate Organization";
    }
}
