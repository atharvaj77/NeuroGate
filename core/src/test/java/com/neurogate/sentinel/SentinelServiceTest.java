package com.neurogate.sentinel;

import com.neurogate.agent.AgentLoopDetector;
import com.neurogate.pulse.PulseEventPublisher;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import com.neurogate.validation.StructuredOutputService;
import com.neurogate.vault.PiiRestorerFactory;
import com.neurogate.vault.PiiSanitizationService;
import com.neurogate.vault.StreamingPiiRestorer;
import com.neurogate.vault.model.SanitizedPrompt;
import com.neurogate.vault.neuroguard.ActiveDefenseService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SentinelServiceTest {

    @Mock
    private MultiProviderRouter multiProviderRouter;
    @Mock
    private PiiSanitizationService piiSanitizationService;
    @Mock
    private PiiRestorerFactory piiRestorerFactory;
    @Mock
    private ActiveDefenseService activeDefenseService;
    @Mock
    private PulseEventPublisher pulseEventPublisher;
    @Mock
    private AgentLoopDetector agentLoopDetector;
    @Mock
    private StructuredOutputService structuredOutputService;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private RetryRegistry retryRegistry;

    private SentinelService sentinelService;

    @BeforeEach
    void setUp() {
        sentinelService = new SentinelService(
                multiProviderRouter,
                piiSanitizationService,
                piiRestorerFactory,
                activeDefenseService,
                pulseEventPublisher,
                agentLoopDetector,
                structuredOutputService,
                circuitBreakerRegistry,
                retryRegistry);
    }

    @Test
    void createSanitizedRequest_shouldNotMutateOriginalMessages() {
        Message originalMessage = Message.builder().role("user").content("original prompt").build();
        ChatRequest originalRequest = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(originalMessage))
                .temperature(0.2)
                .maxTokens(256)
                .build();

        ChatRequest sanitizedRequest = ReflectionTestUtils.invokeMethod(
                sentinelService,
                "createSanitizedRequest",
                originalRequest,
                "sanitized prompt");

        assertEquals("original prompt", originalRequest.getMessages().get(0).getStrContent());
        assertEquals("sanitized prompt", sanitizedRequest.getMessages().get(0).getStrContent());
        assertNotSame(originalRequest.getMessages().get(0), sanitizedRequest.getMessages().get(0));
    }

    @Test
    void processStreamRequest_shouldOpenCircuitAfterFiveConsecutiveFailures() {
        CircuitBreaker streamCircuitBreaker = CircuitBreaker.of(
                "streaming",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build());
        Retry streamRetry = Retry.of("streaming", RetryConfig.custom().maxAttempts(1).build());

        when(circuitBreakerRegistry.circuitBreaker("streaming")).thenReturn(streamCircuitBreaker);
        when(retryRegistry.retry("streaming")).thenReturn(streamRetry);
        when(activeDefenseService.validatePrompt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(agentLoopDetector).validateRequest(any());
        when(multiProviderRouter.routeStream(any())).thenReturn(Flux.error(new RuntimeException("stream failure")));

        SanitizedPrompt sanitizedPrompt = new SanitizedPrompt();
        sanitizedPrompt.setContainsPii(false);
        sanitizedPrompt.setSanitizedText("request text");
        sanitizedPrompt.setDetectedEntities(List.of());
        when(piiSanitizationService.sanitize(anyString())).thenReturn(sanitizedPrompt);

        StreamingPiiRestorer restorer = org.mockito.Mockito.mock(StreamingPiiRestorer.class);
        when(restorer.flush()).thenReturn("");
        when(piiRestorerFactory.createRestorer()).thenReturn(restorer);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.builder().role("user").content("request text").build()))
                .stream(true)
                .build();

        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> sentinelService.processStreamRequest(request).blockLast());
        }

        assertEquals(CircuitBreaker.State.OPEN, streamCircuitBreaker.getState());
    }
}
