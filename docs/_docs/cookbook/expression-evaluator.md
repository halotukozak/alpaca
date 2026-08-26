# Expression Evaluator

This guide builds a math expression evaluator supporting arithmetic (`+`, `-`, `*`, `/`), exponentiation (`**`), unary minus, parentheses, constants (`pi`), and functions (`sin`, `atan2`). It demonstrates Alpaca's operator precedence and conflict resolution.

**What you'll learn:** dynamic token names, named productions, the `before`/`after` DSL, and how to express a complete precedence hierarchy.

## The Lexer

```scala sc-name:calc-lexer
import halotukozak.alpaca.*

val CalcLexer = lexer:
  case "\\s+" => Token.Ignored
  case "#.*" => Token.Ignored
  case "\\*\\*" => Token["exp"]   // multi-char operators before single-char
  case literal @ ("\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | ",") =>
    Token[literal.type]
  case keyword @ ("pi" | "sin" | "atan2") =>
    Token[keyword.type]
  case x @ """(\d+\.\d*|\.\d+)([eE][+-]?\d+)?""" => Token["float"](x.toDouble)
  case x @ "\\d+" => Token["int"](x.toInt)
```

Note that `"\\*\\*"` (exponentiation) must appear before `"\\*"` (multiplication) to avoid shadowing.

## The Parser

Named productions (`"plus"`, `"times"`, etc.) let us reference specific alternatives in conflict resolution:

```scala sc-compile-with:calc-lexer sc:fail
import halotukozak.alpaca.*

object CalcParser extends Parser:
  val root: Rule[Double] = rule:
    case Expr(v) => v

  val Expr: Rule[Double] = rule(
    "plus"   { case (Expr(a), CalcLexer.`\\+`(_), Expr(b)) => a + b },
    "minus"  { case (Expr(a), CalcLexer.`-`(_), Expr(b)) => a - b },
    "times"  { case (Expr(a), CalcLexer.`\\*`(_), Expr(b)) => a * b },
    "divide" { case (Expr(a), CalcLexer.`/`(_), Expr(b)) => a / b },
    "exp"    { case (Expr(a), CalcLexer.`exp`(_), Expr(b)) => math.pow(a, b) },
    "uminus" { case (CalcLexer.`-`(_), Expr(a)) => -a },
    "pi"     { case CalcLexer.pi(_) => math.Pi },
    "sin"    { case (CalcLexer.sin(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.sin(a) },
    "atan2"  {
      case (CalcLexer.atan2(_), CalcLexer.`\\(`(_), Expr(y), CalcLexer.`,`(_), Expr(x), CalcLexer.`\\)`(_)) =>
        math.atan2(y, x)
    },
    { case (CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => a },
    { case CalcLexer.float(x) => x.value },
    { case CalcLexer.int(n) => n.value.toDouble },
  )
```

Without conflict resolution, this grammar is ambiguous -- the compiler reports shift/reduce conflicts for every binary operator.

## Conflict Resolution

<!-- sc:nocompile: the grammar is genuinely ambiguous, so CalcParser's macro expansion requires
     the given Resolutions[CalcParser.type] below to be in scope to avoid a ShiftReduceConflict --
     but that Resolutions instance needs the `production` selector, which is declared `protected`
     in halotukozak.alpaca (src/halotukozak/alpaca/parser.scala:23) and so isn't resolvable from a
     doc snippet compiled outside that package (or from any real downstream consumer's code).
     Filed as https://github.com/halotukozak-com/alpaca/issues/477. The `package halotukozak.alpaca`
     prefix workaround that works elsewhere on the site (see full-example.md) fails on this page with
     "this kind of statement is not allowed here", a separate Scaladoc snippet-compiler quirk. -->

```scala sc:nocompile
given Resolutions[CalcParser.type] = resolutions(
  // Exponentiation: right-associative, highest binary precedence
  CalcLexer.exp.before(production.uminus, production.exp, production.times, production.divide),
  production.exp.before(CalcLexer.`\\*`, CalcLexer.`/`),

  // Unary minus binds tighter than * /
  production.uminus.before(CalcLexer.`\\*`, CalcLexer.`/`),

  // Multiplication/division: left-associative, higher than +/-
  production.times.before(CalcLexer.`\\*`, CalcLexer.`/`),
  production.divide.before(CalcLexer.`\\*`, CalcLexer.`/`),

  // Addition/subtraction: left-associative, lowest binary precedence
  production.plus.after(CalcLexer.`\\*`, CalcLexer.`/`),
  production.plus.before(CalcLexer.`\\+`, CalcLexer.`-`),
  production.minus.after(CalcLexer.`\\*`, CalcLexer.`/`),
  production.minus.before(CalcLexer.`\\+`, CalcLexer.`-`),
)
```

The precedence hierarchy from highest to lowest: `**` > unary `-` > `*` `/` > `+` `-`.

- `before(tokens)` = prefer reducing this production over shifting those tokens
- `after(tokens)` = prefer shifting those tokens over reducing this production
- `Token.before(productions)` = prefer shifting this token over reducing those productions

## Running It

<!-- sc:nocompile: depends on the fully resolved CalcParser from the previous section, which
     doesn't compile as a doc snippet -- see https://github.com/halotukozak-com/alpaca/issues/477. -->

```scala sc:nocompile
val input = "sin(pi / 2) + 2 ** 3 * 4"
val (_, lexemes) = CalcLexer.tokenize(input)
val (_, result) = CalcParser.parse(lexemes)
println(result) // 33.0 (1.0 + 8.0 * 4.0)
```

## Exercises

- Add a modulo operator (`%`) with the same precedence as `*` and `/`
- Add variable assignment (`x = expr`) with right-associative binding
- Add `cos`, `tan`, and `sqrt` functions
