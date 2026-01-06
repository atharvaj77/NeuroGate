package com.neurogate.rag.client;

import java.util.List;
import java.util.Map;

public interface VectorStoreClient {

    void createCollection(String collectionName, int vectorSize);

    void upsert(String collectionName, List<VectorPoint> points);

    // Hybrid Search signature
    List<ScoredPoint> search(String collectionName, List<Double> denseVector, SparseVector sparseVector, int topK,
            Map<String, Object> filter);

    // Overload for backward compatibility (dense only)
    default List<ScoredPoint> search(String collectionName, List<Double> denseVector, int topK,
            Map<String, Object> filter) {
        return search(collectionName, denseVector, null, topK, filter);
    }

    record SparseVector(List<Integer> indices, List<Double> values) {
    }

    record VectorPoint(String id, List<Double> denseVector, SparseVector sparseVector, Map<String, Object> payload) {
        // Constructor for dense-only compatibility
        public VectorPoint(String id, List<Double> vector, Map<String, Object> payload) {
            this(id, vector, null, payload);
        }

        // Accessor for backward compatibility
        public List<Double> vector() {
            return denseVector;
        }
    }

    record ScoredPoint(String id, double score, Map<String, Object> payload) {
    }
}
