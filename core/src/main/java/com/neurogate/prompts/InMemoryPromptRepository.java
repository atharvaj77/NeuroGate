package com.neurogate.prompts;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPromptRepository implements PromptRepository {

    private final Map<String, PromptVersion> versions = new ConcurrentHashMap<>();
    private final Map<String, PromptBranch> branches = new ConcurrentHashMap<>();

    @Override
    public PromptVersion saveVersion(PromptVersion version) {
        versions.put(version.getVersionId(), version);
        return version;
    }

    @Override
    public Optional<PromptVersion> findVersionById(String versionId) {
        return Optional.ofNullable(versions.get(versionId));
    }

    @Override
    public PromptBranch saveBranch(PromptBranch branch) {
        branches.put(branch.getBranchName(), branch);
        return branch;
    }

    @Override
    public Optional<PromptBranch> findBranchByName(String branchName) {
        return Optional.ofNullable(branches.get(branchName));
    }

    @Override
    public List<PromptVersion> findVersionsByBranch(String branchName) {
        // Trace back version history from branch head

        PromptBranch branch = branches.get(branchName);
        if (branch == null) {
            return Collections.emptyList();
        }

        List<PromptVersion> history = new ArrayList<>();
        String currentVersionId = branch.getHeadVersionId();

        while (currentVersionId != null) {
            PromptVersion version = versions.get(currentVersionId);
            if (version == null)
                break;

            history.add(version);
            currentVersionId = version.getParentVersionId();
        }
        return history;
    }
}
