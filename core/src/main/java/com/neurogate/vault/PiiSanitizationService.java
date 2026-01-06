package com.neurogate.vault;

import com.neurogate.vault.detector.PiiDetector;
import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.SanitizedPrompt;
import com.neurogate.vault.tokenizer.TokenVault;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for sanitizing text by detecting and replacing PII with tokens.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PiiSanitizationService {

    private final List<PiiDetector> piiDetectors;
    private final TokenVault tokenVault;
    private final com.neurogate.vault.detector.ImagePiiDetector imagePiiDetector;

    public SanitizedPrompt sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return new SanitizedPrompt(text, Map.of());
        }

        long startTime = System.currentTimeMillis();
        List<PiiEntity> entities = new java.util.ArrayList<>();

        for (PiiDetector detector : piiDetectors) {
            entities.addAll(detector.detect(text));
        }

        if (entities.isEmpty()) {
            log.debug("No PII detected in text");
            return createCleanPrompt(text);
        }

        log.info("Detected {} PII entities", entities.size());

        entities.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        StringBuilder sanitized = new StringBuilder(text);
        for (PiiEntity entity : entities) {
            String token = tokenVault.tokenize(entity);
            sanitized.replace(entity.getStart(), entity.getEnd(), token);
        }

        SanitizedPrompt result = new SanitizedPrompt();
        result.setSanitizedText(sanitized.toString());
        result.setOriginalText(text);
        result.setTokenMap(tokenVault.getTokenMappings());
        result.setDetectedEntities(entities);
        result.setContainsPii(true);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Sanitized text in {}ms. Replaced {} PII instances", duration, entities.size());

        return result;
    }

    /**
     * Sanitizes multimodal content (String or List of Maps)
     */
    public com.neurogate.vault.model.SanitizedContentResult sanitizeContent(Object content) {
        if (content instanceof String) {
            SanitizedPrompt prompt = sanitize((String) content);
            return com.neurogate.vault.model.SanitizedContentResult.builder()
                    .sanitizedContent(prompt.getSanitizedText())
                    .detectedEntities(prompt.getDetectedEntities())
                    .tokenMap(prompt.getTokenMap())
                    .containsPii(prompt.isContainsPii())
                    .build();
        } else if (content instanceof List) {
            // Handle Multimodal List
            List<?> parts = (List<?>) content;
            java.util.List<Object> sanitizedParts = new java.util.ArrayList<>();
            java.util.List<PiiEntity> allEntities = new java.util.ArrayList<>();
            boolean hasPii = false;

            for (Object part : parts) {
                if (part instanceof Map) {
                    Map<String, Object> partMap = new java.util.HashMap<>((Map<String, Object>) part);
                    String type = (String) partMap.get("type");

                    if ("text".equals(type) && partMap.containsKey("text")) {
                        SanitizedPrompt textRes = sanitize((String) partMap.get("text"));
                        if (textRes.isContainsPii()) {
                            hasPii = true;
                            allEntities.addAll(textRes.getDetectedEntities());
                            partMap.put("text", textRes.getSanitizedText());
                        }
                    } else if ("image_url".equals(type) && partMap.containsKey("image_url")) {
                        Map<String, String> imageUrl = (Map<String, String>) partMap.get("image_url");
                        String url = imageUrl.get("url");

                        List<PiiEntity> imageEntities = imagePiiDetector.detect(url);
                        if (!imageEntities.isEmpty()) {
                            hasPii = true;
                            allEntities.addAll(imageEntities);
                            // Redact by replacing with placeholder
                            partMap.put("image_url",
                                    Map.of("url", "https://via.placeholder.com/500x300?text=REDACTED+PII"));
                            log.warn("Redacted image with PII: {}", url);
                        }
                    }
                    sanitizedParts.add(partMap);
                } else {
                    sanitizedParts.add(part);
                }
            }

            return com.neurogate.vault.model.SanitizedContentResult.builder()
                    .sanitizedContent(sanitizedParts)
                    .detectedEntities(allEntities)
                    .tokenMap(tokenVault.getTokenMappings())
                    .containsPii(hasPii)
                    .build();
        }

        // Fallback for unknown types
        return com.neurogate.vault.model.SanitizedContentResult.builder()
                .sanitizedContent(content)
                .detectedEntities(List.of())
                .tokenMap(Map.of())
                .containsPii(false)
                .build();
    }

    public String desanitize(String sanitizedText) {
        if (sanitizedText == null || sanitizedText.isEmpty()) {
            return sanitizedText;
        }

        String restored = tokenVault.detokenizeText(sanitizedText);
        log.debug("Restored {} PII values in text", tokenVault.getStats().totalTokens());

        return restored;
    }

    public boolean containsPii(String text) {
        for (PiiDetector detector : piiDetectors) {
            if (!detector.detect(text).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public PiiStats getStats() {
        TokenVault.TokenStats vaultStats = tokenVault.getStats();
        return new PiiStats(
                vaultStats.totalTokens(),
                vaultStats.tokensByType());
    }

    private SanitizedPrompt createCleanPrompt(String text) {
        SanitizedPrompt prompt = new SanitizedPrompt();
        prompt.setSanitizedText(text);
        prompt.setOriginalText(text);
        prompt.setTokenMap(Map.of());
        prompt.setDetectedEntities(List.of());
        prompt.setContainsPii(false);
        return prompt;
    }

    public record PiiStats(int totalPiiDetected, Map<String, Integer> piiByType) {
    }
}
