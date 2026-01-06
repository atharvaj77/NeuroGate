package com.neurogate.client.adapters;

import com.neurogate.client.ProviderClient;
import com.neurogate.router.upstream.GeminiClient;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeminiProviderClient implements ProviderClient {

    private final GeminiClient geminiClient;

    @Override
    public ChatResponse generate(ChatRequest request) {
        // Use the model from the request, or default to gemini-pro if null
        String model = request.getModel() != null ? request.getModel() : "gemini-pro";
        return geminiClient.generateCompletion(request, model);
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
