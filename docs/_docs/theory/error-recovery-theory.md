# Error Recovery Theory

When a lexer or parser encounters invalid input, it must decide what to do. A naïve approach — stop immediately — is fine for batch compilation but frustrating for interactive tools and IDEs. This page covers the theory behind error recovery and how Alpaca currently handles it.

## Types of Errors

**Lexical errors** — the lexer encounters a character that matches no token pattern. Example: `@` in a BrainFuck program.

**Syntactic errors** — the parser encounters a token sequence that matches no grammar rule. Example: `++]` (closing bracket without an opening bracket in the grammar).

**Semantic errors** — the input is syntactically valid but violates a semantic rule. Example: calling an undefined function in BrainFuck> (`bar!` without a prior `bar(...)` definition).

Each level has different recovery strategies.

## Lexical Error Recovery

### Strategy: Fail Fast (Default)

The simplest approach: throw an exception on the first unmatched character. This is Alpaca's default behavior.

```
RuntimeException: Unexpected character at line 1, position 5: '@'
```

### Strategy: Skip and Continue

Skip the unmatched character and resume tokenization from the next position. Alpaca supports this via `ErrorHandling.Strategy.IgnoreChar`. The skipped character is lost — the parser never sees it.

### Strategy: Catch-All Token

Add a low-priority pattern that matches any single character. The BrainFuck lexer uses this approach: `case "." => Token.Ignored` catches everything that isn't a BrainFuck command.

Alternatively, emit an `ERROR` token and let the parser decide what to do:

```scala sc:nocompile
case "." => Token["ERROR"]
```

### Strategy: Stop Gracefully

Stop tokenization and return the lexemes collected so far. Alpaca supports this via `ErrorHandling.Strategy.Stop`. Useful when processing a prefix of the input.

## Syntactic Error Recovery

Syntactic error recovery is more complex. The parser has a state stack and must somehow get back to a valid state to continue parsing.

### Panic Mode

The most common strategy. When the parser encounters an error:

1. Pop states from the stack until a state is found that has a valid action for a designated "synchronization" token (e.g., `;`, `}`, EOF)
2. Discard input tokens until the synchronization token is found
3. Resume parsing from the synchronized state

Panic mode is simple and reliable but can skip large chunks of input.

### Phrase-Level Recovery

More targeted than panic mode. The parser recognizes common error patterns and inserts or deletes specific tokens:

- Missing semicolon → insert a synthetic `;`
- Extra closing brace → delete it and continue
- Missing operand → insert a synthetic error operand

This produces better error messages but requires hand-written recovery rules for each error pattern.

### Error Productions

Add explicit grammar rules that match erroneous input:

```
Stmt → error ;
```

The `error` pseudo-terminal matches any sequence of tokens until a recovery point (the `;`). This approach is used by yacc/bison and gives the grammar author control over recovery behavior.

## What Alpaca Currently Supports

### Lexer

Alpaca provides four `ErrorHandling` strategies (see [Error Recovery](../lexer-error-recovery.md)):

| Strategy | Behavior |
|----------|----------|
| `Throw(ex)` | Abort immediately (default) |
| `IgnoreChar` | Skip one character |
| `IgnoreToken` | Skip to next match |
| `Stop` | Return partial results |

### Parser

Alpaca's parser currently has minimal error recovery:

- On a parse table miss (no action for the current state and token), `parse()` returns `null` for the result
- No panic mode, phrase-level recovery, or error productions
- No structured error information (error position, expected tokens, etc.)

### Semantic

Semantic error handling is up to your code. The BrainFuck interpreter uses `require()` in rule bodies to validate constraints:

```scala sc:nocompile
val FunctionCall: Rule[BrainAST] = rule:
  case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
    require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
    BrainAST.FunctionCall(name.value)
```

This throws a `RuntimeException` on semantic errors. For better error reporting, accumulate errors in the parser context rather than throwing.

## Cross-links

- See [Error Recovery](../lexer-error-recovery.md) for the lexer error handling API.
- See [Parser](../parser.md) for the `parse()` return type and null-result semantics.
- See [Parser Context](../parser-context.md) for accumulating semantic errors in a custom context.
