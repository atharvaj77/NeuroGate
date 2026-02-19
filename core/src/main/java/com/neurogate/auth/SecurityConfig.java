package com.neurogate.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.tenant.TenantRequestFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(ClerkAuthProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "neurogate.auth.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain secureFilterChain(
            HttpSecurity http,
            ApiKeyAuthFilter apiKeyAuthFilter,
            ClerkUserSyncFilter clerkUserSyncFilter,
            TenantRequestFilter tenantRequestFilter,
            BearerTokenResolver bearerTokenResolver,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
            ObjectMapper objectMapper) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v1/health", "/actuator/health", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    Map.of("error", "forbidden", "message", accessDeniedException.getMessage()));
                        }))
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(apiKeyAuthFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(clerkUserSyncFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(tenantRequestFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "neurogate.auth.enabled", havingValue = "false")
    public SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(false);
        return request -> {
            String apiKey = request.getHeader("X-API-Key");
            if (StringUtils.hasText(apiKey)) {
                return null;
            }
            String token = resolver.resolve(request);
            if (!StringUtils.hasText(token) && request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("ng_jwt".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            if (StringUtils.hasText(token) && token.startsWith("ng_live_")) {
                return null;
            }
            return token;
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(ClerkAuthProperties properties) {
        String issuer = properties.getIssuer();
        String jwksUri = properties.getJwksUri();

        if (!StringUtils.hasText(jwksUri) && StringUtils.hasText(issuer)) {
            String trimmedIssuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
            jwksUri = trimmedIssuer + "/.well-known/jwks.json";
        }

        if (!StringUtils.hasText(jwksUri)) {
            return token -> {
                throw new BadJwtException("JWT authentication is not configured");
            };
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OAuth2TokenValidator<Jwt> validator = StringUtils.hasText(issuer)
                ? JwtValidators.createDefaultWithIssuer(issuer)
                : JwtValidators.createDefault();

        if (StringUtils.hasText(properties.getAudience())) {
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(properties.getAudience());
            validator = new DelegatingOAuth2TokenValidator<>(validator, audienceValidator);
        }

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            Role role = Role.fromClaims(jwt.getClaims());
            JwtAuthenticationToken token = new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority(role.asAuthority())),
                    jwt.getSubject());
            return token;
        };
    }

    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "JWT audience does not match expected audience",
                    null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
