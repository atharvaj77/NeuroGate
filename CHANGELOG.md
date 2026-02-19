# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-02-18

### Added
- Global exception handling via `@RestControllerAdvice` with structured JSON responses.
- Admin cache invalidation endpoint: `DELETE /admin/cache` (token-protected).
- Unit tests for cache key isolation, sentinel request-copy behavior, context-aware SSN detection, and guard-chain concurrency.
- Streaming resilience wiring (reactive circuit breaker + retry) for streamed chat responses.

### Changed
- Upgraded Java toolchain and CI runtime to Java 21.
- Upgraded Docker runtime image to Java 21 (`eclipse-temurin:21-jre`).
- CORS now uses configurable origins and explicit allowed headers.
- Consensus judge model is configurable via `neurogate.consensus.judge-model`.
- Consensus confidence now reflects response agreement (Jaccard similarity), not a hardcoded value.
- OpenAI provider model catalog updated with `gpt-4o`, `gpt-4o-mini`, `o1`, and `o3-mini`.
- Cache key generation now includes prompt content + model + temperature + max tokens.
- Cache invalidation now clears L1 + Redis keys and triggers L3 clear.
- SSN regex detection narrowed to formatted SSNs; unformatted SSNs require context.
- Website claims adjusted: Hive Mind self-correction claim removed, module count corrected, and explicit demo-mode messaging added to Pulse/Forge.

### Fixed
- Duplicate `management` YAML block in `application.yml`.
- Grafana admin password env var now supports override with `${GRAFANA_PASSWORD:-...}`.
- Streaming request sanitization now deep-copies message lists to avoid mutating the original request.
- Security guard chain now uses a concurrency-safe guard list implementation.
