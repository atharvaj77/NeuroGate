# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **website** subdirectory of NeuroGate, containing a Next.js 14 landing page and documentation site. NeuroGate is an AI Gateway/Agent Kernel built on Java 21 Virtual Threads that provides intelligent LLM routing, PII protection, and semantic caching.

## Build and Development Commands

```bash
# Install dependencies
npm install

# Start development server (runs on localhost:3000)
npm run dev

# Build for production (static export to out/)
npm run build

# Run linter
npm run lint

# Start production server
npm start
```

## Architecture

### Directory Structure
- `app/` - Next.js App Router pages using TypeScript and Tailwind CSS
  - `page.tsx` - Main landing page
  - `docs/` - Technical documentation
  - `api/` - API documentation
  - `synapse/`, `cortex/`, `nexus/`, `forge/`, `reinforce/`, `pulse/`, `hive-mind/` - Feature-specific pages matching backend modules
  - `components/` - Shared React components
- `public/` - Static assets
- `out/` - Production build output (static HTML)

### Key Configuration
- **Static Export**: `next.config.js` configures `output: 'export'` for static hosting (GitHub Pages, Vercel, Netlify)
- **Tailwind Theme**: Custom colors defined in `tailwind.config.js` for each NeuroGate module (synapse/fuchsia, forge/pink, cortex/amber, nexus/cyan, reinforce/green)
- **TypeScript**: Strict mode enabled with `@/*` path alias

### Styling Conventions
- Use Tailwind utility classes
- Module-specific colors: `synapse`, `forge`, `cortex`, `nexus`, `reinforce` (mapped to Tailwind colors)
- Dark theme base: `dark-bg` (#000000), `dark-card` (#0a0a0a), `dark-border` (#1a1a1a)
- Custom animations available: `animate-gradient-x`, `animate-float`, `animate-pulse-slow`, `animate-shimmer`, `animate-pulse-glow`

## Relation to Parent Project

The website documents and showcases the Java backend located at `../core/`. The backend packages map to website pages:
- `com.neurogate.router` - Core routing engine
- `com.neurogate.vault` - PII protection (NeuroGuard)
- `com.neurogate.consensus` - Hive Mind multi-model consensus
- `com.neurogate.core.cortex` - Evaluation engine
- `com.neurogate.forge` - Fine-tuning/distillation
- `com.neurogate.reinforce` - Human-in-the-loop feedback
- `com.neurogate.rag` - RAG gateway (Nexus)
- `com.neurogate.synapse` - Prompt optimization
- `com.neurogate.pulse` - Real-time monitoring

## CI/CD

The parent project's `.github/workflows/ci-cd.yml` includes a `build-website` job that runs `npm ci && npm run build` in this directory.