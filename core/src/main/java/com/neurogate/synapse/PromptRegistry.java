package com.neurogate.synapse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neurogate.prompts.PromptRepository;
import com.neurogate.prompts.PromptVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptRegistry {

    private final PromptWorkflowRepository workflowRepository;
    private final PromptRepository promptRepository;
    private final StringRedisTemplate redisTemplate; // Redis for Hot-Swapping

    private static final String REDIS_KEY_PREFIX = "neurogate:prompts:";
    private static final String REDIS_UPDATE_CHANNEL = "neurogate:prompts:updates";

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
        // 1. Try Redis Hot-Swap Key first (Fastest, Distributed)
        try {
            String redisKey = REDIS_KEY_PREFIX + "production:" + promptName;
            String activeVersionId = redisTemplate.opsForValue().get(redisKey);

            if (activeVersionId != null) {
                return promptRepository.findVersionById(activeVersionId).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to DB for prompt: {}", promptName);
        }

        // 2. Fallback to Database (Reliable)
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
        // Similar logic could apply to staging, but we usually want latest DB state for
        // testing
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

            // 1. Update Redis Hot-Swap Key
            try {
                String redisKey = REDIS_KEY_PREFIX + "production:" + promptName;
                redisTemplate.opsForValue().set(redisKey, versionId);

                // 2. Publish invalidation event for other nodes' L1 Caffeine cache
                redisTemplate.convertAndSend(REDIS_UPDATE_CHANNEL, promptName);
                log.info("Hot-swapped prompt '{}' to version '{}' in Redis", promptName, versionId);
            } catch (Exception e) {
                log.error("Failed to update Redis during promotion", e);
                // We typically don't fail the transaction, as DB source of truth is updated
            }

            // Invalidate local cache
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
