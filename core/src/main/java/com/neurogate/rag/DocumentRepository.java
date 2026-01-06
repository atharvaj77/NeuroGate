package com.neurogate.rag;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);

    Optional<Document> findById(String documentId);

    List<Document> findAll();

    long count();

    void deleteById(String documentId);
}
