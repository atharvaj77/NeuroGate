package com.neurogate.prompts;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {
    PromptTemplate save(PromptTemplate template);

    Optional<PromptTemplate> findById(String templateId);

    List<PromptTemplate> findByTag(String tag);
}
