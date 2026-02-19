package com.neurogate.router.cache;

import com.neurogate.auth.RequiresRole;
import com.neurogate.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
@RequiresRole(Role.ADMIN)
public class CacheAdminController {

    private final TieredCacheService tieredCacheService;

    @Value("${neurogate.admin.cache-token:}")
    private String adminCacheToken;

    @DeleteMapping
    public ResponseEntity<Map<String, String>> invalidateAll(
            @RequestHeader(value = "X-Admin-Token", required = false) String providedToken) {

        if (adminCacheToken == null || adminCacheToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    Map.of(
                            "error", "admin_cache_token_not_configured",
                            "message", "Cache admin token is not configured."));
        }

        if (!adminCacheToken.equals(providedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(
                            "error", "forbidden",
                            "message", "Invalid admin token."));
        }

        tieredCacheService.invalidateAll();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "All cache tiers invalidated."));
    }
}
