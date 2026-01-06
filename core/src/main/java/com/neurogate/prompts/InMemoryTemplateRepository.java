package com.neurogate.prompts;

import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTemplateRepository implements TemplateRepository {

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @Override
    public PromptTemplate save(PromptTemplate template) {
        templates.put(template.getTemplateId(), template);
        return template;
    }

    @Override
    public Optional<PromptTemplate> findById(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    @Override
    public List<PromptTemplate> findByTag(String tag) {
        return templates.values().stream()
                .filter(t -> t.getTags() != null && Arrays.asList(t.getTags()).contains(tag))
                .sorted(Comparator.comparing(PromptTemplate::getAverageRating,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}
