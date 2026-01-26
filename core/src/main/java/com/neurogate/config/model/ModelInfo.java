package com.neurogate.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Information about an available model including pricing.
 */
@Data
@Builder
@Schema(description = "Model information including pricing and capabilities")
public class ModelInfo {

    @Schema(description = "Model identifier", example = "gpt-4o")
    private String id;

    @Schema(description = "Display name", example = "GPT-4o")
    private String name;

    @Schema(description = "Provider name", example = "openai")
    private String provider;

    @Schema(description = "Cost per 1K input tokens in USD", example = "0.0025")
    private BigDecimal inputCostPer1k;

    @Schema(description = "Cost per 1K output tokens in USD", example = "0.01")
    private BigDecimal outputCostPer1k;

    @Schema(description = "Maximum context window in tokens", example = "128000")
    private int contextWindow;

    @Schema(description = "Maximum output tokens", example = "16384")
    private int maxOutputTokens;

    @Schema(description = "Model capabilities", example = "[\"chat\", \"function_calling\", \"vision\"]")
    private List<String> capabilities;

    @Schema(description = "Whether this model is currently available", example = "true")
    private boolean available;

    @Schema(description = "Model family/series", example = "gpt-4")
    private String family;
}