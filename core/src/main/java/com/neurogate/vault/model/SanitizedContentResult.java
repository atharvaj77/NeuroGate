package com.neurogate.vault.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SanitizedContentResult {
    private Object sanitizedContent;
    private List<PiiEntity> detectedEntities;
    private Map<String, String> tokenMap;
    private boolean containsPii;
}
