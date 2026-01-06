package com.neurogate.agentops.memory;

import java.util.List;

/**
 * AgentMemoryService - Facade for Managing Agent State
 */
public interface AgentMemoryService {

    /**
     * Store a message or observation in the agent's short-term memory
     */
    void storeShortTerm(String sessionId, String role, String content);

    /**
     * Retrieve the current context window for an agent session.
     * Guaranteed to be optimized/compressed if needed.
     */
    List<String> getContextWindow(String sessionId);

    /**
     * Compress memory for a session if it exceeds token limits.
     */
    void compressMemory(String sessionId);
}
