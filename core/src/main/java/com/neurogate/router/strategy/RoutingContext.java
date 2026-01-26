package com.neurogate.router.strategy;

import com.neurogate.sentinel.model.ChatRequest;
import lombok.Builder;
import lombok.Data;

/**
 * Context object for routing decisions.
 * Carries request and metadata through the routing pipeline.
 */
@Data
@Builder(toBuilder = true)
public class RoutingContext {

    private final ChatRequest originalRequest;
    private ChatRequest currentRequest;
    private String selectedModel;
    private String selectedProvider;
    private String routingReason;
    private boolean intentRoutingApplied;
    private boolean experimentRoutingApplied;
    private String experimentId;
    private Object experimentVariant;
    private long startTimeMs;

    /**
     * Create initial context from a request.
     */
    public static RoutingContext from(ChatRequest request) {
        return RoutingContext.builder()
                .originalRequest(request)
                .currentRequest(request)
                .selectedModel(request.getModel())
                .startTimeMs(System.currentTimeMillis())
                .build();
    }

    /**
     * Update the model selection.
     */
    public RoutingContext withModel(String model, String reason) {
        return this.toBuilder()
                .selectedModel(model)
                .currentRequest(currentRequest.toBuilder().model(model).build())
                .routingReason(reason)
                .build();
    }
}
