# Coding Conventions

## Language Style
- **Scala 3 Syntax:** Uses the new indentation-based syntax and `end` markers.
- **Type Safety:** Strong emphasis on type safety, but with pragmatic use of `asInstanceOf` in internal engine code where necessary.
- **Null Safety:** Configured with `-Yexplicit-nulls` to avoid unexpected null pointer exceptions.

## Naming
- **Files:** camelCase, typically matching the primary class or object defined within.
- **Classes/Objects:** PascalCase.
- **Methods/Variables:** camelCase.

## Formatting
- **Tool:** Managed via `.scalafmt.conf`.
- **Indentation:** 2 spaces (standard Scala convention).

## Patterns
- **Internal/External Split:** Strict separation between public API (`alpaca/`) and implementation (`alpaca/internal/`).
- **Macro-Heavy Implementation:** Uses macros extensively for performance and compile-time validation.
- **Immutable by Default:** Prefers immutable structures, though internal state tracking in the lexer context uses mutability for performance.
