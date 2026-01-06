package com.neurogate.vault;

import com.neurogate.vault.model.SanitizedPrompt;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Restores PII tokens in streaming responses (SSE).
 * Uses a sliding window buffer.
 */
@Slf4j
public class StreamingPiiRestorer {

    private static final int BUFFER_SIZE = 5;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("<([A-Z_]+)_(\\d+)>");

    private final Queue<String> buffer = new LinkedList<>();
    private final PiiSanitizationService piiSanitizationService;

    public StreamingPiiRestorer(PiiSanitizationService piiSanitizationService) {
        this.piiSanitizationService = piiSanitizationService;
    }

    /**
     * Process a streaming chunk with PII restoration.
     */
    public String processChunk(String chunk) {
        // Add chunk to buffer
        buffer.add(chunk);

        // Maintain buffer size
        if (buffer.size() > BUFFER_SIZE) {
            buffer.poll();
        }

        // Combine buffer contents to check for complete tokens
        String combined = String.join("", buffer);

        // Check if we have complete PII tokens
        Matcher matcher = TOKEN_PATTERN.matcher(combined);

        if (matcher.find()) {
            // We found a complete token, restore all tokens in the combined string
            String restored = piiSanitizationService.desanitize(combined);

            // Clear buffer after restoration
            buffer.clear();

            return restored;
        }

        // Check if we have a partial token at the end
        if (hasPartialToken(combined)) {
            // Buffer partial tokens
            log.debug("Detected partial token, buffering...");
            return "";
        }

        // No complete or partial tokens, safe to return the chunk
        // But keep the last few chunks in buffer in case a token spans multiple chunks
        if (buffer.size() > 3) {
            return buffer.poll();
        }

        return "";
    }

    /**
     * Flush any remaining buffered content
     * Call this when the stream ends
     */
    public String flush() {
        if (buffer.isEmpty()) {
            return "";
        }

        String remaining = String.join("", buffer);
        buffer.clear();

        // Restore any remaining PII tokens
        return piiSanitizationService.desanitize(remaining);
    }

    /**
     * Check if the combined string has a partial token at the end
     */
    private boolean hasPartialToken(String text) {
        // Check for partial token patterns like "<EM" or "<EMAIL_" at the end
        return text.matches(".*<[A-Z_]*$") || text.matches(".*<[A-Z_]+_\\d*$");
    }

    /**
     * Reset the buffer (for new streaming request)
     */
    public void reset() {
        buffer.clear();
        log.debug("StreamingPiiRestorer buffer reset");
    }
}
