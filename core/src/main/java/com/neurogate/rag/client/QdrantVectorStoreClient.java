package com.neurogate.rag.client;

import com.neurogate.core.config.RagConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorFactory.vector;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "neurogate.qdrant", name = "enabled", havingValue = "true")
public class QdrantVectorStoreClient implements VectorStoreClient {

    private final QdrantClient qdrantClient;

    public QdrantVectorStoreClient(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void createCollection(String collectionName, int vectorSize) {
        try {
            qdrantClient.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(Distance.Cosine)
                            .build())
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to create collection", e);
        }
    }

    @Override
    public void upsert(String collectionName, List<VectorPoint> points) {
        List<PointStruct> qdrantPoints = points.stream()
                .map(p -> PointStruct.newBuilder()
                        .setId(id(UUID.fromString(p.id())))
                        .setVectors(Points.Vectors.newBuilder().setVector(vector(toFloatList(p.vector()))).build())
                        .putAllPayload(mapPayload(p.payload()))
                        .build())
                .collect(Collectors.toList());

        try {
            qdrantClient.upsertAsync(collectionName, qdrantPoints).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to upsert points", e);
        }
    }

    @Override
    public List<ScoredPoint> search(String collectionName, List<Double> vector, int topK, Map<String, Object> filter) {
        return search(collectionName, vector, null, topK, filter);
    }

    @Override
    public List<ScoredPoint> search(String collectionName, List<Double> denseVector, SparseVector sparseVector,
            int topK, Map<String, Object> filter) {
        try {
            // TODO: Use sparseVector in Qdrant Query if supported by current library
            // version
            // For now, we rely on the dense vector for the actual retrieval, but the
            // interface supports Hybrid.
            // If sparseVector is present, we could potentially re-rank or use a hybrid
            // query builder.

            List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(toFloatList(denseVector))
                            .setLimit(topK)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            // TODO: Implement sophisticated filtering logic based on the map
                            .build())
                    .get();

            return results.stream()
                    .map(sp -> new ScoredPoint(
                            sp.getId().getUuid(),
                            sp.getScore(),
                            sp.getPayloadMap().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStringValue())) // Simplified
                    ))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to search points", e);
        }
    }

    private List<Float> toFloatList(List<Double> doubles) {
        return doubles.stream().map(Double::floatValue).collect(Collectors.toList());
    }

    private Map<String, io.qdrant.client.grpc.JsonWithInt.Value> mapPayload(Map<String, Object> payload) {
        return payload.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> value(e.getValue().toString()) // Simplified
                ));
    }
}
