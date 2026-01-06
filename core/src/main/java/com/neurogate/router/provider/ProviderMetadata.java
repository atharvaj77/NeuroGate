package com.neurogate.router.provider;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Metadata about an LLM provider.
 * Used for intelligent routing decisions.
 */
@Data
@Builder
public class ProviderMetadata {

    /**
     * Provider name
     */
    private String name;

    /**
     * Provider priority (1 = highest, lower numbers = higher priority)
     */
    private int priority;

    /**
     * Is this provider currently enabled?
     */
    private boolean enabled;

    /**
     * Average response latency in milliseconds
     */
    private long avgLatencyMs;

    /**
     * Cost per 1K input tokens (in USD)
     */
    private BigDecimal costPer1kInputTokens;

    /**
     * Cost per 1K output tokens (in USD)
     */
    private BigDecimal costPer1kOutputTokens;

    /**
     * Maximum tokens per request
     */
    private int maxTokens;

    /**
     * Supports streaming responses?
     */
    private boolean supportsStreaming;

    /**
     * Supports function calling?
     */
    private boolean supportsFunctionCalling;

    /**
     * Supports vision (image inputs)?
     */
    private boolean supportsVision;

    /**
     * Maximum requests per minute
     */
    private int maxRpm;

    /**
     * Health check URL
     */
    private String healthCheckUrl;
}
