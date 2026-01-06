package com.neurogate.vault;

import com.neurogate.vault.detector.PiiDetector;
import com.neurogate.vault.detector.RegexPiiDetector;
import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.SanitizedPrompt;
import com.neurogate.vault.tokenizer.TokenVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiiSanitizationServiceTest {

    @Mock
    private PiiDetector piiDetector;
    @Mock
    private TokenVault tokenVault;
    @Mock
    private com.neurogate.vault.detector.ImagePiiDetector imagePiiDetector;

    private PiiSanitizationService service;

    @BeforeEach
    void setUp() {
        // Pass list of detectors
        service = new PiiSanitizationService(List.of(piiDetector), tokenVault, imagePiiDetector);
    }

    @Test
    void testSanitize_NoPii() {
        when(piiDetector.detect(anyString())).thenReturn(List.of());

        SanitizedPrompt result = service.sanitize("Hello world");

        assertFalse(result.isContainsPii());
        assertEquals("Hello world", result.getSanitizedText());
    }
}
