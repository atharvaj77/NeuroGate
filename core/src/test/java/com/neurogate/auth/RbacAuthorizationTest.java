package com.neurogate.auth;

import com.neurogate.core.cortex.CortexService;
import com.neurogate.core.cortex.dto.AdHocEvaluationResponse;
import com.neurogate.prompts.PromptVersionControlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "neurogate.auth.enabled=true",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptVersionControlService promptVersionControlService;

    @MockBean
    private CortexService cortexService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void viewerCannotCreatePrompts() throws Exception {
        mockMvc.perform(post("/api/prompts/commit")
                        .with(user("viewer").roles("VIEWER"))
                        .contentType("application/json")
                        .content("""
                                {"promptText":"hello","commitMessage":"x","author":"viewer","branchName":"main"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void developerCanRunEvaluations() throws Exception {
        when(cortexService.evaluateAdHoc(any())).thenReturn(AdHocEvaluationResponse.builder()
                .results(List.of())
                .overallScore(0.0)
                .build());

        mockMvc.perform(post("/api/v1/cortex/evaluate")
                        .with(user("developer").roles("DEVELOPER"))
                        .contentType("application/json")
                        .content("""
                                {"promptTemplate":"You are helpful","model":"gpt-4","testCases":[]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateApiKeys() throws Exception {
        when(apiKeyService.createKey(eq("org-a"), eq("Backend Key"), eq(Role.DEVELOPER)))
                .thenReturn(new ApiKeyService.CreatedApiKey(
                        UUID.randomUUID(),
                        "ng_live_test1234567890abcdefghijklmno",
                        "Backend Key",
                        Role.DEVELOPER,
                        Instant.now(),
                        null));

        mockMvc.perform(post("/api/v1/keys")
                        .with(authentication(adminAuthentication()))
                        .contentType("application/json")
                        .content("""
                                {"name":"Backend Key","role":"DEVELOPER"}
                                """))
                .andExpect(status().isOk());
    }

    private Authentication adminAuthentication() {
        ApiPrincipal principal = new ApiPrincipal(
                "user-admin",
                "org-a",
                "admin@example.com",
                "Admin User",
                UUID.randomUUID(),
                Role.ADMIN);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(Role.ADMIN.asAuthority())));
    }
}
