# Tutorial: Expression Evaluator

This tutorial builds a math expression evaluator supporting basic arithmetic (`+`, `-`, `*`, `/`), exponentiation
(`**`), unary minus, parentheses, constants (`pi`), and functions (`sin`, `atan2`). It highlights Alpaca's **operator
precedence** and **conflict resolution**.

## 1. Defining the Lexer

```scala sc:nocompile
import alpaca.*

val CalcLexer = lexer:
  case "\\s+" => Token.Ignored
  case "#.*" => Token.Ignored
  case "\\*\\*" => Token["exp"]   // multi-char operators first
  case literal@("\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | ",") =>
    Token[literal.type]
  case keyword@("pi" | "sin" | "atan2") =>
    Token[keyword.type]
  case x@"""(\d+\.\d*|\.\d+)([eE][+-]?\d+)?""" => Token["float"](x.toDouble)
  case x@"\\d+" => Token["int"](x.toInt)
```

## 2. Defining the Parser

**Named productions** (e.g., `"plus"`, `"times"`) let us reference specific alternatives in conflict resolution.

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val root: Rule[Double] = rule:
    case Expr(v) => v

  val Expr: Rule[Double] = rule(
    // Binary arithmetic
    "plus" { case (Expr(a), CalcLexer.`\\+`(_), Expr(b)) => a + b },
    "minus" { case (Expr(a), CalcLexer.`-`(_), Expr(b)) => a - b },
    "times" { case (Expr(a), CalcLexer.`\\*`(_), Expr(b)) => a * b },
    "divide" { case (Expr(a), CalcLexer.`/`(_), Expr(b)) => a / b },
    "exp" { case (Expr(a), CalcLexer.`exp`(_), Expr(b)) => math.pow(a, b) },

    // Unary operators
    "uminus" { case (CalcLexer.`-`(_), Expr(a)) => -a },

    // Constants and Functions
    "pi" { case CalcLexer.pi(_) => math.Pi },
    "sin" { case (CalcLexer.sin(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.sin(a) },
    "atan2" {
      case (CalcLexer.atan2(_), CalcLexer.`\\(`(_), Expr(y), CalcLexer.`,`(_), Expr(x), CalcLexer.`\\)`(_)) =>
        math.atan2(y, x)
    },

    // Parentheses and literals
    { case (CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => a },
    { case CalcLexer.float(x) => x.value },
    { case CalcLexer.int(n) => n.value.toDouble }
  )
```

## 3. Conflict Resolution

The grammar above is ambiguous — does `1 + 2 * 3` mean `(1 + 2) * 3` or `1 + (2 * 3)`? Override `resolutions` to
disambiguate:

```scala sc:nocompile
  override val resolutions = Set(
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

### Key Concepts:

- `production.<name>.before(tokens...)` — **reduce** over shifting those tokens (higher precedence)
- `production.<name>.after(tokens...)` — **shift** those tokens over reducing (lower precedence)
- `CalcLexer.<TOKEN>.before(productions...)` — prefer **shifting** this token over reducing those productions

## 4. Evaluating Expressions

```scala sc:nocompile
val input = "sin(pi / 2) + 2 ** 3 * 4"
val (_, lexemes) = CalcLexer.tokenize(input)
val (_, result) = CalcParser.parse(lexemes)

println(result) // 33.0 (1.0 + 8.0 * 4.0)
```
