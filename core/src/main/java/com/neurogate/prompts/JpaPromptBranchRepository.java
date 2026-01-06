package com.neurogate.prompts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPromptBranchRepository extends JpaRepository<PromptBranch, String> {
    Optional<PromptBranch> findByBranchName(String branchName);
}
