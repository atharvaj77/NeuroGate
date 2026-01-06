package com.neurogate.agent.memory;

import com.neurogate.rag.client.VectorStoreClient;
import com.neurogate.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMemoryService {

    private final VectorStoreClient vectorStoreClient;
    private final EmbeddingService embeddingService;
    private static final String MEMORY_COLLECTION = "agent_memory";

    public String save(StoreMemoryRequest request) {
        String id = request.getId() != null ? request.getId() : UUID.randomUUID().toString();
        List<Double> vector = embeddingService.embed(request.getContent());

        // Payload metadata
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", request.getContent());
        payload.put("timestamp", System.currentTimeMillis());
        if (request.getMetadata() != null) {
            payload.putAll(request.getMetadata());
        }

        VectorStoreClient.VectorPoint point = new VectorStoreClient.VectorPoint(id, vector, payload);
        vectorStoreClient.upsert(MEMORY_COLLECTION, List.of(point));
        log.debug("Saved memory trace: {}", id);
        return id;
    }

    public List<String> search(String query, int limit) {
        List<Double> queryVector = embeddingService.embed(query);
        List<VectorStoreClient.ScoredPoint> results = vectorStoreClient.search(MEMORY_COLLECTION, queryVector, limit,
                null);

        return results.stream()
                .map(p -> (String) p.payload().get("content"))
                .collect(Collectors.toList());
    }
}
