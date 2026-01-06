package com.neurogate.client.adapters;

import com.neurogate.client.ProviderClient;
import com.neurogate.router.upstream.OpenAiClient;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiProviderClient implements ProviderClient {

    private final OpenAiClient openAiClient;

    @Override
    public ChatResponse generate(ChatRequest request) {
        return openAiClient.generateCompletion(request);
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}
