package com.neurogate.router.cache;

import com.neurogate.sentinel.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Optimized cache key generation for request caching.
 *
 * <p>Uses fast non-cryptographic hashing (MurmurHash3-inspired) instead of
 * SHA-256 for better performance. Cache keys don't require cryptographic
 * security - they just need to be fast and have good distribution.</p>
 *
 * <p>Performance comparison (typical):</p>
 * <ul>
 *   <li>SHA-256: ~500ns per hash</li>
 *   <li>MurmurHash3: ~50ns per hash (10x faster)</li>
 * </ul>
 */
@Slf4j
@Component
public class CacheKeyGenerator {

    private static final String KEY_PREFIX = "neurogate:cache:";
    private static final long SEED = 0x9747b28cL;

    /**
     * Generate a cache key from a chat request.
     *
     * @param request the chat request
     * @return the cache key
     */
    public String generate(ChatRequest request) {
        String content = buildCacheContent(request);
        long hash = murmurHash64(content);
        return KEY_PREFIX + Long.toHexString(hash);
    }

    /**
     * Generate a cache key from raw content.
     *
     * @param content the content to hash
     * @return the cache key
     */
    public String generate(String content) {
        long hash = murmurHash64(content);
        return KEY_PREFIX + Long.toHexString(hash);
    }

    /**
     * Build the content string for caching.
     * Includes model and messages to ensure cache correctness.
     */
    private String buildCacheContent(ChatRequest request) {
        StringBuilder sb = new StringBuilder();

        // Include model in cache key
        if (request.getModel() != null) {
            sb.append("model:").append(request.getModel()).append("|");
        }

        // Include temperature if set (affects output)
        if (request.getTemperature() != null) {
            sb.append("temp:").append(request.getTemperature()).append("|");
        }

        // Include message content
        sb.append(request.getConcatenatedContent());

        return sb.toString();
    }

    /**
     * MurmurHash3-inspired 64-bit hash function.
     * Fast, non-cryptographic hash with excellent distribution.
     */
    private long murmurHash64(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        long h = SEED ^ (length * 0x85ebca6bL);

        int i = 0;
        while (i + 8 <= length) {
            long k = getLong(bytes, i);
            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
            h = Long.rotateLeft(h, 27);
            h = h * 5 + 0x52dce729;
            i += 8;
        }

        // Process remaining bytes
        long k = 0;
        int shift = 0;
        while (i < length) {
            k |= (long) (bytes[i] & 0xff) << shift;
            shift += 8;
            i++;
        }

        if (shift > 0) {
            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
        }

        // Finalization
        h ^= length;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;

        return h;
    }

    /**
     * Read 8 bytes as a long (little-endian).
     */
    private long getLong(byte[] bytes, int offset) {
        return (long) (bytes[offset] & 0xff)
                | ((long) (bytes[offset + 1] & 0xff) << 8)
                | ((long) (bytes[offset + 2] & 0xff) << 16)
                | ((long) (bytes[offset + 3] & 0xff) << 24)
                | ((long) (bytes[offset + 4] & 0xff) << 32)
                | ((long) (bytes[offset + 5] & 0xff) << 40)
                | ((long) (bytes[offset + 6] & 0xff) << 48)
                | ((long) (bytes[offset + 7] & 0xff) << 56);
    }

    /**
     * Generate a semantic cache key for similarity-based caching.
     * This combines content hash with an embedding-based prefix.
     *
     * @param request the chat request
     * @param embeddingPrefix first few dimensions of embedding (for locality)
     * @return semantic cache key
     */
    public String generateSemantic(ChatRequest request, double[] embeddingPrefix) {
        StringBuilder sb = new StringBuilder(KEY_PREFIX).append("sem:");

        // Add quantized embedding prefix for locality-sensitive hashing
        for (int i = 0; i < Math.min(4, embeddingPrefix.length); i++) {
            int quantized = (int) (embeddingPrefix[i] * 100);
            sb.append(Integer.toHexString(quantized & 0xff));
        }

        sb.append(":");
        sb.append(Long.toHexString(murmurHash64(request.getConcatenatedContent())));

        return sb.toString();
    }
}
