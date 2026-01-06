package com.neurogate.flywheel.model;

import com.neurogate.flywheel.model.GoldenInteraction.FeedbackType;
import com.neurogate.sentinel.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    private String traceId;
    private String sessionId;
    private List<Message> messages;
    private String response;
    private Integer rating; // 1-5
    private String feedbackText;
    private FeedbackType feedbackType;
    private String model;
    private String provider;
    private Integer tokenCount;
    private String userId;
    private Map<String, String> tags;
    private String comment; // Kept for backward compatibility if needed, mapped to feedbackText
    private String correctedOutput; // For RLHF
}
