# Alpaca

A powerful parser and lexer library for Scala 3 with macro-based DSL.

## Overview

Alpaca is a Scala 3 library that provides an intuitive DSL for building lexers and parsers. It leverages Scala 3's advanced macro system to offer compile-time safety and excellent error messages.

## Features

- **Macro-based DSL** - Type-safe lexer and parser definitions using Scala 3 macros
- **Regex-based Lexer** - Define tokens using regular expressions
- **LR Parser** - Generate LR parsing tables at compile time
- **Debug Utilities** - Comprehensive debugging tools for macro development
- **Type Safety** - Compile-time verification of grammar rules

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.1.0"
```

For Mill:

```scala
def ivyDeps = Agg(
  ivy"io.github.halotukozak::alpaca:0.1.0"
)
```

## Quick Start

Here's a simple calculator example:

```scala
import alpaca.lexer.{lexer, Token}
import alpaca.parser.{Parser, rule}

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
    case Term(t) ~ PLUS ~ Expr(e) => t + e,
    case Term(t) ~ MINUS ~ Expr(e) => t - e,
    case Term(t) => t
  )

  val Term: Rule[Double] = rule(
    case Factor(f) ~ STAR ~ Term(t) => f * t,
    case Factor(f) ~ SLASH ~ Term(t) => f / t,
    case Factor(f) => f
  )

  val Factor: Rule[Double] = rule(
    case LP ~ Expr(e) ~ RP => e,
    case NUM(n) => n
  )
}
```

## Requirements

- Scala 3.7.4 or later
- JVM 21 or later

## Documentation

Full documentation is available at [https://halotukozak.github.io/alpaca](https://halotukozak.github.io/alpaca)

## Building

This project uses Mill as its build tool:

```bash
# Compile
./mill compile

# Run tests
./mill test

# Generate documentation
./mill docJar
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

Created by [Kamil Tułowiecki](https://github.com/halotukozak)

## Acknowledgments

Made with ❤️ and coffee
