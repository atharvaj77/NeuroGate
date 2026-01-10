package com.neurogate.synapse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the deployment state of a Prompt.
 * Maps a logical prompt name (e.g., "customer-service-bot") to specific version
 * IDs
 * for different environments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("prompt_workflow")
public class PromptWorkflow implements Serializable {

    @Id
    private String promptName; // e.g., "customer-support-agent"

    private String activeProductionVersionId;
    private String activeStagingVersionId;

    private Instant lastDeployedToProduction;
    private Instant lastDeployedToStaging;

    private String productionDeployedBy; // User who deployed
    private String stagingDeployedBy;

    // Shadow Deployment (Specter Mode)
    private String activeShadowVersionId;
    private Instant lastDeployedToShadow;
    private String shadowDeployedBy;
}
