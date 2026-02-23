# Expression Evaluator

Alpaca's `before`/`after` DSL resolves operator precedence conflicts in the LR parse table at compile time, letting you build a fully evaluated expression parser with correct precedence and associativity.

> **Compile-time processing:** When you declare `override val resolutions = Set(...)`, the Alpaca macro bakes your precedence rules directly into the LR(1) parse table during compilation. No precedence checks happen at runtime -- the parser executes deterministically from a pre-resolved table.

## The Problem

Arithmetic grammars are ambiguous without explicit precedence declarations. The expression `1 + 2 * 3` can parse as `(1 + 2) * 3 = 9` or `1 + (2 * 3) = 11`, and the LR algorithm cannot choose between them on its own. Alpaca reports these as shift/reduce conflicts at compile time and gives you the `before`/`after` DSL to resolve them by declaring which productions take priority.

## Define the Lexer

```scala sc:nocompile
import alpaca.*

val CalcLexer = lexer:
  case num @ "[0-9]+(\\.[0-9]+)?" => Token["NUMBER"](num.toDouble)
  case "\\+"  => Token["PLUS"]
  case "-"    => Token["MINUS"]
  case "\\*"  => Token["TIMES"]
  case "/"    => Token["DIVIDE"]
  case "\\("  => Token["LPAREN"]
  case "\\)"  => Token["RPAREN"]
  case "\\s+" => Token.Ignored
```

The regex `[0-9]+(\.[0-9]+)?` matches both integers and decimals. `num.toDouble` converts the matched string to a `Double`, so `Token["NUMBER"]` carries a `Double` value -- this is what makes `Rule[Double]` the right type for the parser.

## Define the Parser

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Double] = rule(
    "plus"  { case (Expr(a), CalcLexer.PLUS(_),   Expr(b)) => a + b },
    "minus" { case (Expr(a), CalcLexer.MINUS(_),  Expr(b)) => a - b },
    "times" { case (Expr(a), CalcLexer.TIMES(_),  Expr(b)) => a * b },
    "div"   { case (Expr(a), CalcLexer.DIVIDE(_), Expr(b)) => a / b },
    { case (CalcLexer.`\(`(_), Expr(e), CalcLexer.`\)`(_)) => e },
    { case CalcLexer.NUMBER(n) => n.value },
  )
  val root: Rule[Double] = rule:
    case Expr(e) => e

  override val resolutions = Set(
    production.plus.before(CalcLexer.PLUS, CalcLexer.MINUS),
    production.plus.after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    production.minus.before(CalcLexer.PLUS, CalcLexer.MINUS),
    production.minus.after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    production.times.before(CalcLexer.TIMES, CalcLexer.DIVIDE, CalcLexer.PLUS, CalcLexer.MINUS),
    production.div.before(CalcLexer.TIMES, CalcLexer.DIVIDE, CalcLexer.PLUS, CalcLexer.MINUS),
  )
```

Reading `production.plus.before(CalcLexer.PLUS, CalcLexer.MINUS)`: when the parser has reduced the `plus` production and the next token is `+` or `-`, prefer the reduction. This gives `+` left associativity and equal precedence with `-`.

Reading `production.plus.after(CalcLexer.TIMES, CalcLexer.DIVIDE)`: when the conflict is between reducing `plus` and shifting `*` or `/`, prefer shifting. This makes `*` and `/` bind tighter.

## Run It

```scala sc:nocompile
import alpaca.*

val (_, lexemes) = CalcLexer.tokenize("3 + 4 * 2")
val (_, result)  = CalcParser.parse(lexemes)
// result: Double | Null  --  11.0 (not 14.0, because * binds tighter than +)
```

Always check for `null` before using the result -- `null` means the input did not match the grammar.

## Key Points

- `Rule[Double]` because `NUMBER` yields `Double` (`num.toDouble` in the lexer).
- `n.value` extracts the `Double` from the matched lexeme -- `n` is a `Lexeme`, not a `Double` directly.
- `resolutions` must be the **last `val`** in the parser object -- the macro reads top-to-bottom and must have seen all rule declarations before processing `resolutions`.
- Use `before`/`after` (not `alwaysBefore`/`alwaysAfter` -- the compiler error message suggests those names but they do not exist in the API).
- `production` is a `@compileTimeOnly` compile-time construct: valid only inside the `resolutions` value.

## See Also

- [Conflict Resolution](../conflict-resolution.html) -- `before`/`after` DSL reference, `Production(symbols*)` selector, token-side resolution
- [Parser](../parser.html) -- rule syntax, `root` requirement, `Rule[T]` types
- [Lexer](../lexer.html) -- token definition, `Token["NAME"](value)` constructor
