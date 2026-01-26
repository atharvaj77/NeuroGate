package com.neurogate.router.provider;

import com.neurogate.metrics.NeuroGateMetrics;
import com.neurogate.router.resilience.ResilienceService;
import com.neurogate.router.shadow.ShadowDeploymentService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import com.neurogate.vault.PiiSanitizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiProviderRouterTest {

    @Mock
    private LLMProvider openAiProvider;

    @Mock
    private LLMProvider anthropicProvider;

    @Mock
    private NeuroGateMetrics metrics;

    @Mock
    private ResilienceService resilienceService;

    @Mock
    private PiiSanitizationService piiSanitizationService;

    @Mock
    private ShadowDeploymentService shadowDeploymentService;

    private MultiProviderRouter router;
    private ChatRequest testRequest;
    private ChatResponse testResponse;

    @BeforeEach
    void setUp() {
        // Setup mock provider behavior
        lenient().when(openAiProvider.getName()).thenReturn("openai");
        lenient().when(openAiProvider.isAvailable()).thenReturn(true);
        lenient().when(openAiProvider.supportsModel("gpt-4")).thenReturn(true);
        lenient().when(openAiProvider.supportsModel("gpt-3.5-turbo")).thenReturn(true);

        lenient().when(anthropicProvider.getName()).thenReturn("anthropic");
        lenient().when(anthropicProvider.isAvailable()).thenReturn(true);

        // Setup Router with mocks
        router = new MultiProviderRouter(
                List.of(openAiProvider, anthropicProvider),
                metrics,
                resilienceService,
                piiSanitizationService);
        router.setShadowDeploymentService(shadowDeploymentService);

        // Standard Request
        testRequest = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(Message.builder().role("user").content("Hello").build()))
                .build();

        // Standard Response
        testResponse = ChatResponse.builder()
                .model("gpt-4")
                .choices(List.of(Choice.builder()
                        .message(Message.builder().role("assistant").content("Hi there").build())
                        .build()))
                .latencyMs(100L)
                .build();

        // resilienceService mock - execute functional interface
        lenient().when(resilienceService.execute(anyString(), any(), any())).thenAnswer(invocation -> {
            Supplier<ChatResponse> supplier = invocation.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    void testRoute_StandardFlow() {
        when(openAiProvider.generate(any())).thenReturn(testResponse);

        ChatResponse response = router.route(testRequest);

        verify(openAiProvider).generate(testRequest);
        verify(resilienceService).execute(eq("openai"), any(), any());
    }

    @Test
    void testSpecterMode_AsyncExecution() {
        // Given a request with shadow_model
        testRequest.setShadowModel("gpt-3.5-turbo");

        when(openAiProvider.generate(any())).thenReturn(testResponse);

        // When routing
        router.route(testRequest);

        // Then
        // 1. Primary request executes immediately
        verify(openAiProvider, times(1)).generate(argThat(req -> req.getModel().equals("gpt-4")));

        // 2. Shadow deployment service is called with the shadow model
        verify(shadowDeploymentService).executeShadowRequest(
                eq(testRequest),
                eq("gpt-3.5-turbo"),
                any(Function.class));
    }

    @Test
    void testRoute_WithIntentOverride() {
        // Given a request with intent override
        ChatRequest requestWithIntent = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(Message.builder().role("user").content("Write code").build()))
                .intentOverride("CODE_GENERATION")
                .build();

        when(openAiProvider.generate(any())).thenReturn(testResponse);

        // When routing (without intent router injected, intent override should be ignored)
        ChatResponse response = router.route(requestWithIntent);

        // Then - should still route normally
        verify(openAiProvider).generate(requestWithIntent);
    }

    @Test
    void testStreamingPiiRedaction() {
        // Given: PiiSanitizationService is set up
        // We want to simulate a stream that contains PII tokens like <EMAIL_1>
        // and expect the desanitize method to be called.

        ChatRequest streamRequest = ChatRequest.builder().model("gpt-4").stream(true).build();

        // Mock streaming response flux
        // Chunk 1: "Contact " -> Buffers (Size 1) -> Returns ""
        // Chunk 2: "<EMAIL" -> Buffers (Size 2) -> Returns ""
        // Chunk 3: "_1>" -> Buffers (Size 3) -> Matches -> Returns "john@doe.com"
        // Chunk 4: " for info." -> Buffers (Size 1) -> Returns ""
        // Flush -> Returns " for info."

        ChatResponse c1 = deltaResponse("Contact ");
        ChatResponse c2 = deltaResponse("<EMAIL");
        ChatResponse c3 = deltaResponse("_1>");
        ChatResponse c4 = deltaResponse(" for info.");

        when(openAiProvider.generateStream(any())).thenReturn(Flux.just(c1, c2, c3, c4));

        // Mock PiiSanitizationService to return restored value
        // Note: Generic matches must come FIRST, specific matches LAST to override
        when(piiSanitizationService.desanitize(anyString())).thenAnswer(i -> i.getArgument(0)); // Default return input
        when(piiSanitizationService.desanitize(contains("<EMAIL_1>"))).thenReturn("john@doe.com");

        // When
        Flux<ChatResponse> result = router.routeStream(streamRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(r -> "".equals(r.getChoices().get(0).getDelta().getStrContent())) // Chunk 1 buffered
                .expectNextMatches(r -> "".equals(r.getChoices().get(0).getDelta().getStrContent())) // Chunk 2 buffered
                .expectNextMatches(r -> "john@doe.com".equals(r.getChoices().get(0).getDelta().getStrContent())) // Chunk
                                                                                                                 // 3
                                                                                                                 // restored
                .expectNextMatches(r -> "".equals(r.getChoices().get(0).getDelta().getStrContent())) // Chunk 4 buffered
                .expectNextMatches(r -> " for info.".equals(r.getChoices().get(0).getDelta().getStrContent())) // Flush
                .verifyComplete();
    }

    private ChatResponse deltaResponse(String content) {
        return ChatResponse.builder()
                .choices(List.of(Choice.builder()
                        .delta(Message.builder().role("assistant").content(content).build())
                        .build()))
                .build();
    }
}