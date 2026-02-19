package com.neurogate.consensus;

import com.neurogate.client.ProviderClient;
import com.neurogate.config.NeuroGateProperties;
import com.neurogate.router.resilience.HedgingService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsensusServiceTest {

    @Mock
    private HedgingService hedgingService;

    @Mock
    private ProviderClient openAiClient;

    @Mock
    private ProviderClient geminiClient;

    private ConsensusService consensusService;
    private NeuroGateProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(openAiClient.getProviderName()).thenReturn("openai");
        when(geminiClient.getProviderName()).thenReturn("gemini");
        properties = new NeuroGateProperties();

        consensusService = new ConsensusService(hedgingService, List.of(openAiClient, geminiClient), properties);
    }

    @Test
    void reachConsensus_shouldAggregateResponses() {
        // Arrange
        ChatRequest request = ChatRequest.builder()
                .model("experimental-model")
                .messages(List.of(Message.builder().role("user").content("Hello").build()))
                .build();

        ChatResponse resp1 = createMockResponse("openai", "Response A");
        ChatResponse resp2 = createMockResponse("gemini", "Response B");

        // Mock Hedging Service returning both responses
        when(hedgingService.executeAll(anyString(), anyList()))
                .thenReturn(List.of(resp1, resp2));

        // Mock Judge (OpenAI) response
        ChatResponse judgeResp = createMockResponse("openai", "Synthesized Response");
        when(openAiClient.generate(any(ChatRequest.class))).thenReturn(judgeResp);

        // Act
        ConsensusResult result = consensusService.reachConsensus(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getSynthesis().contains("Synthesized"));

        // Verify hedging was called
        verify(hedgingService).executeAll(eq("consensus-group"), anyList());

        // Verify judge was called (it's the openAiClient)
        // It's called twice technically in this mock setup (once as provider, once as
        // judge)
        // or once if executeAll mocks the provider call internally.
        // Since we mock executeAll result directly, the provider lambdas inside
        // reachConsensus
        // aren't actually executed by the mock hedgingService, so openAiClient.generate
        // is called ONLY by the judge logic.
        verify(openAiClient).generate(any(ChatRequest.class));
    }

    private ChatResponse createMockResponse(String route, String content) {
        return ChatResponse.builder()
                .route(route)
                .choices(List.of(Choice.builder()
                        .message(Message.builder().content(content).build())
                        .build()))
                .build();
    }
}
