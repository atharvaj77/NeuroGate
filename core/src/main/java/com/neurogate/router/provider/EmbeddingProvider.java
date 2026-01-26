package com.neurogate.router.provider;

import java.util.List;

/**
 * Extension interface for providers that support text embeddings.
 *
 * <p>Embedding providers generate vector representations of text,
 * useful for semantic search, similarity matching, and RAG applications.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (provider instanceof EmbeddingProvider embeddings) {
 *     List<Double> vector = embeddings.embed("Hello world");
 *     List<List<Double>> batchVectors = embeddings.embedBatch(documents);
 * }
 * }</pre>
 */
public interface EmbeddingProvider {

    /**
     * Generate an embedding for a single text.
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    List<Double> embed(String text);

    /**
     * Generate embeddings for multiple texts in a batch.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors
     */
    List<List<Double>> embedBatch(List<String> texts);

    /**
     * Get the embedding model being used.
     */
    String getEmbeddingModel();

    /**
     * Get the dimension of the embedding vectors.
     */
    int getEmbeddingDimension();

    /**
     * Get the maximum batch size for embedBatch.
     */
    default int getMaxBatchSize() {
        return 2048; // OpenAI default
    }

    /**
     * Get the maximum input tokens per text.
     */
    default int getMaxInputTokens() {
        return 8191; // text-embedding-3 default
    }
}
