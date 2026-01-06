package com.neurogate.rag.service;

import com.neurogate.rag.client.VectorStoreClient.ScoredPoint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextInjector {

    public String formatContext(List<ScoredPoint> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n--- RELEVANT CONTEXT START ---\n");

        for (int i = 0; i < documents.size(); i++) {
            ScoredPoint doc = documents.get(i);
            sb.append(String.format("[Doc %d] (Score: %.2f)\n", i + 1, doc.score()));

            // Extract content from payload. Assuming key is "content" or "text" or "body"
            // We'll iterate to find a likely content field or dump all
            String content = extractContent(doc);
            sb.append(content).append("\n\n");
        }

        sb.append("--- RELEVANT CONTEXT END ---\n");
        sb.append(
                "Instructions: Use the above context to answer the user's question. If the answer is not in the context, say so.\n");

        return sb.toString();
    }

    private String extractContent(ScoredPoint doc) {
        if (doc.payload() == null)
            return "";

        // Priority keys
        if (doc.payload().containsKey("content"))
            return doc.payload().get("content").toString();
        if (doc.payload().containsKey("text"))
            return doc.payload().get("text").toString();

        // Fallback: simplified JSON dump
        return doc.payload().toString();
    }
}
