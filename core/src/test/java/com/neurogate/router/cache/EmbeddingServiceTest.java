package com.neurogate.router.cache;

import com.neurogate.config.NeuroGateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EmbeddingService
 */
class EmbeddingServiceTest {

    private EmbeddingService embeddingService;
    private NeuroGateProperties properties;

    @BeforeEach
    void setUp() {
        properties = new NeuroGateProperties();
        embeddingService = new EmbeddingService(properties);
    }

    @Test
    void generateEmbedding_shouldReturnCorrectDimension() {
        // Given
        String text = "What is Java?";

        // When
        float[] embedding = embeddingService.generateEmbedding(text);

        // Then
        assertThat(embedding).hasSize(384);  // all-MiniLM-L6-v2 dimension
    }

    @Test
    void generateEmbedding_sameTextShouldProduceSameEmbedding() {
        // Given
        String text = "What is Java?";

        // When
        float[] embedding1 = embeddingService.generateEmbedding(text);
        float[] embedding2 = embeddingService.generateEmbedding(text);

        // Then
        assertThat(embedding1).isEqualTo(embedding2);
    }

    @Test
    void cosineSimilarity_identicalEmbeddingsShouldReturnOne() {
        // Given
        String text = "What is Java?";
        float[] embedding = embeddingService.generateEmbedding(text);

        // When
        double similarity = embeddingService.cosineSimilarity(embedding, embedding);

        // Then
        assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void cosineSimilarity_differentTextsShouldReturnLowerSimilarity() {
        // Given
        String text1 = "What is Java?";
        String text2 = "Explain quantum physics";

        // When
        float[] embedding1 = embeddingService.generateEmbedding(text1);
        float[] embedding2 = embeddingService.generateEmbedding(text2);
        double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

        // Then
        assertThat(similarity).isLessThan(1.0);
    }
}
