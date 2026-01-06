package com.neurogate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "neurogate")
public class NeuroGateProperties {

    private Qdrant qdrant = new Qdrant();
    private Ollama ollama = new Ollama();
    private Embedding embedding = new Embedding();
    private Router router = new Router();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Qdrant {
        private boolean enabled = true;
        private String host = "localhost";
        private int grpcPort = 6334;
        private int restPort = 6333;
        private String collectionName = "prompt_cache";
        private int vectorSize = 384;
        private double similarityThreshold = 0.95;
        private int timeoutSeconds = 5;
    }

    @Data
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3:8b";
        private int timeoutSeconds = 30;
    }

    @Data
    public static class Embedding {
        private String modelPath = "models/all-MiniLM-L6-v2.onnx";
        private int maxSequenceLength = 512;
        private int batchSize = 32;
    }

    @Data
    public static class Router {
        private int complexityThreshold = 50;
        private boolean enableLocalRouting = true;
        private boolean enableSemanticCache = true;
        private int cacheTtlHours = 24;
        private java.util.List<String> voters = new java.util.ArrayList<>(
                java.util.List.of("gpt-4", "claude-3-opus", "gemini-pro"));
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultRpm = 1000;
        private int burstCapacity = 100;
        private int refillTokensPerMinute = 1000;
    }
}
