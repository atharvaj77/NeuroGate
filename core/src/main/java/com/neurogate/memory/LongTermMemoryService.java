package com.neurogate.memory;

import com.neurogate.rag.client.VectorStoreClient;
import com.neurogate.rag.client.VectorStoreClient.ScoredPoint;
import com.neurogate.rag.client.VectorStoreClient.VectorPoint;
import com.neurogate.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Long-term memory backed by vector database.
 * Supports semantic search for relevant memories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryService implements MemoryService {

    private final VectorStoreClient vectorStoreClient;
    private final EmbeddingService embeddingService;

    private static final String COLLECTION_NAME = "agent_memory";

    @Override
    public String store(String sessionId, String content, Map<String, Object> metadata) {
        String id = UUID.randomUUID().toString();
        List<Double> vector = embeddingService.embed(content);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("sessionId", sessionId);
        payload.put("timestamp", System.currentTimeMillis());

        if (metadata != null) {
            payload.putAll(metadata);
        }

        VectorPoint point = new VectorPoint(id, vector, payload);
        vectorStoreClient.upsert(COLLECTION_NAME, List.of(point));

        log.debug("Stored long-term memory {}: {}", id, content.substring(0, Math.min(50, content.length())));
        return id;
    }

    @Override
    public List<String> search(String sessionId, String query, int limit) {
        List<Double> queryVector = embeddingService.embed(query);

        // Filter by sessionId if provided
        Map<String, Object> filter = sessionId != null
                ? Map.of("sessionId", sessionId)
                : null;

        List<ScoredPoint> results = vectorStoreClient.search(COLLECTION_NAME, queryVector, limit, filter);

        return results.stream()
                .map(p -> (String) p.payload().get("content"))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getContextWindow(String sessionId) {
        // For long-term memory, return recent entries by timestamp
        // This is a simplified implementation
        return search(sessionId, "", 20);
    }

    @Override
    public void clear(String sessionId) {
        // Vector stores typically don't support efficient deletion by filter
        // This would require a more complex implementation
        log.warn("Clear operation not fully implemented for long-term memory");
    }

    @Override
    public MemoryType getType() {
        return MemoryType.LONG_TERM;
    }
}
