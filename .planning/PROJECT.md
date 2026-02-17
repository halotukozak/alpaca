# Alpaca Documentation Extension

## What This Is

Comprehensive user-facing documentation for Alpaca, a type-safe lexer and parser library for Scala 3. This milestone adds 8 new documentation pages covering the full pipeline (lexer through parser), a navigation structure linking all docs together, and verified runnable code examples. The docs live as plain markdown in `docs/_docs/` and deploy via GitHub Pages.

## Core Value

Library users can learn and use every major Alpaca feature — lexer, parser, contexts, error recovery, conflict resolution, extractors, and stage transitions — through clear, example-driven documentation with code that actually compiles.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- ✓ Getting started / quick start guide — existing (`docs/_docs/getting-started.md`)
- ✓ Debug settings documentation — existing (`docs/_docs/debug-settings.md`)

### Active

<!-- Current scope. Building toward these. -->

- [ ] Lexer documentation (GH #189) — responsibilities, lifecycle, basic usage, DSL syntax
- [ ] Lexer context documentation (GH #194) — what LexerCtx is, how to use stateful lexing
- [ ] Lexer error recovery documentation (GH #190) — error types, recovery strategies, examples
- [ ] Between stages documentation (GH #192) — data flow from lexer to parser, formats, interfaces
- [ ] Parser documentation (GH #193) — how the parser processes tokens, rule definitions, key APIs
- [ ] Parser context documentation (GH #195) — parser state management, ParserCtx usage
- [ ] Parser conflict resolution documentation (GH #191) — shift/reduce conflicts, resolution strategies
- [ ] Extractors documentation (GH #197) — what extractors are, usage and use-cases
- [ ] Navigation structure — sidebar or table of contents linking all doc pages in pipeline order
- [ ] All code examples compile against current Alpaca version

### Out of Scope

<!-- Explicit boundaries. Includes reasoning to prevent re-adding. -->

- Contributor/internals documentation — this milestone is user-facing; internals covered only where they help users understand behavior
- Bug fixes (GH #110, #145, #147, #148, #150, #198, #230) — deferred to a future milestone
- Refactoring issues (GH #103, #178, #223-225, #227-229, #231-235) — deferred to a future milestone
- Video tutorials or interactive playground — too much scope for this milestone
- API reference / scaladoc generation — separate concern from narrative docs

## Context

- Alpaca is a Scala 3 library using macros for compile-time lexer/parser generation
- Built with Mill (1.1.2), Scala 3.8.1, published to Maven Central
- Existing docs: 2 pages (getting-started, debug-settings) in `docs/_docs/`
- Docs deploy via GitHub Actions workflow (`.github/workflows/docs.yml`)
- Code examples in getting-started.md use `sc-name` and `sc-compile-with` annotations for compilation verification
- The library has a natural pipeline: Lexer → BetweenStages → Parser → Extractors
- LexerCtx and ParserCtx enable stateful processing
- LR parsing with automatic parse table generation and conflict resolution

## Constraints

- **Tech stack**: Scala 3 / Mill — docs must use current API (version 0.0.4, Scala 3.8.1)
- **Format**: Plain markdown in `docs/_docs/`, served via GitHub Pages
- **Examples**: Must be runnable — code examples should compile against current Alpaca
- **Audience**: Primarily library users; mention internals only where helpful for understanding

## Key Decisions

<!-- Decisions that constrain future work. Add throughout project lifecycle. -->

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Pipeline order for docs | Natural learning progression: lexer → between stages → parser → extractors | — Pending |
| User-facing focus | Users are the primary audience; contributor docs can come later | — Pending |
| Runnable examples | Builds trust and catches API drift; uses existing `sc-name`/`sc-compile-with` pattern | — Pending |

---
*Last updated: 2026-02-17 after initialization*
