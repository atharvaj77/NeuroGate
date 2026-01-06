package com.neurogate.rag.service;

import com.neurogate.core.config.RagConfig;
import com.neurogate.rag.client.VectorStoreClient;
import com.neurogate.rag.client.VectorStoreClient.ScoredPoint;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusServiceTest {

    @Mock
    private RagConfig ragConfig;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private VectorStoreClient vectorStoreClient;
    @Mock
    private ContextInjector contextInjector;
    @Mock
    private RagConfig.VectorDb vectorDbConfig;
    @Mock
    private RagConfig.Retrieval retrievalConfig;

    private NexusService nexusService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ragConfig.getVectorDb()).thenReturn(vectorDbConfig);
        when(ragConfig.getRetrieval()).thenReturn(retrievalConfig);
        when(vectorDbConfig.getCollection()).thenReturn("test_collection");
        when(retrievalConfig.getTopK()).thenReturn(5);

        nexusService = new NexusService(ragConfig, embeddingService, vectorStoreClient, contextInjector);
    }

    @Test
    void testEnrichRequest_RagDisabled() {
        when(ragConfig.isEnabled()).thenReturn(false);
        ChatRequest request = ChatRequest.builder().ragEnabled(true).build();

        List<String> citations = nexusService.enrichRequest(request, "user1");
        assertTrue(citations.isEmpty());
    }

    @Test
    void testEnrichRequest_Success() {
        when(ragConfig.isEnabled()).thenReturn(true);
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        when(vectorStoreClient.search(anyString(), anyList(), anyInt(), anyMap()))
                .thenReturn(List.of(new ScoredPoint("doc1", 0.9, Map.of("content", "Secret Info"))));
        when(contextInjector.formatContext(anyList())).thenReturn("CTX");

        ChatRequest request = ChatRequest.builder()
                .ragEnabled(true)
                .messages(new java.util.ArrayList<>(List.of(Message.user("Hello"))))
                .build();

        List<String> citations = nexusService.enrichRequest(request, "user1");

        assertEquals(1, citations.size());
        assertEquals("doc1", citations.get(0));

        // Verify system message injected
        boolean hasSystem = request.getMessages().stream()
                .anyMatch(m -> "system".equals(m.getRole()) && m.getStrContent().contains("CTX"));
        assertTrue(hasSystem);
    }
}
