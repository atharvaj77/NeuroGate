package com.neurogate.router.intelligence;

import com.neurogate.router.intelligence.model.Intent;
import com.neurogate.router.intelligence.model.ModelRecommendation;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Configuration for intent-based routing.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "neurogate.intent-routing")
public class IntentRoutingConfig {

    /**
     * Whether intent routing is enabled.
     */
    private boolean enabled = true;

    /**
     * Minimum confidence threshold for applying intent routing.
     */
    private double confidenceThreshold = 0.6;

    /**
     * Intent to model mappings (loaded from config).
     */
    private Map<String, List<ModelConfig>> mappings = new HashMap<>();

    /**
     * Processed mappings with Intent keys.
     */
    private Map<Intent, List<ModelRecommendation>> intentMappings;

    @Data
    public static class ModelConfig {
        private String model;
        private int priority;
        private String reason;
    }

    @PostConstruct
    public void init() {
        intentMappings = new EnumMap<>(Intent.class);

        // Process configured mappings
        if (mappings != null) {
            for (Map.Entry<String, List<ModelConfig>> entry : mappings.entrySet()) {
                try {
                    Intent intent = Intent.valueOf(entry.getKey().toUpperCase());
                    List<ModelRecommendation> recommendations = entry.getValue().stream()
                            .map(mc -> ModelRecommendation.builder()
                                    .model(mc.getModel())
                                    .priority(mc.getPriority())
                                    .reason(mc.getReason())
                                    .available(true)
                                    .build())
                            .sorted(Comparator.comparingInt(ModelRecommendation::getPriority))
                            .toList();
                    intentMappings.put(intent, recommendations);
                } catch (IllegalArgumentException e) {
                    // Invalid intent name in config, skip
                }
            }
        }

        // Apply defaults for any unmapped intents
        applyDefaults();
    }

    /**
     * Apply default model mappings for intents not configured.
     */
    private void applyDefaults() {
        // Code Generation defaults
        intentMappings.computeIfAbsent(Intent.CODE_GENERATION, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(1)
                        .reason("Excellent code generation with function calling")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-sonnet-20241022")
                        .priority(2)
                        .reason("Strong reasoning for complex code")
                        .build()
        ));

        // Code Review defaults
        intentMappings.computeIfAbsent(Intent.CODE_REVIEW, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(1)
                        .reason("Detailed code analysis")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-sonnet-20241022")
                        .priority(2)
                        .reason("Thorough code review")
                        .build()
        ));

        // Code Explanation defaults
        intentMappings.computeIfAbsent(Intent.CODE_EXPLANATION, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o-mini")
                        .priority(1)
                        .reason("Cost-effective explanation")
                        .build(),
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(2)
                        .reason("Detailed explanations")
                        .build()
        ));

        // Reasoning defaults
        intentMappings.computeIfAbsent(Intent.REASONING, k -> List.of(
                ModelRecommendation.builder()
                        .model("o1-preview")
                        .priority(1)
                        .reason("Dedicated reasoning model with chain-of-thought")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-sonnet-20241022")
                        .priority(2)
                        .reason("Strong analytical capabilities")
                        .build()
        ));

        // Math/Science defaults
        intentMappings.computeIfAbsent(Intent.MATH_SCIENCE, k -> List.of(
                ModelRecommendation.builder()
                        .model("o1-preview")
                        .priority(1)
                        .reason("Excellent at mathematical reasoning")
                        .build(),
                ModelRecommendation.builder()
                        .model("gemini-1.5-pro")
                        .priority(2)
                        .reason("Strong STEM capabilities")
                        .build()
        ));

        // Creative Writing defaults
        intentMappings.computeIfAbsent(Intent.CREATIVE_WRITING, k -> List.of(
                ModelRecommendation.builder()
                        .model("claude-3-opus-20240229")
                        .priority(1)
                        .reason("Best creative output quality")
                        .build(),
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(2)
                        .reason("Versatile creative capabilities")
                        .build()
        ));

        // Summarization defaults
        intentMappings.computeIfAbsent(Intent.SUMMARIZATION, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o-mini")
                        .priority(1)
                        .reason("Fast and cost-effective")
                        .build(),
                ModelRecommendation.builder()
                        .model("gemini-1.5-flash")
                        .priority(2)
                        .reason("Large context window for long texts")
                        .build()
        ));

        // Translation defaults
        intentMappings.computeIfAbsent(Intent.TRANSLATION, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o-mini")
                        .priority(1)
                        .reason("Excellent multilingual support")
                        .build(),
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(2)
                        .reason("High-quality translations")
                        .build()
        ));

        // Q&A defaults
        intentMappings.computeIfAbsent(Intent.QUESTION_ANSWERING, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o-mini")
                        .priority(1)
                        .reason("Fast factual responses")
                        .build(),
                ModelRecommendation.builder()
                        .model("gemini-1.5-flash")
                        .priority(2)
                        .reason("Quick and accurate")
                        .build()
        ));

        // Data Analysis defaults
        intentMappings.computeIfAbsent(Intent.DATA_ANALYSIS, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(1)
                        .reason("Strong data analysis capabilities")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-sonnet-20241022")
                        .priority(2)
                        .reason("Detailed data insights")
                        .build()
        ));

        // Conversation defaults
        intentMappings.computeIfAbsent(Intent.CONVERSATION, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o-mini")
                        .priority(1)
                        .reason("Fast conversational responses")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-haiku-20241022")
                        .priority(2)
                        .reason("Efficient chat model")
                        .build()
        ));

        // Instruction Following defaults
        intentMappings.computeIfAbsent(Intent.INSTRUCTION_FOLLOWING, k -> List.of(
                ModelRecommendation.builder()
                        .model("gpt-4o")
                        .priority(1)
                        .reason("Reliable instruction following")
                        .build(),
                ModelRecommendation.builder()
                        .model("claude-3-5-sonnet-20241022")
                        .priority(2)
                        .reason("Precise task execution")
                        .build()
        ));
    }

    /**
     * Get recommendations for an intent.
     */
    public List<ModelRecommendation> getRecommendations(Intent intent) {
        return intentMappings.getOrDefault(intent, List.of());
    }
}