# Pulse Module Documentation

The **Pulse** module is the real-time heartbeat of NeuroGate. It streams live metrics and events to connected clients via WebSockets, powering the "Real-Time Pulse Dashboard".

## Streaming

### `PulseStreamService`
**Package:** `com.neurogate.pulse`

**Purpose:**
Aggregates and broadcasts metrics to WebSocket clients. It runs a scheduled task to collect the latest operational stats (latency, RPS) and pushes them to the dashboard.

**Key Methods:**
- `broadcastMetrics()`: Runs every second. Collects metrics from `MeterRegistry` and publishes a `PulseEvent`.

---

## Eventing

### `PulseEventPublisher`
**Package:** `com.neurogate.pulse`

**Purpose:**
An internal event bus wrapper. It decouples the metric collection logic from the WebSocket transport layer. Services publish events here, and it handles the distribution to active WebSocket sessions.

**Key Methods:**
- `publish(PulseEvent event)`: Scopes and sends an event to all subscribers.
- `getConnectedClientCount()`: Returns the number of currently active dashboard viewers.

### `PulseWebSocketHandler`
**Package:** `com.neurogate.pulse`

**Purpose:**
The WebSocket endpoint handler. It manages client connections, handles handshakes, and routes outgoing messages.

**Context:**
Integrates with Spring WebSocket to provide the live data feed for the frontend dashboard.
