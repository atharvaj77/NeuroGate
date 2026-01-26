package com.neurogate.memory;

import java.util.List;
import java.util.Map;

/**
 * Unified interface for agent memory operations.
 * Supports both short-term (session) and long-term (vector) memory.
 */
public interface MemoryService {

    /**
     * Store content in memory.
     *
     * @param sessionId the session/agent identifier
     * @param content the content to store
     * @param metadata optional metadata
     * @return the memory entry ID
     */
    String store(String sessionId, String content, Map<String, Object> metadata);

    /**
     * Search memory for relevant content.
     *
     * @param sessionId the session/agent identifier
     * @param query the search query
     * @param limit maximum results to return
     * @return list of relevant content strings
     */
    List<String> search(String sessionId, String query, int limit);

    /**
     * Get all memory for a session within the context window.
     *
     * @param sessionId the session identifier
     * @return list of memory entries
     */
    List<String> getContextWindow(String sessionId);

    /**
     * Clear memory for a session.
     *
     * @param sessionId the session identifier
     */
    void clear(String sessionId);

    /**
     * Get the type of this memory service.
     */
    MemoryType getType();

    /**
     * Memory storage type.
     */
    enum MemoryType {
        SHORT_TERM,  // Redis-based session memory
        LONG_TERM,   // Vector-based semantic memory
        COMPOSITE    // Combines both types
    }
}
