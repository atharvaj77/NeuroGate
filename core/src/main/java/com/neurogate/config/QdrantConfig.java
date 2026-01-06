package com.neurogate.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.concurrent.ExecutionException;

/**
 * Configuration for Qdrant Vector Database.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "neurogate.qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QdrantConfig {

    private final NeuroGateProperties properties;

    @Bean
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client: {}:{}",
                properties.getQdrant().getHost(),
                properties.getQdrant().getGrpcPort());

        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                properties.getQdrant().getHost(),
                properties.getQdrant().getGrpcPort(),
                false) // TLS disabled for local development
                .build();

        return new QdrantClient(grpcClient);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCollection() {
        log.info("Initializing Qdrant collection: {}", properties.getQdrant().getCollectionName());

        try {
            QdrantClient client = qdrantClient();
            String collectionName = properties.getQdrant().getCollectionName();

            // Check if collection already exists
            boolean exists = collectionExists(client, collectionName);

            if (!exists) {
                log.info("Creating new Qdrant collection: {}", collectionName);
                createCollection(client, collectionName);
                log.info("Successfully created Qdrant collection: {}", collectionName);
            } else {
                log.info("Qdrant collection already exists: {}", collectionName);
            }

        } catch (Exception e) {
            log.warn("Failed to initialize Qdrant collection (Is Qdrant running?): {}", e.getMessage());
            log.warn("Application will continue but semantic caching will use in-memory storage only");
            // Don't throw exception - allow app to start without Qdrant
        }
    }

    /**
     * Check if a collection exists
     */
    private boolean collectionExists(QdrantClient client, String collectionName) {
        try {
            client.getCollectionInfoAsync(collectionName).get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    /**
     * Create a new vector collection
     */
    private void createCollection(QdrantClient client, String collectionName)
            throws ExecutionException, InterruptedException {

        VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(properties.getQdrant().getVectorSize()) // 384 for all-MiniLM-L6-v2
                .setDistance(Distance.Cosine)
                .build();

        client.createCollectionAsync(
                collectionName,
                vectorParams).get();
    }
}
