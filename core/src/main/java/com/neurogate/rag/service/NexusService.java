package com.neurogate.rag.service;

import com.neurogate.core.config.RagConfig;
import com.neurogate.rag.client.VectorStoreClient;
import com.neurogate.rag.client.VectorStoreClient.ScoredPoint;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NexusService {

    private final RagConfig ragConfig;
    private final EmbeddingService embeddingService;
    private final VectorStoreClient vectorStoreClient;
    private final ContextInjector contextInjector;

    /**
     * Enriches the ChatRequest with context from the vector store if RAG is
     * enabled.
     * 
     * @return The list of citations (doc IDs) used, or empty list if none.
     */
    public List<String> enrichRequest(ChatRequest request, String userId) {
        if (!shouldRun(request)) {
            return Collections.emptyList();
        }

        log.info("RAG Enabled for request. Starting Nexus retrieval flow for user: {}", userId);

        String query = getLastUserMessage(request);
        if (query == null)
            return Collections.emptyList();
        List<Double> queryVector = embeddingService.embed(query);

        String collection = ragConfig.getVectorDb().getCollection();
        int topK = ragConfig.getRetrieval().getTopK();

        // Allow overriding via request options
        if (request.getRagOptions() != null) {
            if (request.getRagOptions().getTopK() != null)
                topK = request.getRagOptions().getTopK();
            if (request.getRagOptions().getCollectionNames() != null
                    && !request.getRagOptions().getCollectionNames().isEmpty()) {
                collection = request.getRagOptions().getCollectionNames().get(0); // Support single collection for now
            }
        }

        Map<String, Object> filter = new HashMap<>();

        // Enforce Department Level ACLs
        String department = resolveDepartment(userId);
        if (department != null && !department.equals("admin")) {
            // Admin sees all, others only see their department documents
            filter.put(
                    ragConfig.getRetrieval().getAclField() != null ? ragConfig.getRetrieval().getAclField()
                            : "department",
                    department);
            log.debug("Applying ACL filter: department={}", department);
        }

        // Generate Sparse Vector for Hybrid Search
        VectorStoreClient.SparseVector sparseVector = embeddingService.embedSparse(query);
        log.debug("Generated sparse vector with {} non-zero terms", sparseVector.indices().size());

        List<ScoredPoint> docs = vectorStoreClient.search(collection, queryVector, sparseVector, topK, filter);

        if (docs.isEmpty()) {
            log.info("No relevant documents found for query.");
            return Collections.emptyList();
        }

        String contextBlock = contextInjector.formatContext(docs);
        injectSystemMessage(request, contextBlock);

        log.info("Injected {} documents into context.", docs.size());

        return docs.stream().map(ScoredPoint::id).collect(Collectors.toList());
    }

    private boolean shouldRun(ChatRequest request) {
        if (!ragConfig.isEnabled())
            return false;
        return Boolean.TRUE.equals(request.getRagEnabled());
    }

    private String getLastUserMessage(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty())
            return null;
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(request.getMessages().get(i).getRole())) {
                return request.getMessages().get(i).getStrContent();
            }
        }
        return null;
    }

    private void injectSystemMessage(ChatRequest request, String contextBlock) {
        // Try to append to existing system message
        boolean foundSystem = false;
        if (request.getMessages() != null) {
            for (Message msg : request.getMessages()) {
                if ("system".equalsIgnoreCase(msg.getRole())) {
                    // We need to handle content modification.
                    // Standard string content handling.
                    // TODO: Handle complex message types if Message uses Object content
                    // Check Message class definition to be sure.
                    msg.setContent(msg.getStrContent() + contextBlock); // Using getStrContent helper from ChatRequest
                                                                        // analysis
                    foundSystem = true;
                    break;
                }
            }
        }

        // If no system message, add one at the start
        if (!foundSystem) {
            Message sysMsg = Message.builder().role("system").content("You are a helpful assistant." + contextBlock)
                    .build();
            request.getMessages().add(0, sysMsg);
        }
    }

    private String resolveDepartment(String userId) {
        // Default implementation: Resolve department from user ID prefix.
        if (userId == null)
            return "unknown";
        if (userId.startsWith("admin"))
            return "admin";
        if (userId.startsWith("eng"))
            return "engineering";
        if (userId.startsWith("sales"))
            return "sales";
        if (userId.startsWith("mkt"))
            return "marketing";
        return "general";
    }
}
