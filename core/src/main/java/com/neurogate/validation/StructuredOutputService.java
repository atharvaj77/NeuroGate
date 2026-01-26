package com.neurogate.validation;

import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import com.neurogate.validation.model.ValidationMetadata;
import com.neurogate.validation.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating structured LLM outputs with JSON schema validation.
 * Automatically retries when output doesn't match the expected schema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredOutputService {

    private final StructuredOutputValidator validator;
    private final MultiProviderRouter router;

    @Value("${neurogate.validation.max-retries:3}")
    private int maxRetries;

    /**
     * Generate a response with schema validation and auto-retry.
     *
     * @param request The chat request with response_format specified
     * @return Response with validation metadata
     */
    public ChatResponse generateWithValidation(ChatRequest request) {
        // Check if validation is required
        if (!validator.requiresValidation(request.getResponseFormat())) {
            return router.route(request);
        }

        log.debug("Generating structured output with schema: {}",
                request.getResponseFormat().getJsonSchema().getName());

        int retries = 0;
        ChatResponse lastResponse = null;
        ValidationResult lastResult = null;
        List<Message> originalMessages = new ArrayList<>(request.getMessages());

        while (retries < maxRetries) {
            // Build request (with correction hint if retrying)
            ChatRequest currentRequest = retries == 0
                    ? request
                    : buildRetryRequest(request, originalMessages, lastResponse, lastResult);

            // Route to provider
            lastResponse = router.route(currentRequest);

            // Extract content and validate
            String content = extractContent(lastResponse);
            lastResult = validator.validate(content, request.getResponseFormat());

            if (lastResult.isValid()) {
                log.debug("Structured output validation passed (retries: {})", retries);

                // If auto-fixed, update the response content
                if (lastResult.isAutoFixed()) {
                    updateResponseContent(lastResponse, lastResult.getFixedContent());
                }

                // Add validation metadata
                lastResponse.setValidation(ValidationMetadata.builder()
                        .schemaValid(true)
                        .retriesNeeded(retries)
                        .autoFixed(lastResult.isAutoFixed())
                        .build());

                return lastResponse;
            }

            log.info("Structured output validation failed (attempt {}/{}): {} errors",
                    retries + 1, maxRetries, lastResult.getErrors().size());

            retries++;
        }

        // Max retries exceeded - return last response with error metadata
        log.warn("Structured output validation failed after {} retries", maxRetries);

        lastResponse.setValidation(ValidationMetadata.builder()
                .schemaValid(false)
                .retriesNeeded(retries)
                .errors(lastResult != null ? lastResult.getErrors() : List.of())
                .build());

        return lastResponse;
    }

    /**
     * Build a retry request with correction hints.
     */
    private ChatRequest buildRetryRequest(
            ChatRequest original,
            List<Message> originalMessages,
            ChatResponse invalidResponse,
            ValidationResult result
    ) {
        String invalidContent = extractContent(invalidResponse);
        String correctionHint = validator.generateCorrectionHint(result);

        List<Message> messages = new ArrayList<>(originalMessages);

        // Add the invalid response as assistant message
        messages.add(Message.builder()
                .role("assistant")
                .content(invalidContent)
                .build());

        // Add correction hint as user message
        messages.add(Message.builder()
                .role("user")
                .content(correctionHint)
                .build());

        return original.toBuilder()
                .messages(messages)
                .build();
    }

    /**
     * Extract text content from response.
     */
    private String extractContent(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }

        Message message = response.getChoices().get(0).getMessage();
        if (message == null) {
            // Try delta for streaming responses
            Message delta = response.getChoices().get(0).getDelta();
            return delta != null ? delta.getStrContent() : "";
        }

        return message.getStrContent();
    }

    /**
     * Update the response content with fixed JSON.
     */
    private void updateResponseContent(ChatResponse response, String fixedContent) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            Message message = response.getChoices().get(0).getMessage();
            if (message != null) {
                message.setContent(fixedContent);
            }
        }
    }

    /**
     * Validate a response against a schema without generating.
     */
    public ValidationResult validateOnly(String content, ChatRequest.ResponseFormat format) {
        return validator.validate(content, format);
    }
}