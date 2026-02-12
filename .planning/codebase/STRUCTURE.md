# Directory Structure

## Root Folders
- `src/alpaca/`: Public API and DSL definitions.
- `src/alpaca/internal/`: Core implementation details, partitioned by sub-system.
  - `lexer/`: Lexical analysis logic.
  - `parser/`: Parsing logic and table generation.
- `test/src/alpaca/`: Test suite mirroring the source structure.
  - `integration/`: End-to-end tests (JSON, Math grammars).
  - `internal/`: Unit tests for internal components.
- `benchmarks/`: Performance comparison against other Scala parsing libraries (fastparse, sly).
- `docs/`: Project documentation and assets.
- `debug/`: (Generated) Debug outputs from macro execution (e.g., CSV tables).
- `in/`: Sample input files for testing/debugging.

## Key Files
- `src/alpaca/lexer.scala`: Lexer DSL entry point.
- `src/alpaca/parser.scala`: Parser DSL entry point.
- `src/alpaca/internal/parser/Parser.scala`: Main runtime parser implementation.
- `build.mill`: Project build definition.
