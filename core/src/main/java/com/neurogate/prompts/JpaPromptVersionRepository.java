package com.neurogate.prompts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPromptVersionRepository extends JpaRepository<PromptVersion, String> {
}
