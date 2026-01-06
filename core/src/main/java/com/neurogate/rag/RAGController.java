package com.neurogate.rag;

import com.neurogate.sentinel.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Dynamic RAG operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final DynamicRAGService ragService;

    /**
     * Determine optimal RAG strategy for a query
     */
    @PostMapping("/strategy")
    public ResponseEntity<RAGStrategy> determineStrategy(
            @RequestBody ChatRequest request) {

        RAGStrategy strategy = ragService.determineStrategy(request);
        return ResponseEntity.ok(strategy);
    }

    /**
     * Inject RAG context into a request
     */
    @PostMapping("/inject")
    public ResponseEntity<ChatRequest> injectContext(
            @RequestBody InjectContextRequest request) {

        ChatRequest enhanced = ragService.injectContext(
                request.getRequest(),
                request.getStrategy());

        return ResponseEntity.ok(enhanced);
    }

    /**
     * Add a document to the RAG system
     */
    @PostMapping("/documents")
    public ResponseEntity<String> addDocument(
            @RequestBody AddDocumentRequest request) {

        ragService.addDocument(
                request.getTitle(),
                request.getContent(),
                request.getSource());

        return ResponseEntity.ok("Document added successfully");
    }

    /**
     * Get RAG statistics
     */
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
