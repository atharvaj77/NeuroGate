package com.neurogate.memory;

import com.neurogate.agentops.memory.MemoryCompressor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Short-term memory backed by Redis.
 * Suitable for session-based conversation history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortTermMemoryService implements MemoryService {

    private final StringRedisTemplate redisTemplate;
    private final MemoryCompressor memoryCompressor;

    private static final String KEY_PREFIX = "memory:stm:";
    private static final int MAX_ENTRIES_BEFORE_COMPRESS = 20;

    @Override
    public String store(String sessionId, String content, Map<String, Object> metadata) {
        String key = KEY_PREFIX + sessionId;
        String id = UUID.randomUUID().toString();

        String role = metadata != null ? (String) metadata.getOrDefault("role", "user") : "user";
        String entry = role + ": " + content;

        redisTemplate.opsForList().rightPush(key, entry);
        log.debug("Stored short-term memory for session {}: {}", sessionId, id);

        // Auto-compress if exceeding threshold
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_ENTRIES_BEFORE_COMPRESS) {
            compress(sessionId);
        }

        return id;
    }

    @Override
    public List<String> search(String sessionId, String query, int limit) {
        // Short-term memory doesn't support semantic search
        // Return recent entries instead
        return getContextWindow(sessionId).stream()
                .limit(limit)
                .toList();
    }

    @Override
    public List<String> getContextWindow(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> entries = redisTemplate.opsForList().range(key, 0, -1);

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        return memoryCompressor.compress(entries);
    }

    @Override
    public void clear(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.debug("Cleared short-term memory for session {}", sessionId);
    }

    @Override
    public MemoryType getType() {
        return MemoryType.SHORT_TERM;
    }

    /**
     * Compress memory for a session.
     */
    public void compress(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> entries = redisTemplate.opsForList().range(key, 0, -1);

        if (entries != null && entries.size() > 10) {
            List<String> compressed = memoryCompressor.compress(entries);
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, compressed);
            log.debug("Compressed memory for session {}: {} → {} entries",
                    sessionId, entries.size(), compressed.size());
        }
    }
}
