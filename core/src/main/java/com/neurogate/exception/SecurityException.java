package com.neurogate.exception;

/**
 * Exception thrown when a security threat is detected.
 */
public class SecurityException extends NeuroGateException {

    private final SecurityThreat threat;

    public SecurityException(SecurityThreat threat, String message) {
        super(message, mapThreatToErrorCode(threat));
        this.threat = threat;
    }

    public SecurityException(SecurityThreat threat, String message, double confidenceScore) {
        super(message + " (confidence: " + String.format("%.2f", confidenceScore) + ")",
                mapThreatToErrorCode(threat));
        this.threat = threat;
    }

    private static ErrorCode mapThreatToErrorCode(SecurityThreat threat) {
        return switch (threat) {
            case PII_DETECTED -> ErrorCode.PII_DETECTED;
            case JAILBREAK_ATTEMPT -> ErrorCode.JAILBREAK_ATTEMPT;
            case PROMPT_INJECTION -> ErrorCode.PROMPT_INJECTION;
            default -> ErrorCode.SECURITY_THREAT_DETECTED;
        };
    }

    public SecurityThreat getThreat() {
        return threat;
    }

    /**
     * Types of security threats.
     */
    public enum SecurityThreat {
        PII_DETECTED,
        JAILBREAK_ATTEMPT,
        PROMPT_INJECTION,
        TOXIC_CONTENT,
        MALICIOUS_CODE,
        DATA_EXFILTRATION
    }
}
