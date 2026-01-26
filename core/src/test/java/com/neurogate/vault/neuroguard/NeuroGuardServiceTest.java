package com.neurogate.vault.neuroguard;

import com.neurogate.vault.detector.ContextAwarePiiDetector;
import com.neurogate.vault.neuroguard.JailbreakDetector;
import com.neurogate.vault.neuroguard.PromptInjectionDetector;
import com.neurogate.vault.neuroguard.ToxicOutputFilter;
import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeuroGuardServiceTest {

    @Mock
    private PromptInjectionDetector injectionDetector;

    @Mock
    private JailbreakDetector jailbreakDetector;

    @Mock
    private ContextAwarePiiDetector piiDetector;

    @Mock
    private ToxicOutputFilter toxicOutputFilter;

    private NeuroGuardService neuroGuardService;

    @BeforeEach
    void setUp() {
        neuroGuardService = new NeuroGuardService(injectionDetector, jailbreakDetector, piiDetector, toxicOutputFilter);
    }

    @Test
    void testValidatePrompt_Safe() {
        when(injectionDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());
        when(jailbreakDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());
        when(piiDetector.detect(anyString())).thenReturn(List.of());

        neuroGuardService.validatePrompt("Hello world");
        // No exception
    }

    @Test
    void testValidatePrompt_PiiDetected() {
        when(injectionDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());
        when(jailbreakDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());

        PiiEntity ssnEntity = new PiiEntity(PiiType.SSN, "123-45-6789", 0, 11, 0.95);
        when(piiDetector.detect(anyString())).thenReturn(new ArrayList<>(List.of(ssnEntity)));

        // PII is masked, not blocked - returns sanitized content
        String result = neuroGuardService.validatePrompt("My SSN is 123-45-6789");

        // Result should be masked with <SSN> token
        assertThat(result).contains("<SSN>");
    }

    @Test
    void testAnalyzePrompt_PiiDetected() {
        when(injectionDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());
        when(jailbreakDetector.analyze(anyString())).thenReturn(ThreatDetectionResult.safe());

        PiiEntity apiKeyEntity = new PiiEntity(PiiType.API_KEY, "sk-12345", 0, 8, 0.99);
        when(piiDetector.detect(anyString())).thenReturn(new ArrayList<>(List.of(apiKeyEntity)));

        ThreatDetectionResult result = neuroGuardService.analyzePrompt("Here is my api_key: sk-12345");

        assertThat(result.isThreatDetected()).isTrue();
        assertThat(result.getThreatType()).isEqualTo(ThreatDetectionResult.ThreatType.PII_LEAK);
        assertThat(result.getConfidenceScore()).isEqualTo(0.99);
        // PII is masked, not blocked
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getSanitizedContent()).contains("<API_KEY>");
    }
}
