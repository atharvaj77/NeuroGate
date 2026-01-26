package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Extension interface for providers that support streaming responses.
 *
 * <p>Not all LLM providers support streaming. Providers that do should
 * implement this interface in addition to {@link LLMProvider}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (provider instanceof StreamingProvider streaming) {
 *     Flux<ChatResponse> stream = streaming.generateStream(request);
 * }
 * }</pre>
 */
public interface StreamingProvider extends LLMProvider {

    /**
     * Generate a streaming chat completion.
     *
     * @param request the chat request
     * @return flux of response chunks (Server-Sent Events style)
     */
    Flux<ChatResponse> generateStream(ChatRequest request);

    /**
     * Check if streaming is currently available.
     * Some providers may temporarily disable streaming.
     */
    default boolean isStreamingAvailable() {
        return isAvailable();
    }
}
