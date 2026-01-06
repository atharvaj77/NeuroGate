package com.neurogate.analytics;

import com.neurogate.config.PricingConfig;
import com.neurogate.router.intelligence.ComplexityAnalyzer;
import com.neurogate.router.intelligence.ComplexityScore;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Tracks usage and calculates costs for chargeback reporting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostTrackingService {

    private final UsageRecordRepository usageRecordRepository;
    private final ComplexityAnalyzer complexityAnalyzer;
    private final PricingConfig pricingConfig;

    /**
     * Record usage for a completed request
     *
     * @param request   Original request
     * @param response  Response from LLM
     * @param userId    User identifier
     * @param teamId    Team identifier (optional)
     * @param projectId Project identifier (optional)
     * @param latencyMs Request latency in milliseconds
     */
    public void recordUsage(ChatRequest request, ChatResponse response, String userId,
            String teamId, String projectId, long latencyMs) {
        try {
            // Calculate cost
            BigDecimal cost = calculateCost(response);

            // Get complexity score
            ComplexityScore complexityScore = complexityAnalyzer.analyze(request.getConcatenatedContent());

            // Create usage record
            UsageRecord record = UsageRecord.builder()
                    .userId(userId != null ? userId : "anonymous")
                    .teamId(teamId)
                    .projectId(projectId)
                    .provider(extractProvider(response.getRoute()))
                    .model(response.getModel())
                    .promptTokens(response.getUsage() != null ? response.getUsage().getPromptTokens() : 0)
                    .completionTokens(response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0)
                    .totalTokens(response.getUsage() != null ? response.getUsage().getTotalTokens() : 0)
                    .costUsd(cost)
                    .cacheHit(response.getCacheHit() != null ? response.getCacheHit() : false)
                    .timestamp(Instant.now())
                    .requestId(response.getId())
                    .latencyMs(latencyMs)
                    .complexityScore(complexityScore.getOverallScore())
                    .build();

            // Save to database
            usageRecordRepository.save(record);

            log.debug("Usage recorded: user={}, model={}, cost=${}, tokens={}",
                    userId, response.getModel(), cost, record.getTotalTokens());

        } catch (Exception e) {
            log.error("Failed to record usage", e);
            // Don't fail the request if usage tracking fails
        }
    }

    /**
     * Calculate cost for a response
     */
    private BigDecimal calculateCost(ChatResponse response) {
        if (response.getCacheHit() != null && response.getCacheHit()) {
            return BigDecimal.ZERO; // Cache hits are free
        }

        String model = response.getModel();
        BigDecimal[] costs = pricingConfig.getCostForModel(model);

        if (response.getUsage() == null) {
            return BigDecimal.ZERO;
        }

        // Cost = (inputTokens / 1000 * inputCost) + (outputTokens / 1000 * outputCost)
        BigDecimal inputCost = new BigDecimal(response.getUsage().getPromptTokens())
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .multiply(costs[0]);

        BigDecimal outputCost = new BigDecimal(response.getUsage().getCompletionTokens())
                .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP)
                .multiply(costs[1]);

        return inputCost.add(outputCost);
    }

    /**
     * Extract provider from route string
     */
    private String extractProvider(String route) {
        if (route == null) {
            return "unknown";
        }

        if (route.contains("openai"))
            return "openai";
        if (route.contains("anthropic"))
            return "anthropic";
        if (route.contains("gemini"))
            return "gemini";
        if (route.contains("cache"))
            return "cache";
        if (route.contains("local"))
            return "local";

        return "unknown";
    }
}
