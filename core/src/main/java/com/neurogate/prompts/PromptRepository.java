package com.neurogate.prompts;

import java.util.List;
import java.util.Optional;

public interface PromptRepository {
    PromptVersion saveVersion(PromptVersion version);

    Optional<PromptVersion> findVersionById(String versionId);

    PromptBranch saveBranch(PromptBranch branch);

    Optional<PromptBranch> findBranchByName(String branchName);

    List<PromptVersion> findVersionsByBranch(String branchName); // Needs recursive logic or storage support
}
