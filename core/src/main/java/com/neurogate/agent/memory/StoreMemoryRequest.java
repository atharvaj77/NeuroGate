package com.neurogate.agent.memory;

import lombok.Data;

@Data
public class StoreMemoryRequest {
    private String id;
    private String content;
    private String type; // e.g., "fact", "conversation", "tool_output"
    private String agentId;
    private java.util.Map<String, Object> metadata;
}
