# Parser Context

Parser context lets you carry mutable state through parsing reductions.
Stateless parsers use `ParserCtx.Empty` by default; custom contexts carry domain-specific state like symbol tables, error accumulators, or any state that evolves as productions are reduced.

> **Compile-time processing:** When you define `Parser[Ctx]`, the Alpaca macro verifies that `Ctx` extends `ParserCtx`, has `Copyable` derived, and generates the appropriate context threading code. At runtime, the initial context is created via `Empty[Ctx]` (using default constructor values) and the same object is passed to every rule reduction in a single `parse()` call.

## ParserCtx.Empty (Default)

When you extend `Parser` without a type parameter, the lexer uses `ParserCtx.Empty` internally.
No context definition is needed — this is the right choice for parsers that only care about the parsed value, not accumulated state.

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:           // uses ParserCtx.Empty by default
  val Expr: Rule[Int] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
    { case CalcLexer.NUMBER(n) => n.value },
  )
  val root: Rule[Int] = rule:
    case Expr(e) => e

val (_, result) = CalcParser.parse(CalcLexer.tokenize("1 + 2").lexemes)
// result: Int | Null  -- 3 for valid input, null for invalid input
// ctx is ParserCtx.Empty -- can be discarded with _
```

## Custom Parser Context

To carry state across reductions, define a `case class` extending `ParserCtx` with `derives Copyable`:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class CalcContext(
  names: mutable.Map[String, Int] = mutable.Map.empty,
  errors: mutable.ListBuffer[(tpe: String, value: Any, line: Int)] = mutable.ListBuffer.empty,
) extends ParserCtx derives Copyable
```

Four rules apply to every custom parser context:

1. **Must be a `case class`** — `Copyable.derived` requires `Mirror.ProductOf`, which only case classes provide.
2. **All fields must have default values** — `Empty[Ctx]` constructs the initial context from constructor defaults. Fields without defaults cause a compile error.
3. **`derives Copyable` is required** — `Parser[Ctx]` requires an implicit `Copyable[Ctx]`. Without it, the compiler reports an implicit not found error.
4. **Mutable collections are `val`; other mutable fields are `var`** — mutate the collection contents, not the reference.

## Accessing Context in Rule Bodies

The `ctx` identifier is available inside every `rule { case ... }` body and resolves to the current context instance, typed as your specific `ParserCtx` subtype:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser[CalcContext]:
  val Expr: Rule[Int] = rule(
    { case CalcLexer.NUMBER(n) => n.value },
    { case CalcLexer.ID(id) =>
        ctx.names.getOrElse(    // ctx is CalcContext here, not just ParserCtx
          id.value, {
            ctx.errors.append(("undefined", id, id.line))
            0
          },
        )
    },
  )
  val Statement: Rule[Unit | Int] = rule(
    { case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(expr)) =>
        ctx.names(id.value) = expr },  // mutates the shared context
    { case Expr(expr) => expr },
  )
  val root = rule:
    case Statement(stmt) => stmt
```

`ctx` is `@compileTimeOnly` — it is only valid syntactically inside `rule` bodies:

- Cannot use `ctx` in `resolutions = Set(...)` — compile error.
- Cannot use `ctx` at class body level or in field initializers — compile error.
- Cannot use `ctx` in companion object methods — compile error.

## Shared State Across Reductions

`ctx` is ONE object shared across all rule executions during a single `parse()` call.
Mutations made in one rule body are visible to all subsequent reductions in the same parse.

In the example above, the `Statement` rule writes to `ctx.names`, and the `Expr` rule reads from `ctx.names`.
Because they share the same context object, a value assigned in `Statement` is immediately readable in `Expr`:

```scala sc:nocompile
import alpaca.*

// Parsing "x = 42" then "x":
// 1. Statement reduces "x = 42": ctx.names("x") = 42
// 2. Expr reduces "x": ctx.names.getOrElse("x", 0)  => 42
//    Both rules see the same ctx object — the assignment is visible.

val (ctx, _) = CalcParser.parse(CalcLexer.tokenize("x = 42").lexemes)
// ctx.names contains "x" -> 42 after parsing completes
```

The initial context is created once per `parse()` call, and all reductions during that call operate on the same instance.
There is no per-rule copy — mutations accumulate across all reductions.

## Getting Positional Info from the Lexeme, Not ctx

`ParserCtx` and `LexerCtx` are completely independent.
The parser context has **no** `text`, `position`, or `line` fields.
To access positional information in parser rule bodies, use the fields on the `Lexeme` binding — not `ctx`:

```scala sc:nocompile
import alpaca.*

// WRONG: ParserCtx has no position field
{ case CalcLexer.NUMBER(n) => ctx.position }  // compile error: value position is not a member

// CORRECT: use the lexeme's context snapshot
{ case CalcLexer.ID(id) =>
    ctx.errors.append(("undefined", id, id.line))  // id.line from the lexeme snapshot
}
```

Every terminal binding (e.g., `id` in `CalcLexer.ID(id)`) is a `Lexeme` object carrying a snapshot of the lexer context at match time.
The available fields are:

| Field | Type | Description |
|-------|------|-------------|
| `binding.value` | Token-specific | The extracted semantic value (e.g., `Int` for a number token) |
| `binding.text` | `String` | The raw matched characters |
| `binding.position` | `Int` | Character position from the lexer context snapshot |
| `binding.line` | `Int` | Line number from the lexer context snapshot |

See [Extractors](extractors.html) for the full `Lexeme` field reference and how custom lexer context fields appear in bindings.

## The parse() Return Value

`parse()` returns a named tuple `(ctx: Ctx, result: T | Null)`:

- `ctx` — the **final** context after all reductions in the parse are complete
- `result` — the value produced by `root`, or `null` if the input was rejected by the grammar

```scala sc:nocompile
import alpaca.*

val (ctx, result) = CalcParser.parse(CalcLexer.tokenize("x = 42").lexemes)
// ctx.names now contains "x" -> 42 (accumulated across all reductions)
// result: Int | Null

// Always check result for null before using
if result != null then println(s"Result: $result, names: ${ctx.names}")
```

For stateless parsers, the context is `ParserCtx.Empty` and can be discarded:

```scala sc:nocompile
import alpaca.*

val (_, result) = StatelessParser.parse(StatelessLexer.tokenize("1 + 2").lexemes)
// result: Double | Null  -- 3.0 for valid input, null for invalid input
```

`result` is `T | Null`, not `Option[T]`.
If parsing fails (the input does not match the grammar), `result` is `null` — not an exception.
Always check for null before using the result.

---

See [Parser](parser.html) for grammar rules and EBNF operators.
