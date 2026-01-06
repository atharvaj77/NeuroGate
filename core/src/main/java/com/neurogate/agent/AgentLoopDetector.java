package com.neurogate.agent;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neurogate.sentinel.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service to detect and prevent infinite loops in agentic workflows.
 * It tracks the content of recent requests per session and uses fuzzy matching.
 */
@Slf4j
@Service
public class AgentLoopDetector {

    // Store last 5 request content snippets per session
    private final Cache<String, List<String>> sessionHistoryCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    private static final int MAX_HISTORY_SIZE = 5;
    private static final int LOOP_THRESHOLD = 3; // 3 consecutive similar requests = loop
    private static final double SIMILARITY_THRESHOLD = 0.95; // 95% similarity
    private static final int MAX_CONTENT_LENGTH = 2000; // Truncate validation content

    /**
     * Checks if the current request is part of a loop.
     * 
     * @param request The chat request
     * @throws AgentLoopException if a loop is detected
     */
    public void validateRequest(ChatRequest request) {
        String sessionId = request.getSessionId();

        // Skip loop detection if no session ID provided
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String content = request.getConcatenatedContent();
        String currentContent = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH)
                : content;

        sessionHistoryCache.asMap().compute(sessionId, (key, history) -> {
            if (history == null) {
                history = new ArrayList<>();
            }

            history.add(currentContent);

            // Keep only recent history
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }

            // Check for potential loop (last N items are similar)
            if (detectLoop(history)) {
                log.warn("Agent loop detected for session: {}", sessionId);
                throw new AgentLoopException(
                        "Agent loop detected: 3 consecutive similar requests in session " + sessionId);
            }

            return history;
        });
    }

    private boolean detectLoop(List<String> history) {
        if (history.size() < LOOP_THRESHOLD) {
            return false;
        }

        String lastContent = history.get(history.size() - 1);

        // Check if the last N items are similar enough
        for (int i = 1; i < LOOP_THRESHOLD; i++) {
            String prevContent = history.get(history.size() - 1 - i);
            if (!isSimilar(lastContent, prevContent)) {
                return false;
            }
        }

        return true;
    }

    private boolean isSimilar(String s1, String s2) {
        if (s1.equals(s2))
            return true;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0)
            return true;

        int distance = calculateLevenshteinDistance(s1, s2);
        double similarity = 1.0 - ((double) distance / maxLength);

        return similarity >= SIMILARITY_THRESHOLD;
    }

    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                            + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}
