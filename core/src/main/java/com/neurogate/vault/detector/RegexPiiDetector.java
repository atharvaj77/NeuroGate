package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based PII detector.
 * Detects common PII patterns (Email, SSN, Phone, etc.).
 */
@Slf4j
@Component
public class RegexPiiDetector implements PiiDetector {

    /**
     * Regex patterns for different PII types
     */
    private static final Map<PiiType, Pattern> PATTERNS = Map.of(
            // Email: standard email format
            PiiType.EMAIL, Pattern.compile(
                    "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),

            // SSN: formatted format only. Unformatted detection is context-aware.
            PiiType.SSN, Pattern.compile(
                    "\\b\\d{3}-\\d{2}-\\d{4}\\b"),

            // Phone: US formats like (555) 123-4567, 555-123-4567, 555.123.4567, 5551234567
            PiiType.PHONE, Pattern.compile(
                    "\\b(?:\\+?1[-.]?)?\\(?([0-9]{3})\\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\\b"),

            // Credit Card: 16-digit numbers with optional spaces/dashes
            PiiType.CREDIT_CARD, Pattern.compile(
                    "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b"),

            // IP Address: IPv4 format
            PiiType.IP_ADDRESS, Pattern.compile(
                    "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));

    @Override
    public List<PiiEntity> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<PiiEntity> entities = new ArrayList<>();

        // Scan for each PII type
        for (Map.Entry<PiiType, Pattern> entry : PATTERNS.entrySet()) {
            PiiType type = entry.getKey();
            Pattern pattern = entry.getValue();

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String value = matcher.group();

                // Additional validation for specific types
                if (isValidPii(type, value)) {
                    PiiEntity entity = new PiiEntity(
                            type,
                            value,
                            matcher.start(),
                            matcher.end(),
                            1.0 // High confidence for regex matches
                    );
                    entities.add(entity);
                    log.debug("Detected {} at position {}-{}: {}",
                            type, matcher.start(), matcher.end(), maskValue(value));
                }
            }
        }

        log.info("Detected {} PII entities in text", entities.size());
        return entities;
    }

    /**
     * Additional validation for specific PII types
     */
    private boolean isValidPii(PiiType type, String value) {
        switch (type) {
            case CREDIT_CARD:
                return isValidCreditCard(value);
            case SSN:
                return isValidSSN(value);
            default:
                return true;
        }
    }

    /**
     * Validate credit card using Luhn algorithm
     */
    private boolean isValidCreditCard(String cardNumber) {
        // Remove spaces and dashes
        String digits = cardNumber.replaceAll("[\\s-]", "");

        if (digits.length() != 16) {
            return false;
        }

        // Luhn algorithm
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Validate SSN (basic validation - not all 000 or 666)
     */
    private boolean isValidSSN(String ssn) {
        String digits = ssn.replaceAll("-", "");

        if (digits.length() != 9) {
            return false;
        }

        // First 3 digits cannot be 000 or 666
        String areaNumber = digits.substring(0, 3);
        if (areaNumber.equals("000") || areaNumber.equals("666")) {
            return false;
        }

        // Middle 2 digits cannot be 00
        String groupNumber = digits.substring(3, 5);
        if (groupNumber.equals("00")) {
            return false;
        }

        // Last 4 digits cannot be 0000
        String serialNumber = digits.substring(5, 9);
        return !serialNumber.equals("0000");
    }

    /**
     * Mask PII value for logging (show only first/last chars)
     */
    private String maskValue(String value) {
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    @Override
    public String getName() {
        return "RegexPiiDetector";
    }
}
