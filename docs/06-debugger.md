# Time Travel Debugger

The `core.debugger` package provides "Holographic Replay" capabilities, allowing developers to step through an Agent's execution trace like a video.

## Key Features

### 1. Holographic Replay
Reconstruct the exact state of an Agent at any point in time.
- **Visual Timeline**: See every user message, tool call, and model response.
- **State Inspection**: View memory, variables, and context window at each step.

### 2. Fork & Branch
Test "What-If" scenarios by forking a session from a past step.
- **Endpoint**: `POST /sessions/{id}/fork`
- **Usage**: Correct a tool output manually and let the agent continue from there.

### 3. Comparison Mode
Compare two variations of a prompt side-by-side to see semantic differences (Cost, Latency, Similarity).

## Implementation
- **Snapshots**: Each step is saved as a `DebugSnapshot` in the `Trace`.
- **UI**: Accessible at `/debugger` in the web console.
