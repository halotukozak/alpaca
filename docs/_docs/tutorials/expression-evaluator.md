# Tutorial: Expression Evaluator

In this tutorial, we will build a powerful expression evaluator that supports:
- Basic arithmetic (`+`, `-`, `*`, `/`, `%`)
- Exponentiation (`**`) and Floor division (`//`)
- Unary operators (`+`, `-`)
- Parentheses for grouping
- Math constants (`pi`, `e`, etc.)
- Trigonometric functions (`sin`, `cos`, `atan2`, etc.)

This tutorial highlights Alpaca's ability to handle **operator precedence** and **conflict resolution**.

## 1. Defining the Lexer

The lexer for our calculator needs to handle various operators, keywords for functions, and numeric literals.

```scala
import alpaca.*

val CalcLexer = lexer:
  // Ignore whitespace and comments
  case "\\s+" => Token.Ignored
  case "#.*" => Token.Ignored

  // Multi-character operators FIRST to avoid partial matching
  case "\\*\\*" => Token["exp"]
  case "//" => Token["fdiv"]

  // Single-character operators
  case literal @ ("\\+" | "-" | "\\*" | "/" | "%" | "\\(" | "\\)" | ",") =>
    Token[literal.type]

  // Keywords (constants and functions)
  case keyword @ ("pi" | "e" | "sin" | "cos" | "tan" | "atan2") =>
    Token[keyword.type]

  // Numbers (floats and ints)
  case x @ """(\d+\.\d*|\.\d+)([eE][+-]?\d+)?""" => Token["float"](x.toDouble)
  case x @ "\\d+" => Token["int"](x.toInt)
```

## 2. Defining the Parser

The parser defines the grammar rules. Notice how we use **named productions** (e.g., `"plus"`, `"times"`) to make conflict resolution clearer.

```scala
object CalcParser extends Parser:
  val root: Rule[Double] = rule:
    case Expr(v) => v

  val Expr: Rule[Double] = rule(
    // Binary arithmetic
    "plus"  { case (Expr(a), CalcLexer.`\\+`(_), Expr(b)) => a + b },
    "minus" { case (Expr(a), CalcLexer.`-`(_), Expr(b)) => a - b },
    "times" { case (Expr(a), CalcLexer.`\\*`(_), Expr(b)) => a * b },
    "divide"{ case (Expr(a), CalcLexer.`//`(_), Expr(b)) => a / b },
    "exp"   { case (Expr(a), CalcLexer.`exp`(_), Expr(b)) => math.pow(a, b) },

    // Unary operators
    "uminus"{ case (CalcLexer.`-`(_), Expr(a)) => -a },

    // Constants and Functions
    "pi"    { case CalcLexer.pi(_) => math.Pi },
    "sin"   { case (CalcLexer.sin(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.sin(a) },
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

By default, the grammar above is ambiguous (e.g., does `1 + 2 * 3` mean `(1 + 2) * 3` or `1 + (2 * 3)`?). We resolve this by overriding the `resolutions` member.

```scala
  override val resolutions = Set(
    // Multiplication and division have higher precedence than addition
    production.times.after(CalcLexer.`\\+`, CalcLexer.`-`),
    production.divide.after(CalcLexer.`\\+`, CalcLexer.`-`),

    // Exponentiation has the highest precedence
    production.exp.after(CalcLexer.`\\*`, CalcLexer.`/`),

    // Left-associativity for addition and subtraction
    production.plus.before(CalcLexer.`\\+`, CalcLexer.`-`),
    production.minus.before(CalcLexer.`\\+`, CalcLexer.`-`)
  )
```

### Key Concepts in Conflict Resolution:
- `after`: Specifies that a production/token has **higher** precedence (is reduced later).
- `before`: Specifies that a production/token has **lower** precedence (is reduced earlier).
- `production.<name>`: References a named production.
- `CalcLexer.<TOKEN>`: References a terminal token.

## 4. Evaluating Expressions

```scala
val input = "sin(pi / 2) + 2 ** 3 * 4"
val (_, lexemes) = CalcLexer.tokenize(input)
val (_, result) = CalcParser.parse(lexemes)

println(result) // 33.0 (1.0 + 8.0 * 4.0)
```

## Summary

In this tutorial, we:
1. Built a lexer for a wide range of mathematical symbols and keywords.
2. Created a recursive parser for evaluating expressions.
3. Used **named productions** to simplify grammar definitions.
4. Applied **conflict resolution** rules to handle operator precedence and associativity, turning an ambiguous grammar into a deterministic one.
