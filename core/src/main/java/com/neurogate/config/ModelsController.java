package com.neurogate.config;

import com.neurogate.config.model.CostEstimate;
import com.neurogate.config.model.EstimateRequest;
import com.neurogate.config.model.ModelInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * REST API for model information and pricing.
 * OpenAI-compatible endpoint at /v1/models.
 */
@Slf4j
@RestController
@RequestMapping("/v1/models")
@RequiredArgsConstructor
@Tag(name = "Models", description = "Model information and pricing")
public class ModelsController {

    private final PricingConfig pricingConfig;

    @Operation(summary = "List all models", description = "Get all available models with their information")
    @ApiResponse(responseCode = "200", description = "Models retrieved")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listModels(
            @Parameter(description = "Filter by provider") @RequestParam(required = false) String provider) {

        List<ModelInfo> models;
        if (provider != null && !provider.isEmpty()) {
            models = pricingConfig.getModelsByProvider(provider);
        } else {
            models = pricingConfig.getAllModels();
        }

        // OpenAI-compatible response format
        return ResponseEntity.ok(Map.of(
                "object", "list",
                "data", models
        ));
    }

    @Operation(summary = "Get model details", description = "Get detailed information about a specific model")
    @ApiResponse(responseCode = "200", description = "Model found")
    @ApiResponse(responseCode = "404", description = "Model not found")
    @GetMapping("/{modelId}")
    public ResponseEntity<ModelInfo> getModel(
            @Parameter(description = "Model ID") @PathVariable String modelId) {

        ModelInfo model = pricingConfig.getModelInfo(modelId);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(model);
    }

    @Operation(summary = "Get model pricing", description = "Get pricing information for a specific model")
    @ApiResponse(responseCode = "200", description = "Pricing retrieved")
    @ApiResponse(responseCode = "404", description = "Model not found")
    @GetMapping("/{modelId}/pricing")
    public ResponseEntity<Map<String, Object>> getModelPricing(
            @Parameter(description = "Model ID") @PathVariable String modelId) {

        ModelInfo model = pricingConfig.getModelInfo(modelId);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "model", modelId,
                "provider", model.getProvider(),
                "currency", "USD",
                "input_cost_per_1k_tokens", model.getInputCostPer1k(),
                "output_cost_per_1k_tokens", model.getOutputCostPer1k(),
                "context_window", model.getContextWindow(),
                "max_output_tokens", model.getMaxOutputTokens()
        ));
    }

    @Operation(summary = "Estimate request cost", description = "Estimate the cost for a request based on input/output tokens")
    @ApiResponse(responseCode = "200", description = "Estimate calculated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/estimate")
    public ResponseEntity<CostEstimate> estimateCost(@RequestBody EstimateRequest request) {

        if (request.getModel() == null) {
            return ResponseEntity.badRequest().build();
        }

        ModelInfo model = pricingConfig.getModelInfo(request.getModel());
        BigDecimal inputCostPer1k;
        BigDecimal outputCostPer1k;

        if (model != null) {
            inputCostPer1k = model.getInputCostPer1k();
            outputCostPer1k = model.getOutputCostPer1k();
        } else {
            // Fallback to costs map
            BigDecimal[] costs = pricingConfig.getCostForModel(request.getModel());
            inputCostPer1k = costs[0];
            outputCostPer1k = costs[1];
        }

        // Estimate input tokens if not provided
        int inputTokens = request.getInputTokens() != null
                ? request.getInputTokens()
                : estimateTokens(request.getInputText());

        int outputTokens = request.getExpectedOutputTokens() != null
                ? request.getExpectedOutputTokens()
                : 500; // Default expected output

        // Calculate costs
        BigDecimal inputCost = inputCostPer1k
                .multiply(new BigDecimal(inputTokens))
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputCostPer1k
                .multiply(new BigDecimal(outputTokens))
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);

        BigDecimal totalCost = inputCost.add(outputCost);

        return ResponseEntity.ok(CostEstimate.builder()
                .model(request.getModel())
                .estimatedInputTokens(inputTokens)
                .estimatedOutputTokens(outputTokens)
                .inputCost(inputCost)
                .outputCost(outputCost)
                .totalCost(totalCost)
                .cacheHitCost(BigDecimal.ZERO)
                .potentialSavings(totalCost)
                .build());
    }

    @Operation(summary = "Compare model costs", description = "Compare costs across multiple models for the same request")
    @ApiResponse(responseCode = "200", description = "Comparison generated")
    @PostMapping("/compare")
    public ResponseEntity<List<CostEstimate>> compareModels(
            @RequestBody EstimateRequest request,
            @Parameter(description = "Models to compare (comma-separated)")
            @RequestParam(defaultValue = "gpt-4o,gpt-4o-mini,claude-3-5-sonnet-20241022,gemini-1.5-pro") String models) {

        int inputTokens = request.getInputTokens() != null
                ? request.getInputTokens()
                : estimateTokens(request.getInputText());

        int outputTokens = request.getExpectedOutputTokens() != null
                ? request.getExpectedOutputTokens()
                : 500;

        List<CostEstimate> estimates = java.util.Arrays.stream(models.split(","))
                .map(String::trim)
                .map(modelId -> {
                    BigDecimal[] costs = pricingConfig.getCostForModel(modelId);
                    BigDecimal inputCost = costs[0]
                            .multiply(new BigDecimal(inputTokens))
                            .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
                    BigDecimal outputCost = costs[1]
                            .multiply(new BigDecimal(outputTokens))
                            .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
                    BigDecimal totalCost = inputCost.add(outputCost);

                    return CostEstimate.builder()
                            .model(modelId)
                            .estimatedInputTokens(inputTokens)
                            .estimatedOutputTokens(outputTokens)
                            .inputCost(inputCost)
                            .outputCost(outputCost)
                            .totalCost(totalCost)
                            .cacheHitCost(BigDecimal.ZERO)
                            .potentialSavings(totalCost)
                            .build();
                })
                .sorted((a, b) -> a.getTotalCost().compareTo(b.getTotalCost()))
                .toList();

        return ResponseEntity.ok(estimates);
    }

    /**
     * Rough token estimation: ~4 characters per token for English.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 100; // Default
        }
        return Math.max(1, text.length() / 4);
    }
}