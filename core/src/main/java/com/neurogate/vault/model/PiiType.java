package com.neurogate.vault.model;

/**
 * Types of Personally Identifiable Information (PII) that can be detected
 */
public enum PiiType {
    EMAIL("EMAIL"),
    SSN("SSN"), // Social Security Number
    PHONE("PHONE"),
    CREDIT_CARD("CREDIT_CARD"),
    IP_ADDRESS("IP_ADDRESS"),
    PERSON_NAME("PERSON"),
    ADDRESS("ADDRESS"),

    DATE_OF_BIRTH("DOB"),
    API_KEY("API_KEY");

    private final String code;

    PiiType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
