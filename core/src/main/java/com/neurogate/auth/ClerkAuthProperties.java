package com.neurogate.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "neurogate.auth.clerk")
public class ClerkAuthProperties {
    private String issuer;
    private String audience;
    private String jwksUri;
}
