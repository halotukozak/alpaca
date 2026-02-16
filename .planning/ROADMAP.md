# Roadmap - Alpaca Documentation Extension & Maintenance

## Phase 1: Foundation & Refinement
- [x] Audit existing `getting-started.md` and `debug-settings.md` for clarity and consistency.
- [x] Set up a multi-page structure in `docs/_docs/`.
- [x] Ensure Scaladoc is correctly configured and generated for the public API.
- [x] **[Issue 189]** Add dedicated Lexer documentation page.
- [x] **[Issue 193]** Add dedicated Parser documentation page.
- [x] **[Issue 227]** Automate latest Mill version fetching in bootstrap script.

## Phase 2: Core Tutorials & Documentation
- [x] Implement and document the **JSON Parser** tutorial.
- [x] Implement and document the **Expression Evaluator** tutorial.
- [x] **[Issue 197]** Create "Extractors" tutorial and documentation.
- [x] **[Issue 194, 195]** Document Lexer and Parser Context (`ctx`) and state management.
- [x] **[Issue 192]** Document data flow and interface "Between Stages".
- [x] Update `README.md` to point to the new tutorials.

## Phase 3: Advanced Guides & Bug Fixes
- [x] Create the **Contextual Parsing** deep-dive (Issue 192, 194, 195).
- [x] **[Issue 191]** Create the **Conflict Resolution** guide (including shift/reduce).
- [x] **[Issue 150]** Implement debug conflict resolution graph printing to file.
- [x] **[Issue 190]** Document Lexer error recovery strategies.
- [x] **[Issue 230]** Fix `createTables` macro to handle missing parser rule errors correctly.
- [x] **[Issue 198]** Fix `production` macro name transformation (e.g., `if-else`).
- [ ] **[Issue 148]** Fix Parser failure for multiline actions.
- [ ] **[Issue 147]** Fix Parser failure for lambdas.

## Phase 4: Performance & Refactoring
- [ ] **[Issue 232]** Avoid unnecessary List initialization in `BetweenStages` macro.
- [ ] **[Issue 231]** Evaluate including ignored token info in `TokenInfo` for performance.
- [ ] **[Issue 145]** Optimize lexer to skip ignored characters before pattern matching.
- [ ] **[Issue 235]** Rename `BetweenStages` trait for improved clarity.
- [ ] **[Issue 228, 225]** Refactor `Csv` to use tuple-based representation.
- [ ] **[Issue 224, 229]** Investigate/Implement `opaque type` for `Token` and `IgnoredToken`.
- [ ] **[Issue 223]** Refactor `ValidName` to be an `opaque type`.

## Phase 5: Polish & Launch
- [ ] Review all code snippets for correctness.
- [ ] **[Issue 110]** Improve error messages across Lexer and Parser.
- [ ] **[Issue 151]** Fix overlapping regex detection in lexer cases.
- [ ] **[Issue 233]** Enhance `Showable` instance for `NamedTuple` (field names).
- [ ] Finalize the documentation site and push to GitHub Pages.
