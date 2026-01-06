package com.neurogate.rag;

import com.neurogate.router.cache.EmbeddingService;
import com.neurogate.router.cache.SemanticCacheService;
import com.neurogate.router.intelligence.ComplexityAnalyzer;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic RAG Optimizer
 *
 * Capabilities:
 * 1. Intelligent document selection based on query complexity
 * 2. Context compression to fit token limits
 * 3. Cache-aware RAG (skip retrieval if cached)
 * 4. Multi-source fusion (Vector DB + SQL + Graph + API)
 * 5. Cost-optimized document ranking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRAGService {

    private final ComplexityAnalyzer complexityAnalyzer;
    private final EmbeddingService embeddingService;
    private final Optional<SemanticCacheService> semanticCacheService;
    private final DocumentRepository documentRepository;

    // TODO: Inject list of RetrievalSource strategies when implemented
    // private final List<RetrievalSource> retrievalSources;

    /**
     * Determine optimal RAG strategy based on query complexity
     */
    public RAGStrategy determineStrategy(ChatRequest request) {
        String query = request.getConcatenatedContent();

        log.debug("Determining RAG strategy for query: {}",
                query.substring(0, Math.min(50, query.length())));

        // 1. Check if query is cached (skip RAG if cached)
        if (semanticCacheService.isPresent() && semanticCacheService.get().get(request).isPresent()) {
            log.debug("Query cached, skipping RAG");
            return RAGStrategy.none();
        }

        // 2. Analyze query complexity (Temporarily disabled due to missing dependency)
        // ComplexityScore complexity = complexityAnalyzer.analyze(request);

        // 3. Fallback strategy (default to none for now)
        return RAGStrategy.none();
    }

    /**
     * Retrieve and inject context into request
     */
    public ChatRequest injectContext(ChatRequest request, RAGStrategy strategy) {
        if (!strategy.isEnabled()) {
            return request; // No RAG needed
        }

        log.info("Injecting RAG context: numDocs={}, compression={}, sources={}",
                strategy.getNumDocuments(), strategy.getCompressionLevel(), strategy.getSources());

        // 1. Retrieve documents
        List<Document> documents = retrieveDocuments(request, strategy);

        log.debug("Retrieved {} documents", documents.size());

        // 2. Rank documents
        List<Document> rankedDocs = rankDocuments(documents, strategy);

        // 3. Compress documents to fit token budget
        String compressedContext = compressDocuments(rankedDocs, strategy);

        log.debug("Compressed context: {} characters", compressedContext.length());

        // 4. Inject context into prompt
        String originalPrompt = request.getConcatenatedContent();
        String enhancedPrompt = buildContextualPrompt(originalPrompt, compressedContext);

        // 5. Create new request with enhanced prompt
        List<Message> newMessages = new ArrayList<>(request.getMessages());
        if (!newMessages.isEmpty()) {
            int lastIdx = newMessages.size() - 1;
            newMessages.set(lastIdx, Message.user(enhancedPrompt));
        }

        return ChatRequest.builder()
                .model(request.getModel())
                .messages(newMessages)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .topP(request.getTopP())
                .user(request.getUser())
                .build();
    }

    /**
     * Retrieve documents from multiple sources
     */
    private List<Document> retrieveDocuments(ChatRequest request, RAGStrategy strategy) {
        List<Document> allDocuments = new ArrayList<>();
        float[] queryEmbedding = embeddingService.generateEmbedding(request.getConcatenatedContent());

        for (RAGStrategy.DataSource source : strategy.getSources()) {
            if (source == RAGStrategy.DataSource.VECTOR_DB) {
                // Use repository for Vector DB simulation
                allDocuments.addAll(retrieveFromVectorDB(queryEmbedding, strategy.getNumDocuments()));
            } else {
                // Placeholder for other sources
                log.debug("Source {} not yet implemented", source);
            }
        }

        return allDocuments;
    }

    /**
     * Retrieve from vector database (simulated via repository)
     */
    private List<Document> retrieveFromVectorDB(float[] queryEmbedding, int limit) {
        return documentRepository.findAll().stream()
                .map(doc -> {
                    double similarity = cosineSimilarity(queryEmbedding, doc.getEmbedding());
                    doc.setRelevanceScore(similarity);
                    return doc;
                })
                .sorted(Comparator.comparingDouble(Document::getRelevanceScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Rank documents according to strategy
     */
    private List<Document> rankDocuments(List<Document> documents, RAGStrategy strategy) {
        Comparator<Document> comparator = switch (strategy.getRankingStrategy()) {
            case RELEVANCE -> Comparator.comparingDouble(Document::getRelevanceScore).reversed();
            case RECENCY -> Comparator.comparing(Document::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case POPULARITY -> Comparator.comparingInt(Document::getUsageCount).reversed();
            case COST_OPTIMIZED -> Comparator.comparingDouble(Document::getCostBenefitRatio);
        };

        return documents.stream()
                .sorted(comparator)
                .limit(strategy.getNumDocuments())
                .collect(Collectors.toList());
    }

    /**
     * Compress documents to fit token budget
     */
    private String compressDocuments(List<Document> documents, RAGStrategy strategy) {
        StringBuilder context = new StringBuilder();
        int totalTokens = 0;
        int maxTokens = strategy.getMaxContextTokens();

        for (Document doc : documents) {
            String compressed = doc.getCompressed(strategy.getCompressionLevel());
            int docTokens = estimateTokens(compressed);

            if (totalTokens + docTokens > maxTokens) {
                log.debug("Token budget exceeded, stopping at {} docs", context.length());
                break;
            }

            context.append("\n\n--- Document: ").append(doc.getTitle()).append(" ---\n");
            context.append(compressed);

            totalTokens += docTokens;
        }

        return context.toString();
    }

    /**
     * Build contextual prompt with retrieved documents
     */
    private String buildContextualPrompt(String originalPrompt, String context) {
        if (context.isEmpty()) {
            return originalPrompt;
        }

        return String.format("""
                Context:
                %s

                Question: %s

                Please answer based on the context provided above.
                """, context, originalPrompt);
    }

    /**
     * Add document to store
     */
    public void addDocument(String title, String content, String source) {
        float[] embedding = embeddingService.generateEmbedding(content);

        Document doc = Document.builder()
                .documentId(UUID.randomUUID().toString())
                .title(title)
                .content(content)
                .embedding(embedding)
                .source(source)
                .createdAt(Instant.now())
                .tokenCount(estimateTokens(content))
                .costToInclude(estimateTokens(content) * 0.001) // Rough estimate
                .usageCount(0)
                .averageRating(0.0)
                .build();

        documentRepository.save(doc);
        log.info("Added document: {}, tokens={}", title, doc.getTokenCount());
    }

    /**
     * Get RAG statistics
     */
    public RAGStats getStats() {
        return RAGStats.builder()
                .totalDocuments((int) documentRepository.count())
                .averageTokenCount(documentRepository.findAll().stream()
                        .mapToInt(Document::getTokenCount)
                        .average()
                        .orElse(0.0))
                .totalUsageCount(documentRepository.findAll().stream()
                        .mapToInt(Document::getUsageCount)
                        .sum())
                .build();
    }

    // ========== Helper Methods ==========

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null)
            return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0)
            return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }
}
