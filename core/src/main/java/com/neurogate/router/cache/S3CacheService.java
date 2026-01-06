package com.neurogate.router.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * S3 Cold Storage (L4 Cache).
 * Long-term retrieval storage for cache entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3CacheService {

    private final ObjectMapper objectMapper;

    @Value("${neurogate.cache.l4.enabled:false}")
    private boolean enabled;

    @Value("${neurogate.cache.l4.bucket-name:neurogate-cache-archive}")
    private String bucketName;

    @Value("${neurogate.cache.l4.access-key:#{null}}")
    private String accessKey;

    @Value("${neurogate.cache.l4.secret-key:#{null}}")
    private String secretKey;

    @Value("${neurogate.cache.l4.region:us-east-1}")
    private String region;

    @Value("${neurogate.cache.l4.retention-days:90}")
    private int retentionDays;

    private S3Client s3Client;

    /**
     * Initialize S3 client (lazy initialization)
     */
    private S3Client getS3Client() {
        if (s3Client == null) {
            if (accessKey != null && secretKey != null) {
                s3Client = S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                        .build();
            } else {
                // Use default credentials (IAM role, environment variables, etc.)
                s3Client = S3Client.builder()
                        .region(Region.of(region))
                        .build();
            }

            // Ensure bucket exists
            ensureBucketExists();
        }
        return s3Client;
    }

    /**
     * Store response in S3 cold storage
     *
     * @param cacheKey Cache key (hash of the request)
     * @param response Response to store
     */
    public void put(String cacheKey, ChatResponse response) {
        if (!enabled) {
            log.debug("S3 cold storage is disabled, skipping");
            return;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(response);

            // Store in S3 with metadata
            String objectKey = generateS3Key(cacheKey);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("application/json")
                    .storageClass(StorageClass.STANDARD_IA) // Infrequent Access (cheaper)
                    .metadata(java.util.Map.of(
                            "cache-key", cacheKey,
                            "model", response.getModel(),
                            "timestamp", String.valueOf(System.currentTimeMillis())))
                    .build();

            getS3Client().putObject(putRequest,
                    RequestBody.fromString(jsonValue, StandardCharsets.UTF_8));

            log.debug("Stored response in S3 cold storage: {}", objectKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for S3 storage", e);
        } catch (Exception e) {
            log.error("Failed to store response in S3", e);
        }
    }

    /**
     * Retrieve response from S3 cold storage
     *
     * @param cacheKey Cache key
     * @return ChatResponse if found, empty otherwise
     */
    public Optional<ChatResponse> get(String cacheKey) {
        if (!enabled) {
            return Optional.empty();
        }

        try {
            String objectKey = generateS3Key(cacheKey);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            String jsonValue = getS3Client().getObjectAsBytes(getRequest)
                    .asString(StandardCharsets.UTF_8);

            ChatResponse response = objectMapper.readValue(jsonValue, ChatResponse.class);

            log.debug("Retrieved response from S3 cold storage: {}", objectKey);
            return Optional.of(response);

        } catch (NoSuchKeyException e) {
            log.debug("Cache key not found in S3: {}", cacheKey);
            return Optional.empty();
        } catch (IOException e) {
            log.error("Failed to deserialize response from S3", e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve response from S3", e);
            return Optional.empty();
        }
    }

    /**
     * Delete old cache entries (lifecycle policy alternative)
     */
    public void cleanup() {
        if (!enabled) {
            return;
        }

        try {
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = getS3Client().listObjectsV2(listRequest);

            int deletedCount = 0;
            for (S3Object object : listResponse.contents()) {
                if (object.lastModified().isBefore(cutoffDate)) {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object.key())
                            .build();

                    getS3Client().deleteObject(deleteRequest);
                    deletedCount++;
                }
            }

            log.info("Cleaned up {} old cache entries from S3", deletedCount);

        } catch (Exception e) {
            log.error("Failed to cleanup S3 cache", e);
        }
    }

    /**
     * Generate S3 object key from cache key
     * Uses prefix structure for better organization: cache/YYYY/MM/DD/hash
     */
    private String generateS3Key(String cacheKey) {
        Instant now = Instant.now();
        String datePrefix = String.format("cache/%d/%02d/%02d",
                now.atZone(java.time.ZoneOffset.UTC).getYear(),
                now.atZone(java.time.ZoneOffset.UTC).getMonthValue(),
                now.atZone(java.time.ZoneOffset.UTC).getDayOfMonth());

        return datePrefix + "/" + cacheKey + ".json";
    }

    /**
     * Ensure S3 bucket exists, create if not
     */
    private void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            getS3Client().headBucket(headRequest);
            log.debug("S3 bucket exists: {}", bucketName);

        } catch (NoSuchBucketException e) {
            log.info("S3 bucket does not exist, creating: {}", bucketName);

            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            getS3Client().createBucket(createRequest);

            // Set lifecycle policy for automatic cleanup
            setLifecyclePolicy();

            log.info("Created S3 bucket: {}", bucketName);
        }
    }

    /**
     * Set lifecycle policy to automatically delete old objects
     */
    private void setLifecyclePolicy() {
        try {
            LifecycleRule rule = LifecycleRule.builder()
                    .id("DeleteOldCacheEntries")
                    .status(ExpirationStatus.ENABLED)
                    .expiration(LifecycleExpiration.builder()
                            .days(retentionDays)
                            .build())
                    .filter(LifecycleRuleFilter.builder()
                            .prefix("cache/")
                            .build())
                    .build();

            PutBucketLifecycleConfigurationRequest lifecycleRequest = PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                            .rules(rule)
                            .build())
                    .build();

            getS3Client().putBucketLifecycleConfiguration(lifecycleRequest);

            log.info("Set S3 lifecycle policy: delete after {} days", retentionDays);

        } catch (Exception e) {
            log.error("Failed to set S3 lifecycle policy", e);
        }
    }

    /**
     * Check if S3 cold storage is enabled and configured
     */
    public boolean isEnabled() {
        return enabled && bucketName != null && !bucketName.isBlank();
    }

    /**
     * Get storage statistics
     */
    public S3Stats getStats() {
        if (!enabled) {
            return S3Stats.builder().enabled(false).build();
        }

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = getS3Client().listObjectsV2(listRequest);

            long totalObjects = listResponse.keyCount();
            long totalSize = listResponse.contents().stream()
                    .mapToLong(S3Object::size)
                    .sum();

            return S3Stats.builder()
                    .enabled(true)
                    .totalObjects(totalObjects)
                    .totalSizeBytes(totalSize)
                    .bucketName(bucketName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get S3 stats", e);
            return S3Stats.builder().enabled(true).build();
        }
    }

    /**
     * S3 statistics model
     */
    @lombok.Builder
    @lombok.Data
    public static class S3Stats {
        private boolean enabled;
        private long totalObjects;
        private long totalSizeBytes;
        private String bucketName;
    }
}
