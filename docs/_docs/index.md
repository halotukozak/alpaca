# Alpaca

A modern, type-safe lexer and parser library for Scala 3, featuring compile-time validation and elegant DSL syntax.

## Features

- **Type-safe lexer and parser** — catch errors at compile time with Scala 3's powerful type system
- **Elegant DSL** — define lexers and parsers using intuitive pattern matching syntax
- **Compile-time validation** — regex patterns and grammar rules are validated during compilation
- **Macro-based** — leverages Scala 3 macros for efficient code generation
- **Context-aware** — support for lexical and parsing contexts with type-safe state management
- **LR Parsing** — uses LR parsing algorithm with automatic parse table generation

## Quick Example

```scala sc:nocompile
import alpaca.*

// Define a lexer
val MyLexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toDouble)
  case "\\+"          => Token["PLUS"]
  case "-"            => Token["MINUS"]
  case "\\*"          => Token["STAR"]
  case "/"            => Token["SLASH"]
  case "\\("          => Token["LP"]
  case "\\)"          => Token["RP"]
  case "\\s+"         => Token.Ignored

// Parse input
val (_, lexemes) = MyLexer.tokenize("2 + 3 * 4")
```

## Installation

### Mill

```mill
def mvnDeps = Seq(
  mvn"io.github.halotukozak::alpaca:0.0.2"
)
```

### SBT

```sbt
libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.0.2"
```

### Scala CLI

```scala sc:nocompile
//> using dep "io.github.halotukozak::alpaca:0.0.2"
```

Head to [Getting Started](getting-started.html) for a full walkthrough.
