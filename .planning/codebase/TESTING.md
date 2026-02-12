# Testing Patterns

## Framework
- **ScalaTest 3.2.19**: The primary testing framework.
- **Style**: `AnyFunSuite` with `Matchers` for readable assertions.

## Test Organization
- **Unit Tests**: Located in `test/src/alpaca/internal/`, testing individual components like `FirstSet` or `LazyReader`.
- **Integration Tests**: Found in `test/src/alpaca/integration/`. These test the full pipeline (Lexer -> Parser) using real-world grammar examples:
  - `JsonTest.scala`
  - `MathTest.scala`
- **DSL Tests**: Verify the correctness of the user-facing API and macro-driven refinements.

## Key Helpers
- `test/src/alpaca/TestHelpers.scala`: Contains shared utilities like `withLazyReader` for simulating file-based input.

## Coverage
- **Scoverage**: Configured in `build.mill` to track test coverage.
- **Execution**: Run via `mill alpaca.test`.
