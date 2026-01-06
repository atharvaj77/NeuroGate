package com.neurogate.prompts;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
public class PersistentPromptRepository implements PromptRepository {

    private final JpaPromptVersionRepository versionRepo;
    private final JpaPromptBranchRepository branchRepo;

    @Override
    public PromptVersion saveVersion(PromptVersion version) {
        return versionRepo.save(version);
    }

    @Override
    public Optional<PromptVersion> findVersionById(String versionId) {
        return versionRepo.findById(versionId);
    }

    @Override
    public PromptBranch saveBranch(PromptBranch branch) {
        return branchRepo.save(branch);
    }

    @Override
    public Optional<PromptBranch> findBranchByName(String branchName) {
        return branchRepo.findByBranchName(branchName);
    }

    @Override
    public List<PromptVersion> findVersionsByBranch(String branchName) {
        Optional<PromptBranch> branchOpt = branchRepo.findByBranchName(branchName);
        if (branchOpt.isEmpty()) {
            return Collections.emptyList();
        }

        PromptBranch branch = branchOpt.get();
        List<PromptVersion> history = new ArrayList<>();
        String currentVersionId = branch.getHeadVersionId();

        // Limit depth to prevent infinite loops
        int safetyCounter = 0;
        int maxDepth = 1000;

        while (currentVersionId != null && safetyCounter < maxDepth) {
            Optional<PromptVersion> versionOpt = versionRepo.findById(currentVersionId);
            if (versionOpt.isEmpty()) {
                break;
            }

            PromptVersion version = versionOpt.get();
            history.add(version);
            currentVersionId = version.getParentVersionId();
            safetyCounter++;
        }

        return history;
    }
}
