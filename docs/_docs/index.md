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

```scala sc:nocompile
//> using scala "3.7.4"
//> using dep "io.github.halotukozak::alpaca:0.0.2"

import alpaca.*

// Your code here
```

## Quick Start

### Creating a Lexer

Define a lexer using pattern matching with regex patterns:

```scala sc:nocompile sc-name:MyLexer.scala
import alpaca.*

val MyLexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toDouble)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["STAR"]
  case "/" => Token["SLASH"]
  case "\\(" => Token["LP"]
  case "\\)" => Token["RP"]
  case "\\s+" => Token.Ignored
```

### Creating a Parser

Define a parser by extending the `Parser` class and defining grammar rules:

```scala sc:nocompile sc-name:MyParser.scala sc-compile-with:MyLexer.scala
import alpaca.*

object MyParser extends Parser:
  val root: Rule[Double] = rule { case Expr(e) => e }

  val Expr: Rule[Double] = rule(
    { case (Expr(l), MyLexer.PLUS(_), Term(r)) => l + r },
    { case (Expr(l), MyLexer.MINUS(_), Term(r)) => l - r },
    { case Term(t) => t }
  )

  val Term: Rule[Double] = rule(
    { case (Term(l), MyLexer.STAR(_), Factor(r)) => l * r },
    { case (Term(l), MyLexer.SLASH(_), Factor(r)) => l / r },
    { case Factor(f) => f }
  )

  val Factor: Rule[Double] = rule(
    { case MyLexer.NUM(n) => n.value },
    { case (MyLexer.LP(_), Expr(e), MyLexer.RP(_)) => e }
  )
```

### Parsing Input

```scala sc:nocompile sc-compile-with:MyLexer.scala,MyParser.scala
val input = "2 + 3 * 4"
val (_, lexemes) = MyLexer.tokenize(input)
val (_, result) = MyParser.parse(lexemes)
println(result) // 14.0
```

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

## Advanced Features

### Contextual Lexing and Parsing

Alpaca supports context-aware lexing and parsing, allowing you to maintain state during tokenization and parsing. Here's an example that tracks brace matching:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable.Stack

case class BraceContext(
  var text: CharSequence = "",
  val braces: Stack[Char] = Stack()
) extends LexerCtx

val braceLexer = lexer[BraceContext]:
  case "\\(" => 
    ctx.braces.push('(')
    Token["LPAREN"]
  case "\\)" => 
    if ctx.braces.isEmpty || ctx.braces.pop() != '(' then
      throw RuntimeException("Mismatched parenthesis")
    Token["RPAREN"]
  case "\\{" => 
    ctx.braces.push('{')
    Token["LBRACE"]
  case "\\}" => 
    if ctx.braces.isEmpty || ctx.braces.pop() != '{' then
      throw RuntimeException("Mismatched brace")
    Token["RBRACE"]
  case "\\s+" => Token.Ignored
  case "[a-zA-Z]+" => Token["ID"]

// Usage
val input = "{ foo ( bar ) }"
val (finalCtx, lexemes) = braceLexer.tokenize(input)
if finalCtx.braces.nonEmpty then
  throw RuntimeException("Unclosed braces: " + finalCtx.braces.mkString)
```

### Token Extractors

Tokens can carry values extracted from the input:

```scala sc:nocompile
case num @ "[0-9]+" => Token["NUM"](num.toInt)
case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"](id)
```

### Ignored Tokens

Use `Token.Ignored` for whitespace and comments that should be skipped:

```scala sc:nocompile
case "\\s+" => Token.Ignored
case "#.*" => Token.Ignored 
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

- 📖 [Full Documentation](https://halotukozak.github.io/alpaca/)
- 🚀 [Getting Started](https://halotukozak.github.io/alpaca/docs/getting-started.html) - Installation, quick start, and first steps
- 🔤 [Lexer](https://halotukozak.github.io/alpaca/docs/lexer.html) - Token definitions, regex patterns, and tokenization
- 🗂️ [Lexer Context](https://halotukozak.github.io/alpaca/docs/lexer-context.html) - Stateful lexing with LexerCtx and tracking traits
- ⚠️ [Lexer Error Recovery](https://halotukozak.github.io/alpaca/docs/lexer-error-recovery.html) - ShadowException, pattern ordering, and error handling
- 🔀 [Between Stages](https://halotukozak.github.io/alpaca/docs/between-stages.html) - Lexeme structure and the lexer-to-parser data pipeline
- 📐 [Parser](https://halotukozak.github.io/alpaca/docs/parser.html) - Grammar rules, EBNF operators, and parsing tokenized input
- 🧩 [Parser Context](https://halotukozak.github.io/alpaca/docs/parser-context.html) - Custom ParserCtx, shared state across reductions, and the parse() return value
- ⚖️ [Conflict Resolution](https://halotukozak.github.io/alpaca/docs/conflict-resolution.html) - Shift/reduce and reduce/reduce conflicts, the before/after DSL, and named productions
- 🎯 [Extractors](https://halotukozak.github.io/alpaca/docs/extractors.html) - Terminal and non-terminal matching, EBNF extractors, and Lexeme field access
- 🐛 [Debug Settings](https://halotukozak.github.io/alpaca/docs/debug-settings.html) - Configure compile-time debugging and logging

## Thesis

This project was developed as a Bachelor's Thesis. The full text of the thesis is available in
the [thesis.pdf](https://github.com/halotukozak/alpaca/blob/master/thesis.pdf) file. The LaTeX source files can be found
on the `thesis` [branch](https://github.com/halotukozak/alpaca/tree/thesis). Note that the thesis is written in Polish
and does not represent the current state of the project.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Authors

Created by [halotukozak](https://github.com/halotukozak) and [Corvette653](https://github.com/Corvette653)

---

Made with ❤️ and coffee
