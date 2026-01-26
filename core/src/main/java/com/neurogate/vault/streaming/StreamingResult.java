package com.neurogate.vault.streaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of processing a token through the streaming guardrail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResult {

    /**
     * The token to emit (may be filtered/modified).
     */
    private String token;

    /**
     * Whether to continue the stream.
     * False = abort stream immediately.
     */
    @Builder.Default
    private boolean shouldContinue = true;

    /**
     * Reason for aborting (if shouldContinue is false).
     */
    private String abortReason;

    /**
     * Category of the violation (if any).
     */
    private String violationCategory;

    /**
     * Current toxicity level (0-100).
     */
    private int toxicityLevel;

    /**
     * Number of warnings triggered in this stream.
     */
    private int warningCount;

    /**
     * Create an OK result (continue streaming).
     */
    public static StreamingResult ok(String token, int toxicity) {
        return StreamingResult.builder()
                .token(token)
                .shouldContinue(true)
                .toxicityLevel(toxicity)
                .build();
    }

    /**
     * Create an OK result with warning.
     */
    public static StreamingResult warn(String token, int toxicity, int warningCount) {
        return StreamingResult.builder()
                .token(token)
                .shouldContinue(true)
                .toxicityLevel(toxicity)
                .warningCount(warningCount)
                .build();
    }

    /**
     * Create an abort result (stop streaming).
     */
    public static StreamingResult abort(String reason, String category) {
        return StreamingResult.builder()
                .shouldContinue(false)
                .abortReason(reason)
                .violationCategory(category)
                .build();
    }

    /**
     * Create a result for already aborted stream.
     */
    public static StreamingResult alreadyAborted(String reason) {
        return StreamingResult.builder()
                .shouldContinue(false)
                .abortReason(reason)
                .violationCategory("PREVIOUS_ABORT")
                .build();
    }
}