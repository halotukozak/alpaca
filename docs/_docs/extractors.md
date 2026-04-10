# Extractors

Parser rule bodies are partial functions -- everything on the left side of `=>` is a pattern. Extractors provide type-safe access to terminals (tokens), non-terminals (rule results), and EBNF operators.

<details>
<summary>Under the hood: compile-time pattern analysis</summary>

The Alpaca macro transforms patterns like `BrainLexer.inc(n)` into code that extracts a `Lexeme` from the parse stack. The macro reads each case pattern at compile time, identifies the symbols involved, constructs the grammar productions, and generates the parse table. What you write as patterns is syntactic sugar resolved against the grammar.

</details>

## Terminal Extractors

Use `MyLexer.TOKEN(binding)` to match a terminal. The `binding` is a `Lexeme` -- **not** the extracted value. Use `binding.value` to access the semantic content.

```scala sc:nocompile
// Value-bearing token: use binding.value
{ case BrainLexer.functionName(name) => name.value }   // name: Lexeme, name.value: String

// Structural token: discard the binding
{ case BrainLexer.jumpForward(_) => ... }

// Tokens with special characters need backtick quoting:
// e.g., if a lexer defines Token["\\+"], access it as MyLexer.`\\+`(_)
```

**Pitfall:** After `BrainLexer.functionName(name)`, the variable `name` is a `Lexeme`, not a `String`. Using `name` where a `String` is expected is a type error. Always use `name.value`.

## Non-Terminal Extractors

Use `Rule(binding)` to match a non-terminal. This calls `Rule[R].unapply`, extracting the value of type `R` produced during the parse:

```scala sc:nocompile
// While(whl) extracts the BrainAST produced by the While rule
{ case While(whl) => whl }   // whl: BrainAST

// Multiple non-terminals in a tuple
{ case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) =>
    BrainAST.While(stmts) }
```

Rules can refer to themselves recursively. The macro handles left recursion and mutual recursion automatically.

## Tuple Patterns

Multi-symbol productions match a **tuple**; single-symbol productions match **directly**:

```scala sc:nocompile
val Operation: Rule[BrainAST] = rule(
  // Multi-symbol: tuple pattern with parentheses
  { case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      BrainAST.FunctionCall(name.value) },
  // Single-symbol: no parentheses
  { case BrainLexer.inc(_) => BrainAST.Inc },
  { case While(whl) => whl },
)
```

## EBNF Extractors: .List

`Rule.List(binding)` binds to a `List[R]`. The macro generates a left-recursive accumulation production (empty → `Nil`, append → `list :+ elem`).

The BrainFuck parser uses `.List` for the root and for loop bodies:

```scala sc:nocompile
val root: Rule[BrainAST] = rule:
  case Operation.List(stmts) => BrainAST.Root(stmts)
  // stmts: List[BrainAST] -- zero or more operations

val While: Rule[BrainAST] = rule:
  case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) =>
    BrainAST.While(stmts)
```

`.List` also works on terminals:

```scala sc:nocompile
val root = rule:
  case BrainLexer.inc.List(incs) =>
    incs    // List[Lexeme] -- zero or more inc tokens
```

## EBNF Extractors: .Option

`Rule.Option(binding)` binds to an `Option[R]`. The macro generates an empty production (→ `None`) and a single-element production (→ `Some`).

```scala sc:nocompile
val root = rule:
  case (BrainLexer.functionName(name), BrainLexer.functionCall(_).Option(call)) =>
    (name.value, call)   // call: Option[Lexeme]
```

## Lexeme Fields

When a terminal extractor binds a variable, the variable is a `Lexeme` carrying both the value and a snapshot of the lexer context at match time. Access fields with dot notation:

| Field | Type | Description |
|-------|------|-------------|
| `binding.value` | Token-specific | The extracted semantic value |
| `binding.name` | `String` | The token name (e.g., `"functionName"`) |
| `binding.text` | `String` | The raw matched characters |
| `binding.position` | `Int` | Column position within the current line (post-match, resets on newline) |
| `binding.line` | `Int` | Line number |

```scala sc:nocompile
{ case BrainLexer.functionName(name) =>
    val funcName = name.value      // "foo": String
    val raw = name.text            // "foo": String (same here, but differs for transformed values)
    val pos = name.position        // Int -- character position from lexer
    val ln = name.line             // Int -- line number from lexer
}
```

Field access is type-safe via the `Selectable` refinement on `Lexeme`. The `position` and `line` fields are available when the lexer uses `LexerCtx.Default` or a custom context with `PositionTracking`/`LineTracking`. Custom context fields (e.g., `name.squareBrackets`) are accessible if the lexer context declares them.

**Note:** `position` records the post-match cursor position (after advancing by the token length), not the start position.

See [Parser](parser.md) for grammar rules and [Between Stages](between-stages.md) for how lexeme snapshots are constructed.
