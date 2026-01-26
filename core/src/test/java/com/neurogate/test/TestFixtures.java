package com.neurogate.test;

import com.neurogate.sentinel.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralized test fixtures for consistent test data.
 * Provides builder methods for common test objects.
 */
public final class TestFixtures {

    private TestFixtures() {
        // Utility class
    }

    // ==================== Chat Requests ====================

    /**
     * Create a default chat request with a simple user message.
     */
    public static ChatRequest defaultChatRequest() {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(userMessage("Hello, how are you?")))
                .build();
    }

    /**
     * Create a streaming chat request.
     */
    public static ChatRequest streamingRequest() {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(userMessage("Tell me a story")))
                .stream(true)
                .build();
    }

    /**
     * Create a chat request with specific model.
     */
    public static ChatRequest requestWithModel(String model) {
        return ChatRequest.builder()
                .model(model)
                .messages(List.of(userMessage("Test message")))
                .build();
    }

    /**
     * Create a chat request with multiple messages (conversation).
     */
    public static ChatRequest conversationRequest() {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        userMessage("What is 2+2?"),
                        assistantMessage("2+2 equals 4."),
                        userMessage("And what is 4+4?")
                ))
                .build();
    }

    /**
     * Create a chat request with system prompt.
     */
    public static ChatRequest requestWithSystemPrompt(String systemPrompt) {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        systemMessage(systemPrompt),
                        userMessage("Hello")
                ))
                .build();
    }

    /**
     * Create a code generation request.
     */
    public static ChatRequest codeGenerationRequest() {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(userMessage("Write a Python function to calculate fibonacci numbers")))
                .build();
    }

    /**
     * Create a request with shadow model (A/B testing).
     */
    public static ChatRequest shadowModelRequest(String primaryModel, String shadowModel) {
        ChatRequest request = ChatRequest.builder()
                .model(primaryModel)
                .messages(List.of(userMessage("Test message")))
                .build();
        request.setShadowModel(shadowModel);
        return request;
    }

    // ==================== Chat Responses ====================

    /**
     * Create a successful chat response.
     */
    public static ChatResponse successResponse() {
        return ChatResponse.builder()
                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("gpt-4")
                .choices(List.of(Choice.builder()
                        .index(0)
                        .message(assistantMessage("Hello! I'm doing well, thank you for asking."))
                        .finishReason("stop")
                        .build()))
                .usage(Usage.builder()
                        .promptTokens(10)
                        .completionTokens(15)
                        .totalTokens(25)
                        .build())
                .latencyMs(150L)
                .build();
    }

    /**
     * Create a response with specific content.
     */
    public static ChatResponse responseWithContent(String content) {
        return ChatResponse.builder()
                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("gpt-4")
                .choices(List.of(Choice.builder()
                        .index(0)
                        .message(assistantMessage(content))
                        .finishReason("stop")
                        .build()))
                .usage(Usage.builder()
                        .promptTokens(10)
                        .completionTokens(content.split("\\s+").length)
                        .totalTokens(10 + content.split("\\s+").length)
                        .build())
                .build();
    }

    /**
     * Create a streaming delta response.
     */
    public static ChatResponse deltaResponse(String content) {
        return ChatResponse.builder()
                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
                .object("chat.completion.chunk")
                .created(System.currentTimeMillis() / 1000)
                .model("gpt-4")
                .choices(List.of(Choice.builder()
                        .index(0)
                        .delta(Message.builder()
                                .role("assistant")
                                .content(content)
                                .build())
                        .build()))
                .build();
    }

    /**
     * Create a response with route information.
     */
    public static ChatResponse responseWithRoute(String route) {
        ChatResponse response = successResponse();
        response.setRoute(route);
        return response;
    }

    // ==================== Messages ====================

    /**
     * Create a user message.
     */
    public static Message userMessage(String content) {
        return Message.builder()
                .role("user")
                .content(content)
                .build();
    }

    /**
     * Create an assistant message.
     */
    public static Message assistantMessage(String content) {
        return Message.builder()
                .role("assistant")
                .content(content)
                .build();
    }

    /**
     * Create a system message.
     */
    public static Message systemMessage(String content) {
        return Message.builder()
                .role("system")
                .content(content)
                .build();
    }

    // ==================== PII Test Data ====================

    /**
     * Create a message containing an email address.
     */
    public static Message messageWithEmail() {
        return userMessage("Contact me at john.doe@example.com for more info");
    }

    /**
     * Create a message containing a phone number.
     */
    public static Message messageWithPhone() {
        return userMessage("Call me at 555-123-4567");
    }

    /**
     * Create a message containing an SSN.
     */
    public static Message messageWithSSN() {
        return userMessage("My SSN is 123-45-6789");
    }

    /**
     * Create a message containing an API key.
     */
    public static Message messageWithApiKey() {
        return userMessage("Here is my API key: sk-1234567890abcdef");
    }

    // ==================== Builder Methods ====================

    /**
     * Create a chat request builder with defaults.
     */
    public static ChatRequest.ChatRequestBuilder chatRequestBuilder() {
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(new ArrayList<>());
    }

    /**
     * Create a chat response builder with defaults.
     */
    public static ChatResponse.ChatResponseBuilder chatResponseBuilder() {
        return ChatResponse.builder()
                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model("gpt-4");
    }
}
