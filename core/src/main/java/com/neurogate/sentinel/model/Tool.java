package com.neurogate.sentinel.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Represents a tool/function that can be called by the LLM.
 *
 * <p>Tools enable function calling capabilities, allowing the model to
 * request execution of external functions and use their results.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Tool weatherTool = Tool.builder()
 *     .type("function")
 *     .function(Tool.Function.builder()
 *         .name("get_weather")
 *         .description("Get current weather for a location")
 *         .parameters(Map.of(
 *             "type", "object",
 *             "properties", Map.of(
 *                 "location", Map.of("type", "string", "description", "City name")
 *             ),
 *             "required", List.of("location")
 *         ))
 *         .build())
 *     .build();
 * }</pre>
 */
@Data
@Builder
public class Tool {

    /**
     * Tool type. Currently only "function" is supported.
     */
    private String type;

    /**
     * Function definition.
     */
    private Function function;

    /**
     * Function definition for tool calling.
     */
    @Data
    @Builder
    public static class Function {
        /**
         * The name of the function to be called.
         */
        private String name;

        /**
         * A description of what the function does.
         */
        private String description;

        /**
         * The parameters the function accepts, described as a JSON Schema object.
         */
        private Map<String, Object> parameters;

        /**
         * Whether the function should be called in strict mode.
         */
        private Boolean strict;
    }

    /**
     * Create a function tool with the given name and description.
     */
    public static Tool function(String name, String description, Map<String, Object> parameters) {
        return Tool.builder()
                .type("function")
                .function(Function.builder()
                        .name(name)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }
}
