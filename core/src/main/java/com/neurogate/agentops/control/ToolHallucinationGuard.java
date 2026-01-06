package com.neurogate.agentops.control;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Detects slightly hallucinated tool names and suggests corrections.
 */
@Component
public class ToolHallucinationGuard {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    // In a real app, this would come from a ToolRegistry
    private static final List<String> REGISTERED_TOOLS = List.of(
            "web_search",
            "calculator",
            "sql_query",
            "send_email",
            "read_file");

    private static final int MAX_DISTANCE = 3;

    /**
     * Checks if a tool name is valid, or suggests a correction.
     * 
     * @param toolName Name invoked by the agent
     * @return Optional containing corrected name if hallucination detected and
     *         fixable. Empty if valid or unfixable.
     */
    public Optional<String> suggestCorrection(String toolName) {
        if (toolName == null)
            return Optional.empty();

        // Exact match
        if (REGISTERED_TOOLS.contains(toolName)) {
            return Optional.empty(); // No correction needed
        }

        // Find closest match
        String bestMatch = null;
        int lowestDistance = Integer.MAX_VALUE;

        for (String registered : REGISTERED_TOOLS) {
            int distance = levenshteinDistance.apply(toolName, registered);
            if (distance < lowestDistance) {
                lowestDistance = distance;
                bestMatch = registered;
            }
        }

        if (lowestDistance <= MAX_DISTANCE && bestMatch != null) {
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }
}
