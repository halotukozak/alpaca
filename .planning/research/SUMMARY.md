# Documentation Research Summary

## Competitive Analysis
- **FastParse**: Uses a dedicated website (GitHub Pages) with a mix of reference documentation, tutorials, and practical examples (JSON, Python, Scala). It emphasizes "easy parsing" and provides live demos.
- **Cats Parse**: Focuses on functional combinators and integration with the Cats ecosystem. Documentation is heavy on examples showing how to build complex parsers from simple ones. It uses Typelevel's documentation stack.

## Alpaca Gaps
- **Tutorials**: Currently only has a "Quick Start". Needs step-by-step guides for common formats (JSON, CSV, Expression Evaluator).
- **Theory & Implementation**: As an LR(1) generator, it should explain *why* this matters (no back-tracking, efficient) and how to handle common LR pitfalls like conflicts.
- **API Reference**: Needs structured Scaladoc. The macro-based nature might make standard Scaladoc tricky, but it's essential.
- **Advanced Features**: Context-aware lexing/parsing is a unique selling point but is only briefly mentioned. It needs a deep-dive tutorial.
- **Build Tooling**: Installation is well-documented for Mill and SBT, but more "copy-paste" examples for complex `scalacOptions` would be helpful.

## Recommended Content Structure
1. **Introduction**: What is Alpaca? Why use it over FastParse/Cats Parse?
2. **Getting Started**: 5-minute guide.
3. **Tutorials**:
   - Building a JSON Parser.
   - Expression Evaluator (Math).
   - Context-Aware Lexing (Nested comments or brace matching).
4. **User Guide**:
   - Lexer DSL Deep Dive.
   - Parser DSL Deep Dive.
   - Handling Conflicts (Shift/Reduce, Reduce/Reduce).
   - Debugging at Compile-Time.
5. **API Reference**: Generated Scaladoc.
6. **Performance & Benchmarks**: Comparative data.
