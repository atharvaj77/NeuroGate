package com.neurogate.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composite memory service that combines short-term and long-term memory.
 * Short-term memory holds recent context, long-term memory enables semantic recall.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompositeMemoryService implements MemoryService {

    private final ShortTermMemoryService shortTermMemory;
    private final LongTermMemoryService longTermMemory;

    @Override
    public String store(String sessionId, String content, Map<String, Object> metadata) {
        // Store in both short-term and long-term memory
        String stmId = shortTermMemory.store(sessionId, content, metadata);
        String ltmId = longTermMemory.store(sessionId, content, metadata);

        log.debug("Stored in composite memory: STM={}, LTM={}", stmId, ltmId);
        return stmId; // Return short-term ID as primary
    }

    @Override
    public List<String> search(String sessionId, String query, int limit) {
        // Search long-term memory for semantic matches
        return longTermMemory.search(sessionId, query, limit);
    }

    @Override
    public List<String> getContextWindow(String sessionId) {
        // Get recent context from short-term memory
        return shortTermMemory.getContextWindow(sessionId);
    }

    /**
     * Get context augmented with relevant long-term memories.
     *
     * @param sessionId the session identifier
     * @param query the current query for relevance matching
     * @param shortTermLimit max short-term entries
     * @param longTermLimit max long-term entries
     * @return combined memory entries
     */
    public List<String> getAugmentedContext(String sessionId, String query,
                                             int shortTermLimit, int longTermLimit) {
        List<String> context = new ArrayList<>();

        // Add recent short-term memory
        List<String> recentContext = shortTermMemory.getContextWindow(sessionId);
        context.addAll(recentContext.stream().limit(shortTermLimit).toList());

        // Add relevant long-term memories
        if (query != null && !query.isBlank()) {
            List<String> relevantMemories = longTermMemory.search(sessionId, query, longTermLimit);
            context.addAll(relevantMemories);
        }

        return context;
    }

    @Override
    public void clear(String sessionId) {
        shortTermMemory.clear(sessionId);
        longTermMemory.clear(sessionId);
        log.debug("Cleared composite memory for session {}", sessionId);
    }

    @Override
    public MemoryType getType() {
        return MemoryType.COMPOSITE;
    }

    /**
     * Get the short-term memory service.
     */
    public ShortTermMemoryService getShortTermMemory() {
        return shortTermMemory;
    }

    /**
     * Get the long-term memory service.
     */
    public LongTermMemoryService getLongTermMemory() {
        return longTermMemory;
    }
}
