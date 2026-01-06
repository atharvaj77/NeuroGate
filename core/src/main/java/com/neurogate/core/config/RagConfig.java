package com.neurogate.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nexus")
public class RagConfig {

    private boolean enabled = false;
    private String embeddingModel = "text-embedding-3-small";
    private VectorDb vectorDb = new VectorDb();
    private Retrieval retrieval = new Retrieval();

    public static class VectorDb {
        private String type = "qdrant";
        private String url = "localhost:6333";
        private String collection = "enterprise_docs";
        private String apiKey;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Retrieval {
        private int topK = 5;
        private double threshold = 0.75;
        private String aclField = "department_id";

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public String getAclField() {
            return aclField;
        }

        public void setAclField(String aclField) {
            this.aclField = aclField;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public VectorDb getVectorDb() {
        return vectorDb;
    }

    public void setVectorDb(VectorDb vectorDb) {
        this.vectorDb = vectorDb;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }
}
