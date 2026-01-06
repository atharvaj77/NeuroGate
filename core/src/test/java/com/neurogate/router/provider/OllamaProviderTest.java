package com.neurogate.router.provider;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OllamaProviderTest {

    private MockWebServer mockWebServer;
    private OllamaProvider ollamaProvider;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder();
        ollamaProvider = new OllamaProvider(webClientBuilder);

        ReflectionTestUtils.setField(ollamaProvider, "baseUrl", mockWebServer.url("/").toString());
        ReflectionTestUtils.setField(ollamaProvider, "enabled", true);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testIsAvailable_Success() {
        mockWebServer.enqueue(new MockResponse().setBody("0.1.32"));

        assertTrue(ollamaProvider.isAvailable());
    }

    @Test
    void testIsAvailable_Failure() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertFalse(ollamaProvider.isAvailable());
    }

    @Test
    void testGenerate_Success() {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "model": "llama3",
                            "created_at": "2023-12-31T12:00:00Z",
                            "message": {
                                "role": "assistant",
                                "content": "Hello from Llama!"
                            },
                            "done": true,
                            "eval_count": 10
                        }
                        """));

        ChatRequest request = ChatRequest.builder()
                .model("llama3")
                .messages(List.of(Message.builder().role("user").content("Hello").build()))
                .build();

        ChatResponse response = ollamaProvider.generate(request);

        assertNotNull(response);
        assertEquals("llama3", response.getModel());
        assertEquals("Hello from Llama!", response.getChoices().get(0).getMessage().getContent());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
        assertEquals("ollama", response.getRoute());
    }

    @Test
    void testGetSupportedModels() {
        mockWebServer.enqueue(new MockResponse().setBody("0.1.32")); // For isAvailable check
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "models": [
                                {"name": "llama3:latest", "size": 123456},
                                {"name": "mistral:latest", "size": 654321}
                            ]
                        }
                        """));

        List<String> models = ollamaProvider.getSupportedModels();

        assertEquals(2, models.size());
        assertTrue(models.contains("llama3:latest"));
        assertTrue(models.contains("mistral:latest"));
    }
}
