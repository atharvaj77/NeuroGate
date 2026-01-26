# Pulse: Real-Time Observability

The `core.pulse` package provides a live heartbeat of the system.

## Architecture
1.  **Metric Collection**: Micrometer captures `latency`, `requests`, `tokens`.
2.  **Broadcasting**: `PulseStreamService.java` packages these into `PulseEvent` objects.
3.  **WebSocket**: `PulseEventPublisher.java` streams events to connected frontend clients via `/pulse/ws`.

## Dashboard
The frontend visualizes this data in a "Holographic Dashboard", showing:
- Active Agent Threads
- Real-time Cost/Sec
- Provider Latency Heatmap
