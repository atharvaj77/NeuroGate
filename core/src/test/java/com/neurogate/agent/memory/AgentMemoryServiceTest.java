package com.neurogate.agent.memory;

import com.neurogate.rag.client.VectorStoreClient;
import com.neurogate.rag.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AgentMemoryServiceTest {

    @Mock
    private VectorStoreClient vectorStoreClient;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private AgentMemoryService agentMemoryService;

    @Test
    void testSaveMemory() {
        StoreMemoryRequest request = new StoreMemoryRequest();
        request.setContent("The user likes blue.");
        request.setMetadata(Map.of("user", "123"));

        when(embeddingService.embed("The user likes blue."))
                .thenReturn(List.of(0.1, 0.2, 0.3));

        String id = agentMemoryService.save(request);

        assertNotNull(id);
        verify(embeddingService).embed("The user likes blue.");
        verify(vectorStoreClient).upsert(eq("agent_memory"), anyList());
    }

    @Test
    void testSearchMemory() {
        String query = "What does user like?";
        List<Double> vector = List.of(0.1, 0.2, 0.3);

        when(embeddingService.embed(query)).thenReturn(vector);

        VectorStoreClient.ScoredPoint point = new VectorStoreClient.ScoredPoint(
                "mem-1", 0.9, Map.of("content", "The user likes blue."));
        when(vectorStoreClient.search(eq("agent_memory"), eq(vector), eq(5), isNull()))
                .thenReturn(List.of(point));

        List<String> results = agentMemoryService.search(query, 5);

        assertEquals(1, results.size());
        assertEquals("The user likes blue.", results.get(0));
        verify(embeddingService).embed(query);
        verify(vectorStoreClient).search(eq("agent_memory"), eq(vector), eq(5), isNull());
    }
}
