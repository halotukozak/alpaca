# Parser Context

Parser context lets you carry mutable state through parsing reductions. Stateless parsers use `ParserCtx.Empty` by default; custom contexts carry domain-specific state like symbol tables, function registries, or error accumulators.

<details>
<summary>Under the hood: context threading</summary>

When you define `Parser[Ctx]`, the Alpaca macro verifies that `Ctx` extends `ParserCtx` and is a case class (Product). A `Copyable` instance is automatically provided for any `ParserCtx & Product` — you do not need to derive it explicitly. At runtime, the initial context is created via `Empty[Ctx]` (using constructor defaults) and the same object is passed to every rule reduction in a single `parse()` call.

</details>

## ParserCtx.Empty (Default)

When you extend `Parser` without a type parameter, the parser uses `ParserCtx.Empty`. No context definition is needed:

```scala sc:nocompile
import alpaca.*

object BrainParser extends Parser:    // uses ParserCtx.Empty
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val Operation: Rule[BrainAST] = rule(
    { case BrainLexer.inc(_) => BrainAST.Inc },
    { case BrainLexer.dec(_) => BrainAST.Dec },
    // ...
  )
```

## Custom Parser Context

The BrainFuck> extension adds function definitions and calls. To track which functions have been defined (so we can reject calls to undefined functions), we use a custom parser context:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class BrainParserCtx(
  functions: mutable.Set[String] = mutable.Set.empty,
) extends ParserCtx
```

Four rules apply:

1. **Must be a `case class`** -- `Copyable` is auto-derived for any `ParserCtx & Product`.
2. **All fields must have default values** -- `Empty[Ctx]` constructs the initial context from constructor defaults.
3. **Mutable collections are `val`; other mutable fields are `var`** -- mutate the collection contents, not the reference.

## Accessing Context in Rule Bodies

The `ctx` identifier is available inside every `rule { case ... }` body, typed as your specific `ParserCtx` subtype:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class BrainParserCtx(
  functions: mutable.Set[String] = mutable.Set.empty,
) extends ParserCtx 
object BrainParser extends Parser[BrainParserCtx]:
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val FunctionDef: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionOpen(_),
          Operation.List(ops), BrainLexer.functionClose(_)) =>
      require(ctx.functions.add(name.value), s"Function ${name.value} is already defined")
      BrainAST.FunctionDef(name.value, ops)

  val FunctionCall: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
      BrainAST.FunctionCall(name.value)

  val Operation: Rule[BrainAST] = rule(
    { case BrainLexer.inc(_) => BrainAST.Inc },
    { case BrainLexer.dec(_) => BrainAST.Dec },
    { case FunctionDef(fdef) => fdef },
    { case FunctionCall(call) => call },
    // ... other operations
  )
```

`FunctionDef` adds the function name to `ctx.functions`. `FunctionCall` checks that the name exists. Both see the same context object.

## Shared State Across Reductions

`ctx` is one object shared across all rule executions during a single `parse()` call. Mutations made in one rule body are visible to all subsequent reductions:

```scala sc:nocompile
// Parsing "foo(+++)foo!":
// 1. FunctionDef reduces "foo(+++)": ctx.functions.add("foo")
// 2. FunctionCall reduces "foo!": ctx.functions.contains("foo") => true
```

The initial context is created once per `parse()` call. There is no per-rule copy -- mutations accumulate.

## Positional Info from Lexemes, Not ctx

`ParserCtx` and `LexerCtx` are independent. The parser context has no `text`, `position`, or `line` fields. To access positional information, use the `Lexeme` binding:

```scala sc:nocompile
{ case BrainLexer.functionName(name) =>
    val funcName = name.value      // String -- the function name
    val pos = name.position        // Int -- character position from lexer
    val ln = name.line             // Int -- line number from lexer
    // ...
}
```

The `position` and `line` fields come from the lexer context snapshot. They are available when the lexer uses `LexerCtx.Default` or a custom context with `PositionTracking`/`LineTracking`. See [Lexer Context](lexer-context.md) for details.

See [Extractors](extractors.md) for the full `Lexeme` field reference. See [Parser](parser.md) for grammar rules and EBNF operators.
