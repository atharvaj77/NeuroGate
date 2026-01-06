package com.neurogate.client.adapters;

import com.neurogate.client.ProviderClient;
import com.neurogate.router.upstream.BedrockClient;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BedrockProviderClient implements ProviderClient {

    private final BedrockClient bedrockClient;

    @Override
    public ChatResponse generate(ChatRequest request) {
        // Use the model from the request, or default to anthropic.claude-v2 if null
        String model = request.getModel() != null ? request.getModel() : "anthropic.claude-v2";
        return bedrockClient.generateCompletion(request, model);
    }

    @Override
    public String getProviderName() {
        return "bedrock";
    }
}
