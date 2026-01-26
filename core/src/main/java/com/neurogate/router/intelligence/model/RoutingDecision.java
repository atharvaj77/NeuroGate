package com.neurogate.router.intelligence.model;

import com.neurogate.sentinel.model.ChatRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The result of intent-based routing decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDecision {

    /**
     * The original request.
     */
    private ChatRequest originalRequest;

    /**
     * The model to use (may differ from original if intent routing applied).
     */
    private String selectedModel;

    /**
     * Whether intent routing was applied.
     */
    private boolean intentRoutingApplied;

    /**
     * The detected intent (if routing was applied).
     */
    private Intent intent;

    /**
     * Confidence in the intent classification.
     */
    private double confidence;

    /**
     * Reason for the routing decision.
     */
    private String reason;

    /**
     * Create a passthrough decision (no routing change).
     */
    public static RoutingDecision passthrough(ChatRequest request) {
        return RoutingDecision.builder()
                .originalRequest(request)
                .selectedModel(request.getModel())
                .intentRoutingApplied(false)
                .reason("No intent routing applied")
                .build();
    }

    /**
     * Create a routed decision.
     */
    public static RoutingDecision routed(
            ChatRequest request,
            String selectedModel,
            Intent intent,
            double confidence,
            String reason
    ) {
        return RoutingDecision.builder()
                .originalRequest(request)
                .selectedModel(selectedModel)
                .intentRoutingApplied(true)
                .intent(intent)
                .confidence(confidence)
                .reason(reason)
                .build();
    }
}