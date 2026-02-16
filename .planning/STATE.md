# Project State - Alpaca Documentation Extension & Maintenance

## Current Phase
- **Phase 3: Advanced Guides & Bug Fixes** (Active)

## Recent Progress
- Completed Phase 2: Core Tutorials & Documentation.
- Implemented JSON Parser and Expression Evaluator tutorials.
- Created "Extractors" and "Context & State Management" guides.
- **Phase 3 Documentation:** Created deep-dive guides for **Conflict Resolution**, **Contextual Parsing** (including `BetweenStages`), and **Lexer Error Handling**.
- **Phase 3 Fixes:** 
    - Fixed `createTables` macro to handle missing parser rule errors correctly (Issue 230).
    - Implemented debug conflict resolution graph printing to Mermaid files (Issue 150).
    - Fixed production name transformation for names with special characters like hyphens (Issue 198).
- Updated `README.md` and `getting-started.md` with links to all new guides and tutorials.
- Verified all documentation through Scaladoc generation.
- Reorganized work into issue-specific branches for review.

## Next Steps
- **[Issue 148]** Fix Parser failure for multiline actions.
- **[Issue 147]** Fix Parser failure for lambdas.
- **[Issue 232]** Avoid unnecessary List initialization in `BetweenStages` macro (Phase 4 start).

## Active Context
- Focus on completing remaining bug fixes in Phase 3 while maintaining high documentation standards.
- Reorganized branches: `issue-227-*`, `issue-197-*`, `issue-150-*`, `issue-191-*`, `issue-198-230-*`.
- UAT files are maintained in `.planning/`.
