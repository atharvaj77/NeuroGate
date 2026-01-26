package com.neurogate.router.intelligence;

import com.neurogate.router.intelligence.model.*;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Routes requests to optimal models based on intent classification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouter {

    private final IntentClassifier classifier;
    private final IntentRoutingConfig config;

    /**
     * Analyze request and determine optimal routing.
     *
     * @param request The chat request
     * @return Routing decision with selected model
     */
    public RoutingDecision route(ChatRequest request) {
        if (!config.isEnabled()) {
            log.debug("Intent routing disabled, passing through");
            return RoutingDecision.passthrough(request);
        }

        // Check for intent override header
        String intentOverride = request.getIntentOverride();
        if (intentOverride != null && !intentOverride.isBlank()) {
            try {
                Intent overrideIntent = Intent.valueOf(intentOverride.toUpperCase());
                log.info("🎯 Intent override: {}", overrideIntent);
                return routeToIntent(request, overrideIntent, 1.0);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid intent override: {}", intentOverride);
            }
        }

        // Extract prompt text for classification
        String promptText = extractPromptText(request);
        if (promptText.isBlank()) {
            return RoutingDecision.passthrough(request);
        }

        // Classify intent
        IntentClassification classification = classifier.classify(promptText);

        log.debug("🎯 Intent classified: {} (confidence: {:.2f})",
                classification.getIntent(), classification.getConfidence());

        // Only apply routing if confidence meets threshold
        if (classification.getConfidence() < config.getConfidenceThreshold()) {
            log.debug("Confidence {:.2f} below threshold {:.2f}, passing through",
                    classification.getConfidence(), config.getConfidenceThreshold());
            return RoutingDecision.passthrough(request);
        }

        return routeToIntent(request, classification.getIntent(), classification.getConfidence());
    }

    /**
     * Route to a specific intent's recommended model.
     */
    private RoutingDecision routeToIntent(ChatRequest request, Intent intent, double confidence) {
        List<ModelRecommendation> recommendations = config.getRecommendations(intent);

        if (recommendations.isEmpty()) {
            log.warn("No model recommendations for intent: {}", intent);
            return RoutingDecision.passthrough(request);
        }

        // Get first available recommendation
        ModelRecommendation selected = recommendations.stream()
                .filter(ModelRecommendation::isAvailable)
                .findFirst()
                .orElse(recommendations.get(0));

        // Don't change if already using an optimal model
        if (request.getModel() != null && request.getModel().equals(selected.getModel())) {
            log.debug("Already using optimal model {} for intent {}", selected.getModel(), intent);
            return RoutingDecision.builder()
                    .originalRequest(request)
                    .selectedModel(selected.getModel())
                    .intentRoutingApplied(false)
                    .intent(intent)
                    .confidence(confidence)
                    .reason("Already using optimal model")
                    .build();
        }

        log.info("🎯 Intent routing: {} → {} ({})",
                intent, selected.getModel(), selected.getReason());

        return RoutingDecision.routed(
                request,
                selected.getModel(),
                intent,
                confidence,
                selected.getReason()
        );
    }

    /**
     * Extract the user's prompt text from the request.
     */
    private String extractPromptText(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }

        // Get the last user message
        StringBuilder sb = new StringBuilder();
        for (Message msg : request.getMessages()) {
            if ("user".equals(msg.getRole())) {
                String content = msg.getStrContent();
                if (content != null) {
                    sb.append(content).append(" ");
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Get classification for a prompt without routing.
     */
    public IntentClassification classifyIntent(String prompt) {
        return classifier.classify(prompt);
    }

    /**
     * Get model recommendations for an intent.
     */
    public List<ModelRecommendation> getRecommendations(Intent intent) {
        return config.getRecommendations(intent);
    }

    /**
     * Check if intent routing is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
}