package com.neurogate.vault.tokenizer;

import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request-scoped vault for storing PII token mappings.
 */
@Slf4j
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TokenVault {

    // Token format: <EMAIL_1>, <SSN_2>, etc.
    private static final String TOKEN_FORMAT = "<%s_%d>";

    // Maps: token -> original value
    private final Map<String, String> tokenToValue = new ConcurrentHashMap<>();

    // Maps: original value -> token (for idempotency)
    private final Map<String, String> valueToToken = new ConcurrentHashMap<>();

    // Counters for each PII type
    private final Map<PiiType, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * Create a token for a PII entity
     *
     * @param entity The PII entity to tokenize
     * @return The generated token
     */
    public String tokenize(PiiEntity entity) {
        // Check if this value was already tokenized (idempotency)
        String existingToken = valueToToken.get(entity.getValue());
        if (existingToken != null) {
            log.debug("Reusing existing token for {}: {}", entity.getType(), existingToken);
            return existingToken;
        }

        // Generate new token
        int counter = counters.computeIfAbsent(entity.getType(), k -> new AtomicInteger(0))
                .incrementAndGet();

        String token = String.format(TOKEN_FORMAT, entity.getType().getCode(), counter);

        // Store bidirectional mapping
        tokenToValue.put(token, entity.getValue());
        valueToToken.put(entity.getValue(), token);

        log.debug("Created token {} for {} (type: {})",
                token, maskValue(entity.getValue()), entity.getType());

        return token;
    }

    /**
     * Restore original value from token
     *
     * @param token The token to detokenize
     * @return Original value, or null if token not found
     */
    public String detokenize(String token) {
        String value = tokenToValue.get(token);
        if (value == null) {
            log.warn("Attempted to detokenize unknown token: {}", token);
        }
        return value;
    }

    /**
     * Replace all tokens in text with original values
     *
     * @param text Text containing tokens
     * @return Text with tokens replaced by original values
     */
    public String detokenizeText(String text) {
        if (text == null || text.isEmpty() || tokenToValue.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : tokenToValue.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Get all token mappings (for debugging/testing)
     */
    public Map<String, String> getTokenMappings() {
        return Map.copyOf(tokenToValue);
    }

    /**
     * Get statistics about tokenization
     */
    public TokenStats getStats() {
        return new TokenStats(
                tokenToValue.size(),
                counters.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().name(),
                                e -> e.getValue().get())));
    }

    /**
     * Clear all tokens (for testing)
     */
    public void clear() {
        tokenToValue.clear();
        valueToToken.clear();
        counters.clear();
    }

    public boolean hasTokens() {
        return !tokenToValue.isEmpty();
    }

    public Map<String, String> getAllTokens() {
        return new java.util.HashMap<>(tokenToValue);
    }

    /**
     * Mask value for logging
     */
    private String maskValue(String value) {
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * Statistics about tokenization
     */
    public record TokenStats(int totalTokens, Map<String, Integer> tokensByType) {
    }
}
