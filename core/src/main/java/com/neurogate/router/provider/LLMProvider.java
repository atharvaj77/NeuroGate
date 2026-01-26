package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Unified interface for all LLM providers in NeuroGate.
 *
 * <p>This interface defines the contract that all LLM provider implementations
 * must follow, enabling seamless integration with OpenAI, Anthropic, Google Gemini,
 * Azure OpenAI, AWS Bedrock, and other providers.</p>
 *
 * <h2>Implementation Guide</h2>
 *
 * <p>To add a new provider:</p>
 * <ol>
 *   <li>Implement this interface (or extend {@link AbstractLLMProvider})</li>
 *   <li>Annotate with {@code @Component} for auto-discovery</li>
 *   <li>Configure provider credentials in application.yml</li>
 * </ol>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * @Component
 * public class MyProvider implements LLMProvider {
 *     @Override
 *     public String getName() { return "my-provider"; }
 *
 *     @Override
 *     public ChatResponse generate(ChatRequest request) {
 *         // Call your LLM API
 *     }
 * }
 * }</pre>
 *
 * <h2>Extended Interfaces</h2>
 * <p>Providers may also implement additional capability interfaces:</p>
 * <ul>
 *   <li>{@link StreamingProvider} - For streaming response support</li>
 *   <li>{@link FunctionCallingProvider} - For tool/function calling</li>
 *   <li>{@link VisionProvider} - For image input support</li>
 *   <li>{@link EmbeddingProvider} - For text embeddings</li>
 * </ul>
 *
 * @see AbstractLLMProvider
 * @see ProviderMetadata
 * @see MultiProviderRouter
 */
public interface LLMProvider {

    /**
     * Get the unique provider identifier.
     *
     * <p>This name is used for:</p>
     * <ul>
     *   <li>Logging and metrics</li>
     *   <li>Circuit breaker identification</li>
     *   <li>Configuration lookup</li>
     *   <li>Route identification in responses</li>
     * </ul>
     *
     * @return provider name (e.g., "openai", "anthropic", "gemini")
     */
    String getName();

    /**
     * Get the list of model identifiers supported by this provider.
     *
     * <p>These should match the model IDs used in API requests.
     * For example: "gpt-4", "gpt-4o", "claude-3-5-sonnet-20241022".</p>
     *
     * @return list of supported model IDs
     */
    List<String> getSupportedModels();

    /**
     * Check if the provider is currently available and healthy.
     *
     * <p>A provider may be unavailable due to:</p>
     * <ul>
     *   <li>Missing API credentials</li>
     *   <li>Failed health check</li>
     *   <li>Circuit breaker open</li>
     *   <li>Explicit disable in configuration</li>
     * </ul>
     *
     * @return true if the provider can accept requests
     */
    boolean isAvailable();

    /**
     * Get provider metadata including cost and capability information.
     *
     * <p>Metadata is used for intelligent routing decisions, such as:</p>
     * <ul>
     *   <li>Cost-based routing (choosing cheapest provider)</li>
     *   <li>Latency-based routing (choosing fastest provider)</li>
     *   <li>Capability-based routing (choosing provider with needed features)</li>
     * </ul>
     *
     * @return provider metadata
     */
    ProviderMetadata getMetadata();

    /**
     * Generate a chat completion (non-streaming).
     *
     * <p>This is the primary method for getting LLM responses. The implementation
     * should handle:</p>
     * <ul>
     *   <li>API authentication</li>
     *   <li>Request/response format conversion</li>
     *   <li>Error handling and retries</li>
     *   <li>Rate limiting</li>
     * </ul>
     *
     * @param request the chat request containing model, messages, and parameters
     * @return the complete chat response
     * @throws com.neurogate.exception.ProviderException if the API call fails
     */
    ChatResponse generate(ChatRequest request);

    /**
     * Generate a streaming chat completion.
     *
     * <p>Returns a reactive stream of response chunks, suitable for
     * Server-Sent Events (SSE) delivery to clients.</p>
     *
     * <p>Note: Not all providers support streaming. Check provider capabilities
     * or implement {@link StreamingProvider} for streaming-specific logic.</p>
     *
     * @param request the chat request with stream=true
     * @return flux of response chunks
     * @throws UnsupportedOperationException if streaming is not supported
     */
    Flux<ChatResponse> generateStream(ChatRequest request);

    /**
     * Check if this provider supports the requested model.
     *
     * @param model the model identifier to check
     * @return true if the model is supported
     */
    default boolean supportsModel(String model) {
        return getSupportedModels().contains(model);
    }

    /**
     * Get an equivalent model from this provider for cross-provider fallback.
     *
     * <p>Used when the primary provider fails and requests need to be routed
     * to a fallback provider. Maps models across providers:</p>
     * <ul>
     *   <li>gpt-4 → claude-3-opus-20240229</li>
     *   <li>gpt-4o → claude-3-5-sonnet-20241022</li>
     *   <li>gpt-4o-mini → claude-3-5-haiku-20241022</li>
     * </ul>
     *
     * @param requestedModel the originally requested model
     * @return equivalent model ID for this provider, or the original if no mapping
     */
    String getEquivalentModel(String requestedModel);
}
