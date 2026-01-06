package com.neurogate.agentops.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisMemoryStore implements AgentMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final MemoryCompressor memoryCompressor;

    private static final String PRIX_STM = "agent:stm:";

    @Override
    public void storeShortTerm(String sessionId, String role, String content) {
        String key = PRIX_STM + sessionId;
        String entry = role + ": " + content;
        redisTemplate.opsForList().rightPush(key, entry);

        // Naive auto-compress on write (in prod, do this async)
        if (redisTemplate.opsForList().size(key) > 20) {
            compressMemory(sessionId);
        }
    }

    @Override
    public List<String> getContextWindow(String sessionId) {
        String key = PRIX_STM + sessionId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null)
            return List.of();

        // Read-time compression check (optional, but good for safety)
        return memoryCompressor.compress(raw);
    }

    @Override
    public void compressMemory(String sessionId) {
        String key = PRIX_STM + sessionId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw != null && raw.size() > 10) {
            List<String> compressed = memoryCompressor.compress(raw);

            // Atomic replacement would be better (Lua script), but for now:
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, compressed);
        }
    }
}
