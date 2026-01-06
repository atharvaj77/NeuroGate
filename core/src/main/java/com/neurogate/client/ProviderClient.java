package com.neurogate.client;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;

public interface ProviderClient {
    ChatResponse generate(ChatRequest request);

    String getProviderName();
}
