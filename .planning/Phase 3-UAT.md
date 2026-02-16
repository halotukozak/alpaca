# Phase 3 UAT - Advanced Guides & Bug Fixes

## Test Case 1: Conflict Resolution Guide
- **Status:** PASS
- **Details:** 
    - Created `docs/_docs/guides/conflict-resolution.md`.
    - Verified content covers Shift/Reduce vs Reduce/Reduce, `before`/`after` operators, and named productions.
    - Verified link in README.md and getting-started.md.

## Test Case 2: Contextual Parsing Deep-Dive
- **Status:** PASS
- **Details:** 
    - Created `docs/_docs/guides/contextual-parsing.md`.
    - Verified content covers Brace Matching, Indentation patterns, and `BetweenStages` hook logic.
    - Verified link in README.md and getting-started.md.

## Test Case 3: Lexer Error Handling (Issue 190)
- **Status:** PASS
- **Details:** 
    - Created `docs/_docs/guides/lexer-error-handling.md` documenting recovery strategies (catch-all tokens, context counting).
    - Verified link in README.md and getting-started.md.

## Test Case 4: Fix `createTables` Macro (Issue 230)
- **Status:** PASS
- **Details:** 
    - Verified code in `src/alpaca/internal/parser/createTables.scala` now includes `.getOrElse(report.errorAndAbort(...))` for the root rule.
    - Successfully prevents `NoSuchElementException` during macro expansion when `root` is missing.

## Test Case 5: Debug Conflict Resolution Graph (Issue 150)
- **Status:** PASS
- **Details:** 
    - Added `toMermaid` extension method to `ConflictResolutionTable`.
    - Updated `createTables` macro to save `.mmd` files in the debug directory.
    - Verified that `debug/CalcParser/conflictResolutions.mmd` is generated during tests.

## Test Case 6: Fix `production` macro name transformation (Issue 198)
- **Status:** PENDING
- **Details:** Reproduction attempted with `typeCheckErrors` but hit compiler cycles in test environment.

# Summary
Core documentation for Phase 3 is completed and verified. One critical bug fix (Issue 230) is implemented and verified. The remaining tasks are related to more bug fixes and a debug feature.
