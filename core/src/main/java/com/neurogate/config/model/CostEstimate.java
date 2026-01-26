package com.neurogate.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cost estimate for a request.
 */
@Data
@Builder
@Schema(description = "Estimated cost for a request")
public class CostEstimate {

    @Schema(description = "Model used for estimate", example = "gpt-4o")
    private String model;

    @Schema(description = "Estimated input tokens", example = "1000")
    private int estimatedInputTokens;

    @Schema(description = "Estimated output tokens", example = "500")
    private int estimatedOutputTokens;

    @Schema(description = "Input cost in USD", example = "0.0025")
    private BigDecimal inputCost;

    @Schema(description = "Output cost in USD", example = "0.005")
    private BigDecimal outputCost;

    @Schema(description = "Total estimated cost in USD", example = "0.0075")
    private BigDecimal totalCost;

    @Schema(description = "Cost if served from cache (typically $0)", example = "0.0")
    private BigDecimal cacheHitCost;

    @Schema(description = "Potential savings if cached", example = "0.0075")
    private BigDecimal potentialSavings;
}