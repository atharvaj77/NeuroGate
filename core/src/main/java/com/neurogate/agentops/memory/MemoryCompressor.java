package com.neurogate.agentops.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MemoryCompressor - Innovative feature to keep context windows small.
 */
@Slf4j
@Component
public class MemoryCompressor {

    private static final int MAX_RAW_MESSAGES = 10;

    /**
     * Compresses a list of messages into a smaller context.
     * Current Micro-Innovation: "Sliding Window with Summary Placeholder"
     */
    public List<String> compress(List<String> messages) {
        if (messages.size() <= MAX_RAW_MESSAGES) {
            return messages;
        }

        log.info("Compressing agent memory: {} messages -> {}", messages.size(), MAX_RAW_MESSAGES);

        // Strategy: Keep first 2 (system prompt/goal), summarize middle, keep last 8
        List<String> head = messages.subList(0, 2);
        List<String> tail = messages.subList(messages.size() - 8, messages.size());

        // In a real system, we'd call an LLM here to summarize messages.subList(2,
        // size-8)
        String summary = String.format(
                "<SYSTEM_NOTE> ... %d previous messages summarized: [Context maintained] ... </SYSTEM_NOTE>",
                messages.size() - 10);

        List<String> compressed = new java.util.ArrayList<>(head);
        compressed.add(summary);
        compressed.addAll(tail);

        return compressed;
    }
}
