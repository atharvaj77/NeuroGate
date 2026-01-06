# Mermaid Architecture Diagram

This document explains the enhanced Mermaid.js architecture diagram and how to customize it.

## What's New

The ASCII architecture diagram has been replaced with a beautiful, interactive Mermaid.js flowchart that features:

- **Professional Styling**: Color-coded components (blue for users, green for trusted zones, red for external services, purple for cache)
- **Interactive Elements**: Hover effects and smooth transitions
- **Responsive Design**: Automatically adjusts for mobile, tablet, and desktop
- **Clear Visual Hierarchy**: Shows data flow with different line styles (solid for sanitized, dashed for untrusted)
- **Detailed Labels**: Rich information about each component
- **Legend**: Color-coded legend explaining each zone
- **Key Features Cards**: Three cards highlighting architecture benefits

## Components Created

### 1. MermaidDiagram.tsx
Generic Mermaid renderer component that:
- Initializes Mermaid with dark theme matching your brand colors
- Handles client-side rendering (SSR safe)
- Provides responsive SVG output
- Shows loading state while rendering

### 2. ArchitectureDiagram.tsx
Specific architecture diagram for NeuroGate:
- Contains the complete Mermaid flowchart definition
- Shows all three layers: PII Vault, Semantic Cache, Smart Router
- Includes metrics and observability
- Highlights security boundaries

## Architecture Flow

```
User Request (with PII)
    ↓
┌─────────────────────────────────────┐
│  1️⃣ PII Vault                      │
│  - Detects: EMAIL, SSN, PHONE, etc │
│  - Tokenizes: real → <TOKEN_1>     │
│  - Stores in request-scoped vault  │
└─────────────────────────────────────┘
    ↓ (sanitized prompt)
┌─────────────────────────────────────┐
│  2️⃣ Semantic Cache (Qdrant)        │
│  - Check similarity (> 0.95)       │
│  - Return if cache hit (40-60%)    │
└─────────────────────────────────────┘
    ↓ (cache miss)
┌─────────────────────────────────────┐
│  3️⃣ Smart Router                   │
│  - Route complex → OpenAI          │
│  - Route simple → Local SLM        │
│  - Circuit breakers & retries      │
└─────────────────────────────────────┘
    ↓
External LLM (never sees real PII)
    ↓ (sanitized response)
Restore PII tokens
    ↓
User receives complete response
```

## Color Coding

| Color | Zone | Purpose |
|-------|------|---------|
| 🔵 Blue | User Interaction | Entry and exit points |
| 🟢 Green | Trusted Zone | Internal processing, PII protection |
| 🔴 Red | External/Untrusted | OpenAI/Anthropic (no real PII) |
| 🟣 Purple | Cache Layer | Qdrant semantic cache |
| 🟠 Orange | Storage | Request-scoped token vault |

## Customizing the Diagram

### Change Colors

Edit `ArchitectureDiagram.tsx`:

```typescript
classDef user fill:#YOUR_COLOR,stroke:#YOUR_BORDER,stroke-width:3px,color:#fff;
classDef trusted fill:#YOUR_COLOR,stroke:#YOUR_BORDER,stroke-width:2px,color:#fff;
// etc.
```

### Add New Nodes

Add to the Mermaid diagram string:

```typescript
NEW_NODE["🆕 New Component<br/><small>Description</small>"]:::trusted
EXISTING_NODE --> NEW_NODE
```

### Change Layout Direction

At the top of the diagram:

```typescript
graph TD  // Top-Down (current)
// OR
graph LR  // Left-Right
// OR
graph TB  // Top-Bottom
```

### Modify Theme

Edit `MermaidDiagram.tsx`:

```typescript
mermaid.initialize({
  theme: 'dark', // or 'default', 'forest', 'neutral'
  themeVariables: {
    primaryColor: '#YOUR_COLOR',
    // ... more variables
  }
})
```

## Features

### 1. Visual Legend
After the diagram, there's a legend showing:
- User Interaction (Blue)
- Trusted Zone (Green)
- External (Red)
- Cache Layer (Purple)

