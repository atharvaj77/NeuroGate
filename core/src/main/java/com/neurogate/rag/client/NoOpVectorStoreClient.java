package com.neurogate.rag.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "neurogate.qdrant", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVectorStoreClient implements VectorStoreClient {

    @Override
    public void createCollection(String collectionName, int vectorSize) {
        // No-op
    }

    @Override
    public void upsert(String collectionName, List<VectorPoint> points) {
        // No-op
    }

    @Override
    public List<ScoredPoint> search(String collectionName, List<Double> vector, int topK, Map<String, Object> filter) {
        return Collections.emptyList();
    }

    @Override
    public List<ScoredPoint> search(String collectionName, List<Double> denseVector, SparseVector sparseVector,
            int topK, Map<String, Object> filter) {
        // No-op
        return Collections.emptyList();
    }
}
