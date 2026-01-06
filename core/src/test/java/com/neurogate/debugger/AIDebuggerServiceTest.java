package com.neurogate.debugger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.agentops.TraceService;
import com.neurogate.router.cache.EmbeddingService;
import com.neurogate.router.cache.SemanticCacheService;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import com.neurogate.vault.PiiSanitizationService;
import com.neurogate.vault.tokenizer.TokenVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for AI Debugger Service
 */
@ExtendWith(MockitoExtension.class)
class AIDebuggerServiceTest {

        @Mock
        private MultiProviderRouter routerService;

        @Mock
        private TraceService traceService;

        @Mock
        private SemanticCacheService semanticCacheService;

        @Mock
        private EmbeddingService embeddingService;

        @Mock
        private PiiSanitizationService piiSanitizationService;

        private AIDebuggerService debuggerService;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                debuggerService = new AIDebuggerService(
                                routerService, traceService, embeddingService, objectMapper);
        }

        @Test
        void testRecordRequest() {
                // Given
                ChatRequest request = ChatRequest.builder()
                                .model("gpt-3.5-turbo")
                                .messages(List.of(Message.user("What is Java?")))
                                .user("test-user")
                                .build();

                ChatResponse response = ChatResponse.builder()
                                .route("openai")
                                .cacheHit(false)
                                .costUsd(0.002)
                                .build();

                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                // When
                debuggerService.recordRequest(request, response, null, 100L);

                // Then
                List<DebugRecord> records = debuggerService.getUserRecords("test-user", 10);
                assertThat(records).hasSize(1);
                assertThat(records.get(0).getProvider()).isEqualTo("openai");
                assertThat(records.get(0).getLatencyMs()).isEqualTo(100L);
        }

        @Test
        void testCreateSession() {
                // Given
                ChatRequest request = ChatRequest.builder()
                                .model("gpt-3.5-turbo")
                                .messages(List.of(Message.user("Test prompt")))
                                .build();

                ChatResponse response = ChatResponse.builder()
                                .route("openai")
                                .build();

                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);

                debuggerService.recordRequest(request, response, null, 100L);
                List<DebugRecord> records = debuggerService.getUserRecords(null, 10);
                String requestId = records.get(0).getRequestId();

                // When
                DebugSession session = debuggerService.createSession(requestId);

                // Then
                assertThat(session).isNotNull();
                assertThat(session.getRequestId()).isEqualTo(requestId);
                assertThat(session.getOriginalRequest()).isNotNull();
                assertThat(session.getLatencyMs()).isEqualTo(100L);
        }

        @Test
        void testReplay() {
                // Given
                ChatRequest originalRequest = ChatRequest.builder()
                                .model("gpt-3.5-turbo")
                                .messages(List.of(Message.user("Test prompt")))
                                .temperature(0.7)
                                .build();

                ChatResponse originalResponse = ChatResponse.builder()
                                .route("openai")
                                .build();

                ChatResponse replayResponse = ChatResponse.builder()
                                .route("anthropic")
                                .costUsd(0.001)
                                .build();

                float[] embedding = new float[] { 0.1f, 0.2f, 0.3f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);
                when(routerService.route(any())).thenReturn(replayResponse);

                debuggerService.recordRequest(originalRequest, originalResponse, null, 100L);
                List<DebugRecord> records = debuggerService.getUserRecords(null, 10);
                String requestId = records.get(0).getRequestId();
                DebugSession session = debuggerService.createSession(requestId);

                ReplayOptions options = ReplayOptions.builder()
                                .model("gpt-4")
                                .temperature(0.5)
                                .bypassCache(true)
                                .build();

                // When
                ChatResponse result = debuggerService.replay(session.getSessionId(), options);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getRoute()).isEqualTo("anthropic");
                assertThat(session.getComparisonResponse()).isNotNull();
        }

        @Test
        void testSemanticDiff() {
                // Given
                ChatRequest request = ChatRequest.builder()
                                .model("gpt-3.5-turbo")
                                .messages(List.of(Message.user("Test prompt")))
                                .build();

                ChatResponse originalResponse = ChatResponse.builder()
                                .route("openai")
                                .latencyMs(100L)
                                .costUsd(0.002)
                                .build();

                ChatResponse replayResponse = ChatResponse.builder()
                                .route("claude")
                                .latencyMs(80L)
                                .costUsd(0.001)
                                .build();

                float[] embedding = new float[] { 0.9f, 0.9f, 0.9f };
                when(embeddingService.generateEmbedding(any())).thenReturn(embedding);
                when(routerService.route(any())).thenReturn(replayResponse);

                debuggerService.recordRequest(request, originalResponse, null, 100L);
                List<DebugRecord> records = debuggerService.getUserRecords(null, 10);
                DebugSession session = debuggerService.createSession(records.get(0).getRequestId());

                debuggerService.replay(session.getSessionId(), ReplayOptions.builder().build());

                // When
                SemanticDiff diff = debuggerService.compareResponses(session.getSessionId());

                // Then
                assertThat(diff).isNotNull();
                assertThat(diff.getOriginalProvider()).isEqualTo("openai");
                assertThat(diff.getComparisonProvider()).isEqualTo("claude");
                assertThat(diff.getCostSavings()).isPositive();
                assertThat(diff.getLatencyImprovement()).isPositive();
        }

        @Test
        void testSearchRecords() {
                // Given
                for (int i = 0; i < 5; i++) {
                        ChatRequest request = ChatRequest.builder()
                                        .model("gpt-3.5-turbo")
                                        .messages(List.of(Message.user("Test " + i)))
                                        .user("user-" + (i % 2))
                                        .build();

                        ChatResponse response = ChatResponse.builder()
                                        .route(i % 2 == 0 ? "openai" : "anthropic")
                                        .costUsd(0.001 * i)
                                        .build();

                        when(embeddingService.generateEmbedding(any()))
                                        .thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

                        debuggerService.recordRequest(request, response, null, 100L + i * 10);
                }

                DebugSearchFilter filter = DebugSearchFilter.builder()
                                .provider("openai")
                                .minCost(0.001)
                                .limit(10)
                                .build();

                // When
                List<DebugRecord> results = debuggerService.searchRecords(filter);

                // Then
                assertThat(results).isNotEmpty();
                assertThat(results).allMatch(r -> r.getProvider().equals("openai"));
                assertThat(results).allMatch(r -> r.getCostUsd() >= 0.001);
        }

        @Test
        void testCleanupOldRecords() {
                // Given
                ChatRequest request = ChatRequest.builder()
                                .model("gpt-3.5-turbo")
                                .messages(List.of(Message.user("Test")))
                                .build();

                ChatResponse response = ChatResponse.builder()
                                .route("openai")
                                .build();

                when(embeddingService.generateEmbedding(any()))
                                .thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

                debuggerService.recordRequest(request, response, null, 100L);

                // When
                try {
                        Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                debuggerService.cleanupOldRecords(0); // Cleanup immediately

                // Then
                List<DebugRecord> records = debuggerService.getUserRecords(null, 10);
                assertThat(records).isEmpty();
        }
}
