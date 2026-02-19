package com.neurogate.auth;

import com.neurogate.prompts.JpaPromptVersionRepository;
import com.neurogate.prompts.PromptVersion;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "neurogate.auth.enabled=true",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantIsolationIntegrationTest {

    @Autowired
    private JpaPromptVersionRepository promptVersionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void orgADataIsNotVisibleToOrgB() {
        PromptVersion orgAVersion = PromptVersion.builder()
                .versionId("org-a-version")
                .promptText("Org A prompt")
                .author("a")
                .branchName("main")
                .timestamp(Instant.now())
                .build();
        orgAVersion.setOrgId("org-a");
        promptVersionRepository.save(orgAVersion);

        PromptVersion orgBVersion = PromptVersion.builder()
                .versionId("org-b-version")
                .promptText("Org B prompt")
                .author("b")
                .branchName("main")
                .timestamp(Instant.now())
                .build();
        orgBVersion.setOrgId("org-b");
        promptVersionRepository.save(orgBVersion);

        Session session = entityManager.unwrap(Session.class);
        Filter tenantFilter = session.enableFilter("tenantFilter");
        tenantFilter.setParameter("orgId", "org-a");

        List<PromptVersion> visibleToOrgA = promptVersionRepository.findAll();
        assertThat(visibleToOrgA)
                .extracting(PromptVersion::getOrgId)
                .containsOnly("org-a");

        session.disableFilter("tenantFilter");
    }

    @Test
    void adminOfOrgACannotManageOrgBKeys() throws Exception {
        organizationRepository.save(Organization.builder().id("org-a").name("Org A").plan(Organization.Plan.FREE).build());
        organizationRepository.save(Organization.builder().id("org-b").name("Org B").plan(Organization.Plan.FREE).build());

        ApiKey key = ApiKey.builder()
                .keyPrefix("ng_live_1234")
                .keyHash(passwordEncoder.encode("ng_live_12345678901234567890123456789012"))
                .name("Org B Key")
                .orgId("org-b")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        ApiKey saved = apiKeyRepository.save(key);

        ApiPrincipal orgAAdmin = new ApiPrincipal(
                "admin-a",
                "org-a",
                "admin-a@example.com",
                "Admin A",
                UUID.randomUUID(),
                Role.ADMIN);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                orgAAdmin,
                null,
                List.of(new SimpleGrantedAuthority(Role.ADMIN.asAuthority())));

        mockMvc.perform(delete("/api/v1/keys/{id}", saved.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