### 2. Key Features Cards
Three cards explaining:
- Zero-Trust Design
- High Performance
- Intelligent Caching

### 3. Responsive Behavior
- Desktop: Full-size diagram with all details
- Tablet: Slightly smaller font, maintains structure
- Mobile: Compact view, horizontal scroll if needed

## Metrics & Observability

The diagram includes a Prometheus metrics node showing:
- Cache hit/miss rates
- PII detections
- Cost savings
- Latency measurements

This connects to key decision points (cache check, PII detection, routing).

## Line Styles

- **Solid Green (thick)**: Sanitized data flow (safe)
- **Dashed Red**: External LLM calls (untrusted zone)
- **Dotted**: Metadata/lookup operations
- **Bold**: Primary data path

## Subgraphs

The diagram uses subgraphs to group related components:

1. **PII Vault**: Detection → Tokenization → Storage
2. **Smart Router**: Decision → Forward → Receive → Restore

This creates visual boundaries and improves readability.

## Technical Details

### Dependencies
- `mermaid` v10.9.0: Diagram rendering engine
- React hooks: `useEffect`, `useRef`, `useState`
- Client-side only: Uses `'use client'` directive

### Performance
- Lazy initialization (only when component mounts)
- SSR safe (waits for client-side hydration)
- Unique IDs prevent conflicts
- Automatic cleanup

### Accessibility
- SVG output is scalable and screen-reader friendly
- High contrast colors (WCAG AA compliant)
- Clear text labels with emojis for visual cues

## Troubleshooting

### Diagram Not Showing
1. Check browser console for errors
2. Verify Mermaid is installed: `npm list mermaid`
3. Clear `.next` cache: `rm -rf .next && npm run dev`

### Syntax Errors
- Validate Mermaid syntax at: https://mermaid.live
- Check for missing brackets or quotes
- Ensure all nodes are defined before use

### Styling Issues
- Verify CSS classes are applied
- Check `globals.css` for Mermaid styles
- Inspect SVG in browser DevTools

## Advanced: Animation

Want to animate the diagram? Add to `MermaidDiagram.tsx`:

```typescript
// After rendering
const nodes = ref.current.querySelectorAll('.node')
nodes.forEach((node, i) => {
  node.style.opacity = '0'
  setTimeout(() => {
    node.style.opacity = '1'
  }, i * 100)
})
```

## Alternative Layouts

### Horizontal Flow
```typescript
graph LR  // Left to Right
```

Better for wide screens, shows process flow more linearly.

### Sequence Diagram
```typescript
sequenceDiagram
    User->>NeuroGate: Request with PII
    NeuroGate->>PII Vault: Sanitize
    PII Vault->>Cache: Check
    Cache-->>NeuroGate: Hit/Miss
    NeuroGate->>OpenAI: Sanitized
    OpenAI-->>NeuroGate: Response
    NeuroGate->>User: Restored
```

Shows temporal flow better for presentations.

## Export Options

### As Image
1. Open browser DevTools
2. Right-click SVG element
3. "Copy as PNG" or "Save as SVG"

### As PDF
Use browser print (Cmd/Ctrl + P) with:
- Background graphics: ON
- Scale: Fit to page

## Best Practices

1. **Keep It Simple**: Don't overcrowd the diagram
2. **Consistent Colors**: Use defined classes
3. **Clear Labels**: Use emojis + text
4. **Show Data Flow**: Use arrows meaningfully
5. **Group Related**: Use subgraphs
6. **Add Context**: Include descriptions in nodes

## Resources

- [Mermaid Documentation](https://mermaid.js.org/intro/)
- [Mermaid Live Editor](https://mermaid.live)
- [Flowchart Syntax](https://mermaid.js.org/syntax/flowchart.html)
- [Theme Configuration](https://mermaid.js.org/config/theming.html)

## Future Enhancements

Potential improvements:
- Click nodes to show more details
- Zoom and pan controls
- Toggle different deployment modes
- Animate data flow
- Show real-time metrics
- Interactive legend

---

**The Mermaid diagram makes your architecture visually stunning and much easier to understand at a glance!** 🎨
