# Technical Concerns

## 1. Mutability in Lexer Context
`LexerCtx` and its implementations rely on mutable `var` fields for state tracking (`text`, `lastLexeme`, `position`, `line`). This may lead to thread-safety issues if contexts are improperly shared and deviates from pure functional Scala patterns.

## 2. Type Safety Risks (`asInstanceOf`)
Frequent use of `asInstanceOf` in runtime parsing logic and macro expansions bypasses Scala's type safety. This increases the risk of `ClassCastException` if internal structures evolve inconsistently.

## 3. Side Effects in Macro Expansion
The `createTablesImpl` macro performs file I/O to write debug information (CSV tables) to the filesystem. This can lead to non-deterministic builds or failures in restricted environments (e.g., sandboxed CI).

## 4. Performance of Table Generation
LR(1) table construction is performed at compile-time within macros. Large or complex grammars significantly increase compilation times. A timeout mechanism exists but may trigger prematurely for complex valid grammars.

## 5. Technical Debt
Multiple `// todo` comments indicate unfinished features or acknowledged suboptimal code (e.g., opaque type issues, lack of parallelism).

## 6. DSL Limitations
Some grammar features (like guards or multiple cases in production definitions) are currently unimplemented, as evidenced by `NotImplementedError` or `report.errorAndAbort` in the macro code.
