package com.neurogate.sentinel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * Chat message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @NotNull(message = "Role is required")
    @NotNull(message = "Role is required")
    private String role;

    @NotNull(message = "Content is required")
    private Object content;

    private String name;

    public static Message user(String content) {
        return Message.builder().role("user").content(content).build();
    }

    /**
     * Helper to get content as String if it is a simple string.
     * Returns concatenated text if it is a list of parts.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getStrContent() {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof java.util.List) {
            // Concatenate all "text" parts
            java.util.List<?> list = (java.util.List<?>) content;
            StringBuilder sb = new StringBuilder();
            for (Object part : list) {
                if (part instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) part;
                    if ("text".equals(map.get("type")) && map.containsKey("text")) {
                        sb.append(map.get("text")).append(" ");
                    }
                }
            }
            return sb.toString().trim();
        }
        return content != null ? content.toString() : "";
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
