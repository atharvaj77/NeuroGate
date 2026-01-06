package com.neurogate.synapse;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiffService {

    /**
     * Compute the difference between two text blocks.
     */
    public DiffResult computeDiff(String original, String revised) {
        List<String> originalLines = Arrays.asList(original.split("\\n"));
        List<String> revisedLines = Arrays.asList(revised.split("\\n"));

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);

        return DiffResult.builder()
                .deltas(patch.getDeltas().stream()
                        .map(this::mapDelta)
                        .collect(Collectors.toList()))
                .build();
    }

    private DiffDelta mapDelta(AbstractDelta<String> delta) {
        return DiffDelta.builder()
                .type(delta.getType().name())
                .sourceStart(delta.getSource().getPosition())
                .sourceLines(delta.getSource().getLines())
                .targetStart(delta.getTarget().getPosition())
                .targetLines(delta.getTarget().getLines())
                .build();
    }

    @Data
    @Builder
    public static class DiffResult {
        private List<DiffDelta> deltas;
    }

    @Data
    @Builder
    public static class DiffDelta {
        private String type; // CHANGE, DELETE, INSERT
        private int sourceStart;
        private List<String> sourceLines;
        private int targetStart;
        private List<String> targetLines;
    }
}
