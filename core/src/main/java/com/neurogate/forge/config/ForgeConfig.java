package com.neurogate.forge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "neurogate.forge")
@Data
public class ForgeConfig {
    /**
     * Whether the distillation pipeline is enabled.
     */
    private boolean enabled = false;

    /**
     * The teacher model to learn from (usually GPT-4)
     */
    private String teacherModel = "gpt-4";

    /**
     * The base model for the student (e.g., gpt-4o-mini).
     */
    private String studentBaseModel = "gpt-4o-mini";

    /**
     * Minimum number of golden examples required to trigger a training job
     */
    private int triggerThreshold = 100;

    /**
     * Cron expression for the nightly cycle (default: 2 AM)
     */
    private String scheduleCron = "0 0 2 * * ?";
}
