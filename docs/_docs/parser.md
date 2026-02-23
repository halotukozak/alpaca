# Parser

The Alpaca parser transforms a `List[Lexeme]` into a typed result by matching token sequences against grammar rules you define.
A parser is a Scala object extending `Parser` (for stateless parsing) or `Parser[Ctx]` (for parsers that maintain context state across reductions).
Like the lexer, the grammar is analyzed at compile time — the macro builds an LR(1) parse table from your rule declarations, catching conflicts before your code runs.

> **Compile-time processing:** When you define `object MyParser extends Parser`, the Alpaca macro reads every `Rule` `val` declaration, builds an LR(1) parse table, and compiles semantic actions into the table. Any grammar conflicts (`ShiftReduceConflict`, `ReduceReduceConflict`) are reported as compile errors. Only the resulting parser object is present at runtime.

## Defining a Parser

Extend `Parser` for a stateless parser (uses `ParserCtx.Empty` by default), or `Parser[Ctx]` to carry custom state through parsing.
The required entry point is a `val root: Rule[R]` — the macro uses this as the grammar start symbol.

[//]: # (todo: sprawdzić czy to prawda)
[//]: # (Note: `root` must be a `val`, not a `def`. Using `def root` causes the macro to miss the declaration.)

The simplest parser extends `Parser` with no type parameter:

```scala sc:nocompile
import alpaca.*

object Parser extends Parser:            // ParserCtx.Empty default
  val Expr: Rule[Double] = rule(
    { case (Expr(a), Lexer.`\\+`(_), Expr(b)) => a + b },
    { case Lexer.float(x) => x.value },
    { case Lexer.int(n) => n.value.toDouble },
  )
  val root: Rule[Double] = rule:
    case Expr(v) => v
```

For parsers that maintain state during parsing, extend `Parser[Ctx]` where `Ctx <: ParserCtx`.
The context is available inside every rule body through the `ctx` identifier:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class CalcContext(
  names: mutable.Map[String, Int] = mutable.Map.empty,
) extends ParserCtx derives Copyable

object Parser extends Parser[CalcContext]:
  val Expr: Rule[Int] = rule(
    { case Lexer.NUMBER(n) => n.value },
    { case Lexer.ID(id) => ctx.names.getOrElse(id.value, 0) },
  )
  val Statement: Rule[Unit | Int] = rule(
    { case (Lexer.ID(id), Lexer.ASSIGN(_), Expr(expr)) =>
        ctx.names(id.value) = expr },
    { case Expr(expr) => expr },
  )
  val root: Rule[Unit | Int] = rule:
    case Statement(stmt) => stmt
```

`ctx` is `@compileTimeOnly` — it is only valid syntactically inside `rule` definitions.
It cannot be referenced at class body level or inside `resolutions`.

## Rules and Productions

A `Rule[R]` is a named non-terminal that produces values of type `R`.
Use the `rule` function to define one or more productions for a rule.

**Single production** — use the colon syntax when a rule has exactly one case:

**Multiple productions** — pass each production as a separate argument when a rule has alternatives:

```scala sc:nocompile
import alpaca.*

object ArithParser extends Parser:
  // Single production — colon syntax
  val Num: Rule[Int] = rule:
    case Lexer.NUMBER(n) => n.value

  // Multiple productions — argument list
  val Expr: Rule[Int] = rule(
    { case (Expr(a), Lexer.PLUS(_), Expr(b))  => a + b },   // multi-symbol: tuple
    { case (Expr(a), Lexer.MINUS(_), Expr(b)) => a - b },
    { case Num(n) => n },                                         // single-symbol: direct
  )

  val root: Rule[Int] = rule:
    case Expr(e) => e
```

Each production is a partial function (`ProductionDefinition[R]`).
Multi-symbol productions match a tuple in the case branch; single-symbol productions match the symbol directly — not as a tuple.

**Guards** (`if ...`) in case branches are not supported (yet?) and cause a compile error: "Guards are not supported yet."
Each `{ case ... }` block must contain exactly one alternative — multiple `case` branches in one production block are not supported; each alternative must be a separate argument to `rule(...)`.

## Terminal and Non-Terminal Matching

### Terminals

Terminals come from the lexer object.
Use the token accessor with a binding variable: `MyLexer.TOKENNAME(binding)`.
For value-bearing tokens, use `binding.value` to access the extracted value.
For tokens used only for structural matching (operators, punctuation), use `_` to discard the binding.

```scala sc:nocompile
// Value-bearing token: use binding.value
{ case Lexer.NUMBER(n) => n.value }   // n.value: Int

// Structural token: discard the binding with _
{ case (Lexer.PLUS(_), Expr(b)) => b }

// Backtick quoting for special-character token names
{ case Lexer.`\\+`(_) => () }
{ case (Lexer.`\\(`(_), Expr(e), Lexer.`\\)`(_)) => e }
```

Token names that are not valid Scala identifiers — names containing `+`, `(`, `)`, reserved words like `if`, etc. — must be quoted with backticks when used as accessors.
See the [Lexer](lexer.html) page for the full token naming rules.

**Pitfall:** After `Lexer.NUMBER(n)`, the binding `n` is a `Lexeme`, not the extracted value.
The semantic content is `n.value`. The `Lexeme` also provides context fields, like `n.text` (matched string), `n.position`, and `n.line`.
Using `n` directly where an `Int` is expected is a type error.

### Non-Terminals

Non-terminals use the rule name in unapply position.
Each `Rule[R]` implements `unapply`, so a rule reference in a case branch extracts the value produced by that rule during the parse:

```scala sc:nocompile
{ case (Expr(left), Lexer.PLUS(_), Expr(right)) => left + right }
// Expr(left): Rule[Int].unapply — extracts the Int produced by the Expr rule
```

Rules can refer to themselves recursively.
The macro builds an LR(1) table that handles left recursion and mutual recursion without extra work on your part.

## EBNF Operators

Alpaca provides `.Option` and `.List` on any `Rule[R]` to express optional and repeated symbols without writing epsilon or recursive productions by hand.

**`.Option`** produces `Option[R]`. The macro generates two synthetic productions at compile time: an empty production (returns `None`) and a single-element production (returns `Some`).

**`.List`** produces `List[R]`. The macro generates two synthetic productions: an empty production (returns `Nil`) and a left-recursive accumulation production.

```scala sc:nocompile
import alpaca.*

object ApiParser extends Parser:
  val Num: Rule[Int] = rule:
    case Lexer.NUMBER(n) => n.value

  val root = rule:
    case (Num(n), Lexer.COMMA(_), Num.Option(opt), Lexer.COMMA(_), Num.List(lst)) =>
      (n, opt, lst)
      // opt: Option[Int]  — None if no number follows the first comma
      // lst: List[Int]    — zero or more numbers after the second comma

// "1,,3"       => (1, None, List(3))
// "1,2,1 2 3"  => (1, Some(2), List(1, 2, 3))
```

`.Option` and `.List` are compile-time pattern extractors — they instruct the macro to generate the appropriate synthetic productions.
They appear inside `case` patterns only. They cannot be called directly at runtime.

## Parsing Input

Call `MyParser.parse(lexemes)` where `lexemes` is the `List[Lexeme]` from `Lexer.tokenize(input).lexemes`.
The `parse()` method appends `Lexeme.EOF` internally before running the shift/reduce loop.

The return type is a named tuple `(ctx: Ctx, result: T | Null)`:
- `ctx` — the final parser context after all reductions (or `ParserCtx.Empty` for stateless parsers)
- `result` — the value produced by `root`, or `null` if parsing failed (the input was rejected by the grammar)

```scala sc:nocompile
import alpaca.*

val (ctx, result) = CalcParser.parse(Lexer.tokenize("1 + 2").lexemes)
// ctx:    ParserCtx.Empty (or CalcContext if parser has custom state)
// result: Double | Null   — 3 for valid input, null for invalid input
```

> **Pitfall:** `result` is `T | Null`, not `Option[T]`. If the input does not match the grammar, `result` is `null` — not an exception. Always check for null before using the result. (Parser error reporting with structured failure information is tracked in GH #65 and GH #51.)

[//]: # (todo: add links to GH refs)

## Conflict Resolution

[//]: # (todo: t should be moved to the another chapter)

Ambiguous grammars produce compile-time errors.
`ShiftReduceConflict` is reported when the parser cannot decide whether to shift the next token or reduce the current production.
`ReduceReduceConflict` is reported when two different rules match the same token sequence.

To resolve conflicts, name individual productions with a string literal placed before the production body, and declare precedence relationships in `resolutions = Set(...)` using the `before`/`after` DSL:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Int] = rule(
    "plus"  { case (Expr(a), Lexer.PLUS(_), Expr(b))  => a + b },
    "minus" { case (Expr(a), Lexer.MINUS(_), Expr(b)) => a - b },
    { case Lexer.NUMBER(n) => n.value },
  )
  val root = rule:
    case Expr(e) => e

  override val resolutions = Set(            // resolutions must be last
    production.plus.before(Lexer.PLUS, Lexer.MINUS),
    production.plus.after(Lexer.TIMES),
  )
```

`production.plus` refers to the production named `"plus"` declared above.
`before(tokens*)` means "this production takes precedence over those tokens" (prefer reduce here over a subsequent shift).
`after(tokens*)` is the inverse — those tokens take precedence (prefer shift).

`resolutions` must be defined after all rule declarations. The macro reads declarations top-to-bottom; placing `resolutions` before all rules may cause "Production with name X not found" errors.

See [Conflict Resolution](conflict-resolution.html) for full details on shift/reduce conflicts, reduce/reduce conflicts, named productions, and the `Production(symbols*)` selector.

---

See [Parsing Input with Context](parser-context.html) for how to maintain state during parsing with `ParserCtx`.

See [Extractors](extractors.html) for detailed coverage of terminal and non-terminal matching, EBNF patterns, and Lexeme field access in parser rules.
