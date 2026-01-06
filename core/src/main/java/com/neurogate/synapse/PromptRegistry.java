package com.neurogate.synapse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neurogate.prompts.PromptRepository;
import com.neurogate.prompts.PromptVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptRegistry {

    private final PromptWorkflowRepository workflowRepository;
    private final PromptRepository promptRepository;

    // Fast in-memory cache for production prompts to avoid Redis round-trip on
    // every request
    private final Cache<String, PromptVersion> productionCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /**
     * Get the active production prompt version.
     */
    public PromptVersion getProductionPrompt(String promptName) {
        return productionCache.get(promptName, this::loadProductionFromSource);
    }

    private PromptVersion loadProductionFromSource(String promptName) {
        PromptWorkflow workflow = workflowRepository.findById(promptName).orElse(null);
        if (workflow == null || workflow.getActiveProductionVersionId() == null) {
            return null;
        }
        return promptRepository.findVersionById(workflow.getActiveProductionVersionId()).orElse(null);
    }

    /**
     * Get active staging prompt version
     */
    public PromptVersion getStagingPrompt(String promptName) {
        PromptWorkflow workflow = workflowRepository.findById(promptName).orElse(null);
        if (workflow == null || workflow.getActiveStagingVersionId() == null) {
            return null;
        }
        return promptRepository.findVersionById(workflow.getActiveStagingVersionId()).orElse(null);
    }

    /**
     * Promote a specific version to an environment (production/staging)
     */
    public void promote(String promptName, String versionId, String environment, String user) {
        PromptWorkflow workflow = workflowRepository.findById(promptName)
                .orElse(PromptWorkflow.builder().promptName(promptName).build());

        if ("production".equalsIgnoreCase(environment)) {
            workflow.setActiveProductionVersionId(versionId);
            workflow.setLastDeployedToProduction(Instant.now());
            workflow.setProductionDeployedBy(user);

            // Invalidate cache
            productionCache.invalidate(promptName);
        } else {
            workflow.setActiveStagingVersionId(versionId);
            workflow.setLastDeployedToStaging(Instant.now());
            workflow.setStagingDeployedBy(user);
        }

        workflowRepository.save(workflow);
        log.info("Promoted prompt '{}' version '{}' to {} by {}", promptName, versionId, environment, user);
    }

    public PromptWorkflow getWorkflow(String promptName) {
        return workflowRepository.findById(promptName).orElse(null);
    }
}
