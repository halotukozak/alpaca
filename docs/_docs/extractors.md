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

// Backtick quoting for special-character names (e.g., if a lexer defines Token["\\+"])
{ case MyLexer.`\\+`(_) => ... }
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
| `binding.position` | `Int` | Character position (post-match) |
| `binding.line` | `Int` | Line number |

```scala sc:nocompile
import alpaca.*

val Num: Rule[Int] = rule:
  case CalcLexer.NUMBER(n) => n.value

val root = rule:
  case (Num(n), CalcLexer.COMMA(_), Num.Option(opt), CalcLexer.COMMA(_), Num.List(lst)) =>
    (n, opt, lst)
    // n: Int, opt: Option[Int], lst: List[Int]

// "1,,3"       => (1, None, List(3))
// "1,2,1 2 3"  => (1, Some(2), List(1, 2, 3))
```

## Lexeme Object Structure

When a terminal extractor binds a variable, the variable is a `Lexeme`.
A `Lexeme` is the record that crosses the lexer-to-parser boundary and carries both the extracted value and a snapshot of the lexer context at match time.

The user-visible fields are:

- **`name: String`** — the token type name (e.g., `"NUMBER"`, `"PLUS"`)
- **`value: T`** — the extracted value; the type depends on the `Token["NAME"](value)` definition in the lexer
- **`text: String`** — the raw matched characters; always a `String` regardless of token type
- **`position: Int`** — character position at match time (post-match; incremented by token length before the snapshot)
- **`line: Int`** — line number at match time
- **`fields: Map[String, Any]`** — all context fields at match time, accessible by name

`Lexeme` extends `Selectable`, so field access is type-safe at compile time — `id.position` returns `Int`, not `Any`.
The type refinement is encoded in the `tokenize()` return type and flows through to the parser.

A concrete example of the snapshot embedded in each lexeme:

```scala sc:nocompile
import alpaca.*

val MiniLang = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+"          => Token["PLUS"]
  case "\\s+"         => Token.Ignored

val (_, lexemes) = MiniLang.tokenize("42 + 13")
// lexemes(0): Lexeme("NUM",  42, Map("text" -> "42", "position" -> 3,  "line" -> 1))
// lexemes(1): Lexeme("PLUS", (), Map("text" -> "+",  "position" -> 5,  "line" -> 1))
// lexemes(2): Lexeme("NUM",  13, Map("text" -> "13", "position" -> 8,  "line" -> 1))

// Inside parser rules, access via dot notation:
//   n.value     == 42     (Int)
//   n.text      == "42"   (String — matched characters)
//   n.position  == 3      (Int — post-match character position)
//   n.line      == 1      (Int — line number)
```

Available fields depend on the `LexerCtx` used to build the lexer:
- `LexerCtx.Default` provides `text`, `position`, and `line`.
- Adding `LineTracking` (already included in `LexerCtx.Default`) provides `line`.
- Custom context fields appear if the lexer context declares them.

See [Between Stages](on-token-match.md) for the full Lexeme structure, context snapshot lifecycle, and how positional values are computed.

## Accessing Fields on a Bound Lexeme

After binding a terminal, use dot notation to access any field from the context snapshot:

```scala sc:nocompile
import alpaca.*

{ case CalcLexer.ID(id) =>
    val name = id.value      // String — the identifier text
    val raw  = id.text       // String — matched characters
    val pos  = id.position   // Int — character position
    val ln   = id.line       // Int — line number
    // Use for error reporting:
    ctx.errors.append(("undefined", id, id.line))
}
```

Field access is type-safe via the `Selectable` refinement on `Lexeme`. The `position` and `line` fields are available when the lexer uses `LexerCtx.Default` or a custom context with `PositionTracking`/`LineTracking`. Custom context fields (e.g., `name.squareBrackets`) are accessible if the lexer context declares them.

**Pitfall:** `position` records the post-match cursor position (after advancing by the token length), not the start position.
For a token `"42"` starting at column 1, `position` is 3. See [Between Stages](on-token-match.md) for the exact semantics.

See [Parser](parser.md) for grammar rules and [Between Stages](on-token-match.md) for how lexeme snapshots are constructed.
