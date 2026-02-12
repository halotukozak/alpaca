# Architecture

## System Overview
Alpaca is a Lexer and Parser generator for Scala 3, leveraging macros to perform table generation at compile-time. It aims to provide a DSL for defining grammars that are both expressive and type-safe.

## Key Components
- **DSL Layer (`src/alpaca/`)**: Entry points for defining lexers and parsers. Provides the user-facing API.
- **Internal Lexer Engine (`src/alpaca/internal/lexer/`)**: Handles regex compilation (via dregex) and tokenization logic.
- **Internal Parser Engine (`src/alpaca/internal/parser/`)**: Implements LR(1) table construction and runtime stack-based parsing.
- **Macro Layer**: Deeply integrated into the parser and lexer definitions to generate efficient code and debug assets during compilation.

## Data Flow
1. **Grammar Definition**: User defines tokens and productions using Alpaca DSL.
2. **Compile-Time Generation**: Macros trigger table construction, reporting conflicts (shift/reduce, reduce/reduce) as compilation errors.
3. **Runtime Execution**: The generated tables are used by the runtime engine to process input streams into ASTs or other user-defined structures.

## Debugging Architecture
The system is designed to output debug information (like LR state tables in CSV format) to a `debug/` directory during compilation, aiding in grammar development.
