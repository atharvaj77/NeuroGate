package com.neurogate.flywheel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neurogate.sentinel.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A high-quality interaction marked for training/fine-tuning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenInteraction {

    private String id;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("session_id")
    private String sessionId;

    private Instant timestamp;

    // The conversation messages
    private List<Message> messages;

    // The model's response
    private String response;

    // Quality indicators
    private Integer rating; // 1-5 star rating

    @JsonProperty("feedback_text")
    private String feedbackText;

    @JsonProperty("feedback_type")
    private FeedbackType feedbackType;

    // Metadata
    private String model;
    private String provider;

    @JsonProperty("token_count")
    private Integer tokenCount;

    @JsonProperty("user_id")
    private String userId;

    // Optional corrected output provided by human reviewer for RLHF
    private String correction;

    private Map<String, String> tags;

    public enum FeedbackType {
        THUMBS_UP,
        THUMBS_DOWN,
        STARRED,
        CORRECTED,
        FLAGGED
    }

    /**
     * Check if this interaction qualifies as "golden" (high quality)
     */
    public boolean isGolden() {
        return feedbackType == FeedbackType.STARRED ||
                feedbackType == FeedbackType.CORRECTED ||
                (rating != null && rating >= 4);
    }
}
