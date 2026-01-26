package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Tool;

import java.util.List;

/**
 * Extension interface for providers that support function/tool calling.
 *
 * <p>Function calling allows the model to request execution of external
 * functions and use their results in generating responses.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (provider instanceof FunctionCallingProvider fc) {
 *     List<Tool> tools = List.of(weatherTool, calculatorTool);
 *     ChatResponse response = fc.generateWithTools(request, tools);
 * }
 * }</pre>
 */
public interface FunctionCallingProvider extends LLMProvider {

    /**
     * Generate a response with tool/function calling support.
     *
     * @param request the chat request
     * @param tools the available tools the model can call
     * @return response potentially containing tool calls
     */
    ChatResponse generateWithTools(ChatRequest request, List<Tool> tools);

    /**
     * Check if function calling is supported for the given model.
     *
     * @param model the model identifier
     * @return true if the model supports function calling
     */
    boolean supportsFunctionCalling(String model);

    /**
     * Get the maximum number of tools that can be provided.
     */
    default int getMaxTools() {
        return 128; // OpenAI default
    }
}
