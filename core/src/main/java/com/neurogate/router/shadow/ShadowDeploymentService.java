package com.neurogate.router.shadow;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Shadow Deployment Service (Specter Mode)
 *
 * Executes shadow requests to alternative models for comparison without
 * affecting the primary response. Useful for A/B testing, model evaluation,
 * and gradual rollouts.
 */
@Slf4j
@Service
public class ShadowDeploymentService {

    /**
     * Execute a shadow request asynchronously.
     *
     * @param request The original request
     * @param shadowModel The model to use for the shadow request
     * @param routeFunction Function to route the shadow request
     */
    public void executeShadowRequest(
            ChatRequest request,
            String shadowModel,
            Function<ChatRequest, ChatResponse> routeFunction
    ) {
        if (shadowModel == null || shadowModel.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Specter Mode: Firing shadow request to {}", shadowModel);

                ChatRequest shadowRequest = ChatRequest.builder()
                        .model(shadowModel)
                        .messages(request.getMessages())
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .topP(request.getTopP())
                        .user(request.getUser())
                        .stream(false)
                        .build();

                ChatResponse shadowResponse = routeFunction.apply(shadowRequest);

                log.info("Specter Mode Result [{}]: Latency={}ms, Tokens={}",
                        shadowModel,
                        shadowResponse.getLatencyMs(),
                        shadowResponse.getUsage() != null ? shadowResponse.getUsage().getTotalTokens() : "N/A");

                recordShadowResult(request, shadowResponse, shadowModel);

            } catch (Exception e) {
                log.warn("Specter Mode Failed for {}: {}", shadowModel, e.getMessage());
            }
        });
    }

    /**
     * Record shadow deployment results for analysis.
     */
    private void recordShadowResult(ChatRequest request, ChatResponse response, String shadowModel) {
        // TODO: Persist to analytics for later comparison
        // Could integrate with ExperimentService or a dedicated shadow analytics table
    }
}
