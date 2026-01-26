# Changelog

All notable changes to the NeuroGate project will be documented in this file.

## [1.0.0] - 2024-05-22
### Added
- **Hive Mind Consensus**: Implementation of `core.consensus` package for 3-model voting (GPT-4, Claude, Gemini).
- **Architecture Visualization**: Added dynamic `ArchitectureDiagram.tsx` to the website showing PII Vault -> Cache -> Router flow.
- **Open Source Branding**: Complete website overhaul to reflect "Research Project" status.
    - Added "Community Driven" section.
    - added "Research Focus" section.
    - Updated all CTAs to point to GitHub.
- **Deep Space Theme**: New "Violet/Cyan" dark mode aesthetic with `glass` and `neon` effects.

### Changed
- **Terminology Update**: Renamed "Operating System" to **"AI Kernel"** to better reflect the middleware nature of the project.
- **Branding Pivot**: Removed all "Commercial/Startup" language to avoid legal ambiguity. Positioned strictly as an MIT-licensed research initiative.
- **Documentation**: Synchronized `website/docs` with `root/docs`, ensuring features like NeuroGuard and Data Flywheel are accurately described.

### Fixed
- **Website Build**: Resolved `MODULE_NOT_FOUND` errors by cleaning `.next` cache.
- **Linting**: Fixed unescaped quotes in `website/app/page.tsx` and components.
