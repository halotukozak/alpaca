# Getting Started

This guide will help you get started with Alpaca, a modern, type-safe lexer and parser library for Scala 3.

## What is Alpaca?

Alpaca is a lexer and parser library featuring compile-time validation and elegant DSL syntax. It leverages Scala 3's powerful type system and macros to provide:

- 🔍 **Type-safe lexer and parser** - Catch errors at compile time
- 🎯 **Elegant DSL** - Define lexers and parsers using intuitive pattern matching syntax
- ⚡ **Compile-time validation** - Regex patterns and grammar rules are validated during compilation
- 🧪 **Macro-based** - Leverages Scala 3 macros for efficient code generation
- 📚 **Context-aware** - Support for lexical and parsing contexts with type-safe state management
- 🛠️ **LR Parsing** - Uses LR parsing algorithm with automatic parse table generation

## Installation

### Mill

Add Alpaca as a dependency in your `build.mill`:

```scala
//| mill-version: 1.0.6
//| mill-jvm-version: 21

import mill._
import mill.scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "3.7.3"
  
  def ivyDeps = Seq(
    ivy"com.github.halotukozak::alpaca:0.1.0"
  )
}
```

### SBT

Add Alpaca to your `build.sbt`:

```scala
libraryDependencies += "com.github.halotukozak" %% "alpaca" % "0.1.0"
```

Make sure you're using Scala 3.7.3 or later:

```scala
scalaVersion := "3.7.3"
```

### Scala CLI

Use Alpaca directly in your Scala CLI scripts:

```scala
//> using scala "3.7.3"
//> using dep "com.github.halotukozak::alpaca:0.1.0"

import alpaca.lexer.{lexer, Token}
import alpaca.parser.{Parser, Rule, rule}

// Your code here
```

## Quick Start

### Creating a Lexer

Define a lexer using pattern matching with regex patterns:

```scala
import alpaca.lexer.{lexer, Token}

val CalcLexer = lexer {
  case num @ "[0-9]+" => Token["NUM"](num.toDouble)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["STAR"]
  case "/" => Token["SLASH"]
  case "\\(" => Token["LP"]
  case "\\)" => Token["RP"]
  case "[ \\t\\r\\n]+" => Token.Ignored
}
```

### Creating a Parser

Define a parser by extending the `Parser` class and defining grammar rules:

```scala
import alpaca.parser.{Parser, Rule, rule}
import alpaca.parser.context.default.EmptyGlobalCtx

object CalcParser extends Parser[EmptyGlobalCtx] {
  val root: Rule[Double] = rule { case Expr(e) => e }

  val Expr: Rule[Double] = rule(
    { case Expr(l) ~ Token["PLUS"] ~ Term(r) => l + r },
    { case Expr(l) ~ Token["MINUS"] ~ Term(r) => l - r },
    { case Term(t) => t }
  )

  val Term: Rule[Double] = rule(
    { case Term(l) ~ Token["STAR"] ~ Factor(r) => l * r },
    { case Term(l) ~ Token["SLASH"] ~ Factor(r) => l / r },
    { case Factor(f) => f }
  )

  val Factor: Rule[Double] = rule(
    { case Token["NUM"](n) => n },
    { case Token["LP"] ~ Expr(e) ~ Token["RP"] => e }
  )
}
```

### Parsing Input

```scala
val input = "2 + 3 * 4"
val tokens = CalcLexer.tokenize(input)
val result = CalcParser.parse(tokens)
println(result) // 14.0
```

## Complete Example

Here's a complete example of a simple calculator:

```scala
import alpaca.lexer.{lexer, Token}
import alpaca.parser.{Parser, Rule, rule}
import alpaca.parser.context.default.EmptyGlobalCtx

@main def calculator(): Unit = {
  // Define lexer
  val CalcLexer = lexer {
    case num @ "[0-9]+" => Token["NUM"](num.toDouble)
    case "\\+" => Token["PLUS"]
    case "-" => Token["MINUS"]
    case "\\*" => Token["STAR"]
    case "/" => Token["SLASH"]
    case "\\(" => Token["LP"]
    case "\\)" => Token["RP"]
    case "[ \\t\\r\\n]+" => Token.Ignored
  }

  // Define parser
  object CalcParser extends Parser[EmptyGlobalCtx] {
    val root: Rule[Double] = rule { case Expr(e) => e }

    val Expr: Rule[Double] = rule(
      { case Expr(l) ~ Token["PLUS"] ~ Term(r) => l + r },
      { case Expr(l) ~ Token["MINUS"] ~ Term(r) => l - r },
      { case Term(t) => t }
    )

    val Term: Rule[Double] = rule(
      { case Term(l) ~ Token["STAR"] ~ Factor(r) => l * r },
      { case Term(l) ~ Token["SLASH"] ~ Factor(r) => l / r },
      { case Factor(f) => f }
    )

    val Factor: Rule[Double] = rule(
      { case Token["NUM"](n) => n },
      { case Token["LP"] ~ Expr(e) ~ Token["RP"] => e }
    )
  }

  // Use the parser
  val input = "2 + 3 * (4 + 5)"
  val tokens = CalcLexer.tokenize(input)
  val result = CalcParser.parse(tokens)
  println(s"$input = $result")  // 2 + 3 * (4 + 5) = 29.0
}
```

## Advanced Features

### Contextual Lexing and Parsing

Alpaca supports context-aware lexing and parsing, allowing you to maintain state during tokenization and parsing:

```scala
// Custom context with state
case class MyContext(depth: Int = 0) derives Empty, Copyable

val contextAwareLexer = lexer[MyContext] {
  case "{" => Token["LBRACE"]
  case "}" => Token["RBRACE"]
  // ... more rules
}
```

### Token Extractors

Tokens can carry values extracted from the input:

```scala
case num @ "[0-9]+" => Token["NUM"](num.toInt)
case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"](id)
```

### Ignored Tokens

Use `Token.Ignored` for whitespace and comments that should be skipped:

```scala
case "[ \\t\\r\\n]+" => Token.Ignored
case "#.*" => Token.Ignored  // Comments
```

## Requirements

- Scala 3.7.3 or later
- JDK 21 or later (for development)

## Next Steps

- Check out the [full documentation](https://halotukozak.github.io/alpaca/)
- Explore the [example projects](../../test)
- Read about the [project structure](../../README.md#project-structure)
