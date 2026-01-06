package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Unified interface for all LLM providers (OpenAI, Anthropic, Google Gemini,
 * etc.)
 */
public interface LLMProvider {

    /**
     * Get the provider name (e.g., "openai", "anthropic", "gemini")
     */
    String getName();

    /**
     * Get list of models supported by this provider
     */
    List<String> getSupportedModels();

    /**
     * Check if the provider is available and healthy
     */
    boolean isAvailable();

    /**
     * Get provider metadata (cost, latency, capabilities)
     */
    ProviderMetadata getMetadata();

    /**
     * Generate a chat completion (non-streaming)
     *
     * @param request The chat request
     * @return The chat response
     */
    ChatResponse generate(ChatRequest request);

    /**
     * Generate a streaming chat completion (Server-Sent Events)
     *
     * @param request The chat request
     * @return Flux of response chunks
     */
    Flux<ChatResponse> generateStream(ChatRequest request);

    /**
     * Check if this provider supports the requested model
     */
    default boolean supportsModel(String model) {
        return getSupportedModels().contains(model);
    }

    /**
     * Get equivalent model from this provider for the requested model
     * For example: gpt-4 -> claude-3-opus-20240229
     */
    String getEquivalentModel(String requestedModel);
}
