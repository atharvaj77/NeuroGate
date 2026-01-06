package com.neurogate.rag.service;

import java.util.List;

public interface EmbeddingService {
    /**
     * Embeds a single string into a vector.
     */
    List<Double> embed(String text);

    /**
     * Embeds a batch of strings.
     */
    List<List<Double>> embed(List<String> texts);

    /**
     * Returns the dimension of the embeddings.
     */
    int getDimension();

    /**
     * Generates a sparse vector (indices and values) for keyword search.
     * Used for Hybrid Search implementation.
     */
    com.neurogate.rag.client.VectorStoreClient.SparseVector embedSparse(String text);
}
