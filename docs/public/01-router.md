# Iron Gate: Resilience & Neural Routing

The `core.router` package provides the "Iron Gate" resilience layer, ensuring 99.99% reliability for agentic workloads.

## Key Features

### Neural Routing
Instead of static round-robin, NeuroKernel uses a **Neural Score** (0.0 - 1.0) to select the best provider.
- **Formula**: `Score = (0.4 * Stability) + (0.3 * Speed) + (0.3 * Cost)`
- **Implementation**: `NeuralRouteStrategy.java`
- **Dynamic Updates**: Scores are recalculated every 30 seconds based on Micrometer metrics.

### Hedging (Scatter-Gather)
For critical requests or Consensus operations, we launch multiple requests in parallel.
- **Service**: `HedgingService.java`
- **Pattern**: 
  - `hedge()`: Returns the *first* successful response (Speed).
  - `executeAll()`: Returns *all* successful responses (Consensus).

### Adaptive Rate Limiting
Static limits fail under load. We use dynamic limits based on latency.
- **Service**: `AdaptiveRateLimiter.java`
- **Logic**: If `latency > 1000ms`, decrease limit. If `latency < 200ms`, increase limit.
