package com.neurogate.sentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * OpenAI-compatible Chat Completion Request.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotNull(message = "Model is required")
    private String model;

    @JsonProperty("shadow_model")
    private String shadowModel;

    @NotEmpty(message = "Messages cannot be empty")
    private List<Message> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private List<String> stop;

    private Boolean stream;

    private String user;

    private Integer canaryWeight;

    private String traceId;
    private String sessionId;

    @JsonProperty("rag_enabled")
    private Boolean ragEnabled;

    @JsonProperty("rag_options")
    private RagOptions ragOptions;

    /**
     * Override the auto-detected intent for routing.
     * Valid values: CODE_GENERATION, CODE_REVIEW, REASONING, CREATIVE_WRITING, etc.
     */
    @JsonProperty("intent_override")
    private String intentOverride;

    /**
     * Response format specification for structured output.
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseFormat {
        /**
         * Type: "text", "json_object", or "json_schema"
         */
        private String type;

        /**
         * JSON schema definition (when type is "json_schema")
         */
        @JsonProperty("json_schema")
        private JsonSchemaSpec jsonSchema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JsonSchemaSpec {
        private String name;
        private String description;
        private boolean strict;
        private java.util.Map<String, Object> schema;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagOptions {
        private Integer topK;
        private Double threshold;
        private List<String> collectionNames;
        private Boolean includeCitations;
    }

    /**
     * Gets the concatenated content of all messages for embedding/caching.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getConcatenatedContent() {
        return messages.stream()
                .map(Message::getStrContent)
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }
}
