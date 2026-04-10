# Parser

The Alpaca parser transforms a `List[Lexeme]` into a typed result by matching token sequences against grammar rules. You define rules using pattern matching, and the macro builds an LR(1) parse table at compile time.

<details>
<summary>Under the hood: compile-time table generation</summary>

When you define `object MyParser extends Parser`, the Alpaca macro:

1. Reads every `Rule` val declaration
2. Builds an LR(1) parse table (states, transitions, actions)
3. Compiles semantic actions (your `case` bodies) into the action table
4. Reports grammar conflicts (`ShiftReduceConflict`, `ReduceReduceConflict`) as compile errors

At runtime, `parse()` executes the precomputed table. No grammar analysis happens during parsing.

</details>

## Defining a Parser

Extend `Parser` for a stateless parser, or `Parser[Ctx]` to carry custom state through reductions. The required entry point is `val root: Rule[R]` -- the macro uses this as the start symbol.

```scala sc:nocompile sc-name:BrainParser.scala sc-compile-with:BrainLexer.scala,BrainAST.scala
import alpaca.*

object BrainParser extends Parser:
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val While: Rule[BrainAST] = rule:
    case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) =>
      BrainAST.While(stmts)

  val Operation: Rule[BrainAST] = rule(
    { case BrainLexer.next(_) => BrainAST.Next },
    { case BrainLexer.prev(_) => BrainAST.Prev },
    { case BrainLexer.inc(_) => BrainAST.Inc },
    { case BrainLexer.dec(_) => BrainAST.Dec },
    { case BrainLexer.print(_) => BrainAST.Print },
    { case BrainLexer.read(_) => BrainAST.Read },
    { case While(whl) => whl },
  )
```

The macro reads both `val` and `def` declarations, but `val` is the conventional and recommended form for grammar rules.

## Rules and Productions

A `Rule[R]` is a named non-terminal that produces values of type `R`. Use `rule` to define one or more productions.

**Single production** -- colon syntax:

```scala sc:nocompile
val root: Rule[BrainAST] = rule:
  case Operation.List(stmts) => BrainAST.Root(stmts)
```

**Multiple productions** -- argument list:

```scala sc:nocompile
val Operation: Rule[BrainAST] = rule(
  { case BrainLexer.inc(_) => BrainAST.Inc },     // single-symbol: direct match
  { case BrainLexer.dec(_) => BrainAST.Dec },
  { case While(whl) => whl },                      // non-terminal reference
)
```

Multi-symbol productions match a tuple; single-symbol productions match directly (no parentheses). Each `{ case ... }` block must contain exactly one alternative.

## Terminal and Non-Terminal Matching

### Terminals

Use `MyLexer.TOKEN(binding)` to match a terminal. The binding is a `Lexeme` -- use `binding.value` for the extracted value:

```scala sc:nocompile
// Value-bearing: use binding.value
{ case BrainLexer.functionName(name) => name.value }  // name.value: String

// Structural: discard the binding
{ case BrainLexer.jumpForward(_) => ... }

// Backtick quoting for special-character token names
{ case BrainLexer.`\\+`(_) => ... }
```

### Non-Terminals

Use the rule name in unapply position. The binding has exactly type `R` from `Rule[R]`:

```scala sc:nocompile
// While(whl) extracts the BrainAST produced by the While rule
{ case While(whl) => whl }   // whl: BrainAST

// Recursive reference
{ case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) =>
    BrainAST.While(stmts) }
```

## EBNF Operators

`.Option` and `.List` on any `Rule[R]` express optional and repeated symbols without hand-written recursion.

**`.List`** produces `List[R]`. The BrainFuck parser uses this heavily -- the root rule matches zero or more operations:

```scala sc:nocompile
val root: Rule[BrainAST] = rule:
  case Operation.List(stmts) => BrainAST.Root(stmts)
  // stmts: List[BrainAST] -- zero or more operations
```

**`.Option`** produces `Option[R]`:

```scala sc:nocompile
val root = rule:
  case (BrainLexer.functionName(name), BrainLexer.functionCall(_).Option(call)) =>
    (name.value, call)   // call: Option[Lexeme]
```

Both operators also work on terminals, not only rules.

## Parsing Input

Call `parse(lexemes)` where `lexemes` comes from `tokenize()`:

```scala sc:nocompile sc-compile-with:BrainLexer.scala,BrainParser.scala,BrainAST.scala
val (_, lexemes) = BrainLexer.tokenize("++[>+<-]")
val (ctx, ast) = BrainParser.parse(lexemes)
// ctx: ParserCtx.Empty
// ast: BrainAST | Null -- the parsed result, or null if the input was rejected
```

The return type is a named tuple `(ctx: Ctx, result: T | Null)`. The result is `null` for invalid input -- not an exception. Always check for null:

```scala sc:nocompile
val (_, ast) = BrainParser.parse(lexemes)
ast.nn.eval(Memory())  // .nn asserts non-null
```

## Conflict Resolution

Ambiguous grammars produce compile-time errors. The BrainFuck grammar has no conflicts (all tokens are unambiguous), but arithmetic grammars do. See [Conflict Resolution](conflict-resolution.md) for the full `before`/`after` DSL.

Quick example:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Double] = rule(
    "plus" { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
    { case CalcLexer.NUMBER(n) => n.value },
  )
  val root = rule:
    case Expr(e) => e

  override val resolutions = Set(
    production.plus.before(CalcLexer.PLUS),  // left-associative
  )
```

`resolutions` must be the **last val** in the parser object. See [Conflict Resolution](conflict-resolution.md) for details.

See [Parser Context](parser-context.md) for custom state, [Extractors](extractors.md) for all pattern forms, and [Debug Settings](debug-settings.md) for compile-time debugging.
