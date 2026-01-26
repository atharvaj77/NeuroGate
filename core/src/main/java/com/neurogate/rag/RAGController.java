package com.neurogate.rag;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.rag.client.VectorStoreClient.ScoredPoint;
import com.neurogate.rag.service.NexusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Dynamic RAG operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "Retrieval-Augmented Generation (Nexus)")
public class RAGController {

    private final DynamicRAGService ragService;
    private final NexusService nexusService;

    @Operation(summary = "Determine RAG strategy", description = "Analyze query complexity and determine optimal retrieval strategy")
    @ApiResponse(responseCode = "200", description = "Strategy determined")
    @PostMapping("/strategy")
    public ResponseEntity<RAGStrategy> determineStrategy(
            @RequestBody ChatRequest request) {

        RAGStrategy strategy = ragService.determineStrategy(request);
        return ResponseEntity.ok(strategy);
    }

    @Operation(summary = "Inject RAG context", description = "Augment a chat request with retrieved context documents")
    @ApiResponse(responseCode = "200", description = "Context injected")
    @PostMapping("/inject")
    public ResponseEntity<ChatRequest> injectContext(
            @RequestBody InjectContextRequest request) {

        ChatRequest enhanced = ragService.injectContext(
                request.getRequest(),
                request.getStrategy());

        return ResponseEntity.ok(enhanced);
    }

    @Operation(summary = "Add document", description = "Index a new document into the RAG knowledge base")
    @ApiResponse(responseCode = "200", description = "Document added successfully")
    @PostMapping("/documents")
    public ResponseEntity<String> addDocument(
            @RequestBody AddDocumentRequest request) {

        ragService.addDocument(
                request.getTitle(),
                request.getContent(),
                request.getSource());

        return ResponseEntity.ok("Document added successfully");
    }

    @Operation(summary = "Search documents", description = "Perform semantic search across the knowledge base")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    @PostMapping("/search")
    public ResponseEntity<List<ScoredPoint>> search(@RequestBody SearchRequest request) {
        // Default to a simulation user for now if auth is not fully integrated in this
        // controller
        String userId = "nexus-user-web";
        List<String> collections = request.getCollection() != null ? List.of(request.getCollection()) : null;

        return ResponseEntity.ok(nexusService.search(
                request.getQuery(),
                userId,
                request.getLimit(),
                collections));
    }

    @lombok.Data
    public static class SearchRequest {
        private String query;
        private Integer limit;
        private String collection;
    }

    @Operation(summary = "Get RAG statistics", description = "Retrieve RAG system statistics including query counts and strategy distribution")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    @GetMapping("/stats")
    public ResponseEntity<RAGStats> getStats() {
        RAGStats stats = ragService.getStats();
        return ResponseEntity.ok(stats);
    }

    @lombok.Data
    public static class InjectContextRequest {
        private ChatRequest request;
        private RAGStrategy strategy;
    }

    @lombok.Data
    public static class AddDocumentRequest {
        private String title;
        private String content;
        private String source;
    }
}
