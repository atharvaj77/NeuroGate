package com.neurogate.rag.interceptor;

import com.neurogate.rag.service.NexusService;
import com.neurogate.sentinel.model.ChatRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Interceptor/Filter logic to be applied before routing.
 * Ideally, this would be part of a filter chain or called explicitly by the
 * Controller.
 * For NeuroGate architecture, we'll assume it's called by the Router or
 * Controller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagInterceptor {

    private final NexusService nexusService;

    public ChatRequest process(ChatRequest request, String userId) {
        if (Boolean.TRUE.equals(request.getRagEnabled())) {
            List<String> citations = nexusService.enrichRequest(request, userId);
            // We might want to store citations in a ThreadLocal or RequestContext
            // to attach them to the response later.
            // For now, we rely on NexusService modifying the request in place.
        }
        return request;
    }
}
