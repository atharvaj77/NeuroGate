package com.neurogate.router.cache;

import com.neurogate.config.NeuroGateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Service for generating text embeddings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final NeuroGateProperties properties;

    /**
     * Generate embedding vector for the given text
     *
     * @param text Input text to embed
     * @return Embedding vector (384 dimensions)
     */
    public float[] generateEmbedding(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());
        return generateSimpleEmbedding(text);
    }

    /**
     * Simple hash-based embedding that creates a deterministic vector from text
     * hash
     */
    private float[] generateSimpleEmbedding(String text) {
        try {
            // Normalize text
            String normalized = normalizeText(text);

            // Generate hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));

            // Convert hash to float array
            int vectorSize = properties.getQdrant().getVectorSize();
            float[] embedding = new float[vectorSize];

            // Distribute hash bytes across vector dimensions
            for (int i = 0; i < vectorSize; i++) {
                int byteIndex = i % hash.length;
                embedding[i] = (hash[byteIndex] & 0xFF) / 255.0f;
            }

            // Normalize to unit vector for cosine similarity
            normalizeVector(embedding);

            return embedding;

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    /**
     * Normalize text for consistent embeddings
     */
    private String normalizeText(String text) {
        return text.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Normalize vector to unit length (for cosine similarity)
     */
    private void normalizeVector(float[] vector) {
        double magnitude = 0.0;
        for (float v : vector) {
            magnitude += v * v;
        }
        magnitude = Math.sqrt(magnitude);

        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
    }

    /**
     * Calculate cosine similarity between two embeddings
     *
     * @param embedding1 First embedding vector
     * @param embedding2 Second embedding vector
     * @return Similarity score (0 to 1, where 1 is identical)
     */
    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
