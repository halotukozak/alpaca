# Requirements - Alpaca Documentation Extension

## 1. High-Level Goals
- Transition from a single-page guide to a multi-page documentation site.
- Provide clear, runnable examples for all major features.
- Educate users on LR(1) parsing within the context of Alpaca.

## 2. Content Requirements
### 2.1 Tutorials
- **JSON Parser**: A complete walkthrough of building a JSON parser, covering tokens, recursive rules, and AST construction.
- **Expression Evaluator**: A guide to building a math expression evaluator, focusing on operator precedence and associativity.
- **Contextual Parsing**: A tutorial showing how to use `LexerCtx` for stateful lexing (e.g., tracking indentation or nested structures).

### 2.2 Deep Dives
- **Conflict Resolution**: Document how Alpaca handles shift/reduce and reduce/reduce conflicts, and how users can resolve them using `prec` or grammar restructuring.
- **Macro Debugging**: Detailed explanation of CSV table outputs and how to read them to debug grammar issues.

### 2.3 Reference
- **DSL Glossary**: A list of all DSL keywords and their functions.
- **Error Messages**: A guide to common compile-time error messages produced by Alpaca and how to fix them.

## 3. Technical Requirements
- Use Scaladoc for API reference.
- Ensure all code snippets in documentation are valid and compile (ideally using `snippet-compiler`).
- Maintain existing GitHub Pages deployment.
- Update `README.md` to link to the new documentation structure.
