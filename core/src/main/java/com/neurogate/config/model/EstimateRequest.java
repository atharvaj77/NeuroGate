package com.neurogate.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request for cost estimation.
 */
@Data
@Schema(description = "Cost estimation request")
public class EstimateRequest {

    @Schema(description = "Model to estimate for", example = "gpt-4o", required = true)
    private String model;

    @Schema(description = "Input text or prompt", example = "What is the capital of France?")
    private String inputText;

    @Schema(description = "Estimated input tokens (if known)", example = "100")
    private Integer inputTokens;

    @Schema(description = "Expected output tokens", example = "500")
    private Integer expectedOutputTokens;
}