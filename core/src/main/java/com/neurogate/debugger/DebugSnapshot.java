package com.neurogate.debugger;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class DebugSnapshot {
    private String stepId;
    private Instant timestamp;
    private String stepType; // "USER_INPUT", "TOOL_CALL", "TOOL_RESULT", "MODEL_RESPONSE"
    private String content;
    private Map<String, Object> state; // Captured memory/context
}
