package com.neurogate.test;

import com.neurogate.router.provider.LLMProvider;
import com.neurogate.router.provider.ProviderMetadata;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Mock LLM providers for testing.
 */
public final class MockProviders {

    private MockProviders() {
        // Utility class
    }

    /**
     * Create a mock OpenAI provider.
     */
    public static LLMProvider openAi() {
        return new MockLLMProvider("openai", List.of("gpt-4", "gpt-4o", "gpt-3.5-turbo", "gpt-4o-mini"), true);
    }

    /**
     * Create a mock Anthropic provider.
     */
    public static LLMProvider anthropic() {
        return new MockLLMProvider("anthropic",
                List.of("claude-3-opus-20240229", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022"),
                true);
    }

    /**
     * Create a mock unavailable provider.
     */
    public static LLMProvider unavailable(String name) {
        return new MockLLMProvider(name, List.of(), false);
    }

    /**
     * Create a failing provider (throws on generate).
     */
    public static LLMProvider failing(String name) {
        return new FailingLLMProvider(name);
    }

    /**
     * Create a mock provider with custom response.
     */
    public static LLMProvider withResponse(String name, Function<ChatRequest, ChatResponse> responseProvider) {
        return new CustomResponseProvider(name, responseProvider);
    }

    // ==================== Mock Provider Implementations ====================

    private static class MockLLMProvider implements LLMProvider {
        private final String name;
        private final List<String> supportedModels;
        private final boolean available;

        MockLLMProvider(String name, List<String> supportedModels, boolean available) {
            this.name = name;
            this.supportedModels = supportedModels;
            this.available = available;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getSupportedModels() {
            return supportedModels;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public boolean supportsModel(String model) {
            return supportedModels.contains(model);
        }

        @Override
        public ChatResponse generate(ChatRequest request) {
            return TestFixtures.responseWithRoute(name);
        }

        @Override
        public Flux<ChatResponse> generateStream(ChatRequest request) {
            return Flux.just(
                    TestFixtures.deltaResponse("Hello"),
                    TestFixtures.deltaResponse(" world"),
                    TestFixtures.deltaResponse("!")
            );
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
            // Map to first supported model
            return supportedModels.stream().findFirst().orElse(requestedModel);
        }

        @Override
        public ProviderMetadata getMetadata() {
            return ProviderMetadata.builder()
                    .name(name)
                    .enabled(available)
                    .priority(1)
                    .build();
        }
    }

    private static class FailingLLMProvider implements LLMProvider {
        private final String name;

        FailingLLMProvider(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getSupportedModels() {
            return List.of("any-model");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean supportsModel(String model) {
            return true;
        }

        @Override
        public ChatResponse generate(ChatRequest request) {
            throw new RuntimeException("Provider " + name + " failed");
        }

        @Override
        public Flux<ChatResponse> generateStream(ChatRequest request) {
            return Flux.error(new RuntimeException("Provider " + name + " streaming failed"));
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
            return requestedModel;
        }

        @Override
        public ProviderMetadata getMetadata() {
            return ProviderMetadata.builder()
                    .name(name)
                    .enabled(true)
                    .priority(1)
                    .build();
        }
    }

    private static class CustomResponseProvider implements LLMProvider {
        private final String name;
        private final Function<ChatRequest, ChatResponse> responseProvider;

        CustomResponseProvider(String name, Function<ChatRequest, ChatResponse> responseProvider) {
            this.name = name;
            this.responseProvider = responseProvider;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getSupportedModels() {
            return List.of("any-model");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean supportsModel(String model) {
            return true;
        }

        @Override
        public ChatResponse generate(ChatRequest request) {
            return responseProvider.apply(request);
        }

        @Override
        public Flux<ChatResponse> generateStream(ChatRequest request) {
            ChatResponse response = responseProvider.apply(request);
            String content = response.getChoices().get(0).getMessage().getStrContent();
            return Flux.fromArray(content.split("(?<=\\s)"))
                    .map(TestFixtures::deltaResponse);
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
            return requestedModel;
        }

        @Override
        public ProviderMetadata getMetadata() {
            return ProviderMetadata.builder()
                    .name(name)
                    .enabled(true)
                    .priority(1)
                    .build();
        }
    }
}
