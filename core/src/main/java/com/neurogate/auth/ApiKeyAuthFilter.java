package com.neurogate.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.exception.RateLimitException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_ID_ATTR = "neurogate.apiKeyId";
    public static final String API_ORG_ID_ATTR = "neurogate.orgId";

    private final ApiKeyService apiKeyService;
    private final UsageTracker usageTracker;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> rawKeyOpt = extractRawApiKey(request);
        if (rawKeyOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = rawKeyOpt.get();
        Optional<ApiKeyService.ValidatedApiKey> validated = apiKeyService.validateKey(rawKey);
        if (validated.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "invalid_api_key", "API key is invalid or expired", null);
            return;
        }

        ApiKeyService.ValidatedApiKey key = validated.get();
        try {
            usageTracker.enforceMonthlyLimit(key.orgId());
        } catch (RateLimitException ex) {
            UsageTracker.CurrentUsage usage = usageTracker.getCurrentUsage(key.orgId());
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", ex.getMessage(),
                    ex.getRetryAfterSeconds(), Map.of(
                            "period", usage.period(),
                            "requests", usage.requests(),
                            "requestLimit", usage.requestLimit(),
                            "requestsRemaining", usage.requestsRemaining()));
            return;
        }

        ApiPrincipal principal = new ApiPrincipal(
                "api-key:" + key.keyId(),
                key.orgId(),
                null,
                key.name(),
                key.keyId(),
                key.role());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                java.util.List.of(new SimpleGrantedAuthority(key.role().asAuthority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute(API_KEY_ID_ATTR, key.keyId().toString());
        request.setAttribute(API_ORG_ID_ATTR, key.orgId());

        try {
            filterChain.doFilter(request, response);
            if (response.getStatus() < 400) {
                usageTracker.trackRequest(key.keyId(), key.orgId());
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> extractRawApiKey(HttpServletRequest request) {
        String xApiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(xApiKey)) {
            return Optional.of(xApiKey.trim());
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.startsWith("ng_live_")) {
            return Optional.of(token);
        }
        return Optional.empty();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message,
            Long retryAfterSeconds) throws IOException {
        writeError(response, status, code, message, retryAfterSeconds, Map.of());
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message,
            Long retryAfterSeconds, Map<String, Object> details) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", code,
                "message", message,
                "details", details,
                "timestamp", Instant.now().toString()));
    }
}
