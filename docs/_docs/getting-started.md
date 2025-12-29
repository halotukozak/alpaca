# Alpaca 🦙

A modern, type-safe lexer and parser library for Scala 3, featuring compile-time validation and elegant DSL syntax.

## Features

- 🔍 **Type-safe lexer and parser** - Catch errors at compile time with Scala 3's powerful type system
- 🎯 **Elegant DSL** - Define lexers and parsers using intuitive pattern matching syntax
- ⚡ **Compile-time validation** - Regex patterns and grammar rules are validated during compilation
- 🧪 **Macro-based** - Leverages Scala 3 macros for efficient code generation
- 📚 **Context-aware** - Support for lexical and parsing contexts with type-safe state management
- 🛠️ **LR Parsing** - Uses LR parsing algorithm with automatic parse table generation

## Installation

### Mill

Add Alpaca as a dependency in your `build.mill`:

```mill
//| mill-version: 1.0.6
//| mill-jvm-version: 21

import mill._
import mill.scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "3.7.4"
  
  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.0.2"
  )
}
```

### SBT

Add Alpaca to your `build.sbt`:

```sbt
libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.0.2"
```

Make sure you're using Scala 3.7.4 or later:

```sbt
scalaVersion := "3.7.4"
```

### Scala CLI

Use Alpaca directly in your Scala CLI scripts:

```scala
//> using scala "3.7.4"
//> using dep "io.github.halotukozak::alpaca:0.0.2"

import alpaca.*

// Your code here
```

## Basic Concepts

### Token

A **token** is the basic unit produced by the lexer. It represents a meaningful sequence of characters.

```scala
Token[Type](value)
```

### Lexeme

A **lexeme** is the actual matched text. Capture it with `@`:

```scala
case num @ "[0-9]+" => Token["NUM"](num.toDouble)
```

### Grammar Rule

A **rule** defines how to parse sequences of tokens:

```scala
val Expr: Rule[Int] = rule(
  { case (Expr(l), PLUS(_), Term(r)) => l + r },
  { case Term(t) => t }
)
```

## Quick Start: Calculator Example

Here is a complete example of a simple calculator using Alpaca.

```scala
import alpaca.*

// Lexer
val CalcLexer = lexer:
  case num@"[0-9]+" => Token["NUM"](num.toDouble)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["STAR"]
  case "/" => Token["SLASH"]
  case "\\s+" => Token.Ignored

// Parser
object CalcParser extends Parser:
  val root: Rule[Double] = rule { case Expr(e) => e }

  val Expr: Rule[Double] = rule(
    { case (Expr(l), CalcLexer.PLUS(_), Term(r)) => l + r },
    { case (Expr(l), CalcLexer.MINUS(_), Term(r)) => l - r },
    { case Term(t) => t }
  )

  val Term: Rule[Double] = rule(
    { case (Term(l), CalcLexer.STAR(_), Factor(r)) => l * r },
    { case (Term(l), CalcLexer.SLASH(_), Factor(r)) => l / r },
    { case Factor(f) => f }
  )

  val Factor: Rule[Double] = rule(
    { case (CalcLexer.NUM(n)) => n.value },
    { case (CalcLexer.LPAREN(_), Expr(e), CalcLexer.RPAREN(_)) => e }
  )

// Usage
@main def main(): Unit =
  val input = "2 + 3 * 4"
  val (_, lexemes) = CalcLexer.tokenize(input)
  val (_, result) = CalcParser.parse(lexemes)
  println(s"$input = $result") // Output: 2 + 3 * 4 = 14.0
```

## Next Steps

- Explore the [Lexer Guide](lexer.md) for more details on tokenization.
- Explore the [Parser Guide](parser.md) for more details on grammar rules and precedence.

## Project Structure

```text sc:nocompile
alpaca/
├── src/alpaca/
│   ├── internal/              # Internal implementation
│   │   ├── lexer/            # Lexer internals (Token, Lexem, Tokenization, etc.)
│   │   ├── parser/           # Parser internals (ParseTable, State, Item, etc.)
│   │   ├── Empty.scala       # Empty type class utilities
│   │   ├── Copyable.scala    # Copyable type class
│   │   ├── Showable.scala    # Showable type class for debugging
│   │   └── ...               # Other core utilities
│   ├── lexer.scala           # Public lexer DSL and API
│   ├── parser.scala          # Public parser DSL and API
│   └── local.scala           # Local utilities
├── test/src/alpaca/          # Test suite
├── example/                  # Example projects
├── docs/                     # Documentation
└── build.mill                # Mill build configuration
```
## Building from Source

### Prerequisites

- JDK 21 or later
- Mill 1.0.6 or later

### Build Commands

```bash
# Compile the project
./mill compile

# Run tests
./mill test

# Generate documentation
./mill docJar

# Run test coverage
./mill test.scoverage.htmlReport
```

## Documentation

- 📖 [Lexer](lexer.md)
- 📖 [Parser](parser.md)
- 📖 [Full Documentation](https://halotukozak.github.io/alpaca/)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Authors

Created by [halotukozak](https://github.com/halotukozak) and [Corvette653](https://github.com/Corvette653)

---

Made with ❤️ and coffee
