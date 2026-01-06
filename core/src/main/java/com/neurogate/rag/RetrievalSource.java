package com.neurogate.rag;

import java.util.List;

/**
 * Strategy interface for retrieving documents from various sources.
 */
public interface RetrievalSource {
    /**
     * Retrieve documents relevant to the query.
     *
     * @param query The search query or content
     * @param limit Maximum number of documents to retrieve
     * @return List of retrieved documents
     */
    List<Document> retrieve(String query, int limit);

    /**
     * Get the type of this source.
     *
     * @return The source type
     */
    RAGStrategy.DataSource getType();
}
