package com.neurogate.prompts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Reusable prompt template with variables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    private String templateId;
    private String templateName;
    private String description;

    // Template with placeholders: "Analyze {document_type} for {analysis_type}"
    private String template;

    // Variable definitions
    private Map<String, VariableDefinition> variables;

    // Default values
    private Map<String, String> defaults;

    // Category/tags
    private String[] tags;

    // Usage stats
    private Integer usageCount;
    private Double averageRating;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableDefinition {
        private String name;
        private String description;
        private String type; // "string", "number", "enum"
        private String[] allowedValues; // For enum type
        private boolean required;
    }

    /**
     * Render template with provided values
     */
    public String render(Map<String, String> values) {
        String result = template;

        // Replace variables
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }

        // Replace remaining placeholders with defaults
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue());
            }
        }

        return result;
    }
}
