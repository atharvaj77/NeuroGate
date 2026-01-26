package com.neurogate.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.validation.model.ValidationError;
import com.neurogate.validation.model.ValidationResult;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates LLM responses against JSON schemas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredOutputValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    // Pattern to extract JSON from markdown code blocks
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```",
            Pattern.MULTILINE
    );

    // Pattern to find JSON object/array
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "(\\{[\\s\\S]*\\}|\\[[\\s\\S]*\\])"
    );

    /**
     * Validate a response against a schema from the request.
     */
    public ValidationResult validate(String response, ChatRequest.ResponseFormat responseFormat) {
        if (responseFormat == null || responseFormat.getJsonSchema() == null) {
            // No schema to validate against
            return ValidationResult.valid(response);
        }

        Map<String, Object> schemaMap = responseFormat.getJsonSchema().getSchema();
        if (schemaMap == null) {
            return ValidationResult.valid(response);
        }

        return validate(response, schemaMap);
    }

    /**
     * Validate a response against a JSON schema.
     */
    public ValidationResult validate(String response, Map<String, Object> schema) {
        if (response == null || response.isBlank()) {
            return ValidationResult.invalid(response, List.of(
                    ValidationError.of("$", "Response is empty")
            ));
        }

        // Try to extract JSON from the response
        String jsonContent = extractJson(response);

        try {
            // Parse response as JSON
            JsonNode responseNode = objectMapper.readTree(jsonContent);

            // Build JSON Schema
            JsonNode schemaNode = objectMapper.valueToTree(schema);
            JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);

            // Validate
            Set<ValidationMessage> validationMessages = jsonSchema.validate(responseNode);

            if (validationMessages.isEmpty()) {
                // Check if we extracted JSON (meaning original had extra content)
                if (!jsonContent.equals(response)) {
                    return ValidationResult.autoFixed(response, jsonContent);
                }
                return ValidationResult.valid(response);
            }

            // Convert validation messages to errors
            List<ValidationError> errors = validationMessages.stream()
                    .map(msg -> ValidationError.builder()
                            .path(msg.getPath())
                            .message(msg.getMessage())
                            .errorType(msg.getType())
                            .build())
                    .toList();

            return ValidationResult.invalid(response, errors);

        } catch (JsonProcessingException e) {
            // Try to auto-fix common JSON issues
            String fixed = attemptAutoFix(jsonContent);
            if (fixed != null) {
                try {
                    JsonNode fixedNode = objectMapper.readTree(fixed);
                    JsonNode schemaNode = objectMapper.valueToTree(schema);
                    JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
                    Set<ValidationMessage> msgs = jsonSchema.validate(fixedNode);

                    if (msgs.isEmpty()) {
                        return ValidationResult.autoFixed(response, fixed);
                    }
                } catch (JsonProcessingException ignored) {
                    // Auto-fix didn't work
                }
            }

            return ValidationResult.invalid(response, List.of(
                    ValidationError.of("$", "Invalid JSON: " + e.getMessage())
            ));
        }
    }

    /**
     * Extract JSON from a response that may contain markdown or other text.
     */
    public String extractJson(String response) {
        if (response == null) return null;

        String trimmed = response.trim();

        // If it already looks like JSON, return as-is
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }

        // Try to extract from markdown code block
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(response);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        // Try to find JSON object/array in the text
        Matcher jsonMatcher = JSON_OBJECT_PATTERN.matcher(response);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(1).trim();
        }

        return trimmed;
    }

    /**
     * Attempt to fix common JSON issues.
     */
    public String attemptAutoFix(String json) {
        if (json == null) return null;

        String fixed = json;

        // Remove trailing commas before } or ]
        fixed = fixed.replaceAll(",\\s*([}\\]])", "$1");

        // Try to fix single quotes to double quotes
        // Only do this if there are no double quotes (to avoid breaking valid JSON)
        if (!fixed.contains("\"") && fixed.contains("'")) {
            fixed = fixed.replace("'", "\"");
        }

        // Remove BOM if present
        if (fixed.startsWith("\uFEFF")) {
            fixed = fixed.substring(1);
        }

        // Remove leading/trailing whitespace
        fixed = fixed.trim();

        return fixed.equals(json) ? null : fixed;
    }

    /**
     * Generate a correction hint for retry prompts.
     */
    public String generateCorrectionHint(ValidationResult result) {
        if (result.isValid()) {
            return null;
        }

        StringBuilder hint = new StringBuilder();
        hint.append("Your response did not match the expected JSON schema. Please fix the following errors:\n\n");

        for (ValidationError error : result.getErrors()) {
            hint.append("- At path '").append(error.getPath()).append("': ")
                    .append(error.getMessage()).append("\n");
        }

        hint.append("\nPlease provide a valid JSON response that matches the schema exactly. ");
        hint.append("Do not include any explanation or markdown formatting, just the raw JSON.");

        return hint.toString();
    }

    /**
     * Check if a response format requires validation.
     */
    public boolean requiresValidation(ChatRequest.ResponseFormat format) {
        if (format == null) return false;
        return "json_schema".equals(format.getType()) && format.getJsonSchema() != null;
    }
}