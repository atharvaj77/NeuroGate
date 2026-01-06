package com.neurogate.rag;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDocumentRepository implements DocumentRepository {

    private final Map<String, Document> store = new ConcurrentHashMap<>();

    @Override
    public Document save(Document document) {
        store.put(document.getDocumentId(), document);
        return document;
    }

    @Override
    public Optional<Document> findById(String documentId) {
        return Optional.ofNullable(store.get(documentId));
    }

    @Override
    public List<Document> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(String documentId) {
        store.remove(documentId);
    }
}
