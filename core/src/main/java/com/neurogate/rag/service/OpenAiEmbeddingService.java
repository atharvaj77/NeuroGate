package com.neurogate.rag.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Double> embed(String text) {
        float[] result = embeddingModel.embed(text);
        return toDoubleList(result);
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        EmbeddingResponse response = embeddingModel.call(
                new org.springframework.ai.embedding.EmbeddingRequest(texts,
                        org.springframework.ai.openai.OpenAiEmbeddingOptions.builder().build()));
        return response.getResults().stream()
                .map(org.springframework.ai.embedding.Embedding::getOutput)
                .map(this::toDoubleList)
                .toList();
    }

    private List<Double> toDoubleList(float[] floats) {
        if (floats == null)
            return List.of();
        java.util.ArrayList<Double> list = new java.util.ArrayList<>(floats.length);
        for (float f : floats) {
            list.add((double) f);
        }
        return list;
    }

    @Override
    public int getDimension() {
        return embeddingModel.dimensions();
    }

    @Override
    public com.neurogate.rag.client.VectorStoreClient.SparseVector embedSparse(String text) {
        // Simple heuristic implementation for Hybrid Search demonstration
        // 1. Tokenize
        String[] tokens = text.toLowerCase().split("\\s+");
        java.util.Map<Integer, Double> freqMap = new java.util.HashMap<>();

        // 2. Count Term Frequency
        for (String token : tokens) {
            // Simple hash-based indexing for demonstration
            int index = Math.abs(token.hashCode()) % 10000;
            freqMap.put(index, freqMap.getOrDefault(index, 0.0) + 1.0);
        }

        // 3. Convert to SparseVector
        java.util.List<Integer> indices = new java.util.ArrayList<>(freqMap.keySet());
        java.util.Collections.sort(indices);

        java.util.List<Double> values = new java.util.ArrayList<>();
        for (Integer idx : indices) {
            values.add(freqMap.get(idx));
        }

        return new com.neurogate.rag.client.VectorStoreClient.SparseVector(indices, values);
    }
}
