# Alpaca

A type-safe lexer and parser library for Scala 3, featuring compile-time validation and a pattern-matching DSL.

## Features

- **Type-safe lexer and parser** — catch errors at compile time with Scala 3's type system
- **Pattern-matching DSL** — define lexers and parsers using intuitive `case` syntax
- **Compile-time validation** — regex patterns and grammar rules are checked during compilation
- **Macro-based code generation** — Scala 3 macros generate efficient tokenizers and parse tables
- **Context-aware** — lexical and parsing contexts with type-safe state management
- **LR(1) parsing** — automatic parse table generation with conflict detection

## Installation

### Mill

Add Alpaca as a dependency in your `build.mill`:

```mill
//| mill-version: 1.1.3
//| mill-jvm-version: 21

import mill._
import mill.scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "3.8.3-RC1"

  def scalacOptions = Seq("-Yretain-trees")

  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.1.0"
  )
}
```

### SBT

Add Alpaca to your `build.sbt`:

```sbt
libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.1.0"
```

Make sure you're using Scala 3.8.3-RC1 or later and enable the required compiler flag:

```sbt
scalaVersion := "3.8.3-RC1"
scalacOptions += "-Yretain-trees"
```

### Scala CLI

Use Alpaca directly in your Scala CLI scripts:

```scala sc:nocompile
//> using scala "3.8.3-RC1"
//> using dep "io.github.halotukozak::alpaca:0.1.0"
//> using option "-Yretain-trees"

import alpaca.*

// Your code here
```

## Quick Start

### Creating a Lexer

Define a lexer using pattern matching with regex patterns:

```scala sc:nocompile
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

```scala sc:nocompile
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

```scala sc:nocompile
import alpaca.*

val input = "2 + 3 * 4"
val (_, lexemes) = MyLexer.tokenize(input)
val (_, result) = MyParser.parse(lexemes)
println(result) // 14.0
```

## What's Next

- **[Getting Started](getting-started.md)** — build a BrainFuck interpreter step by step
- **[Lexer](lexer.md)** — the full lexer DSL reference
- **[Parser](parser.md)** — grammar rules, EBNF operators, conflict resolution
- **[Theory](theory/pipeline.md)** — formal foundations: finite automata, LR parsing, parse tables

## Benchmarks

Runtime benchmarks are **not** run automatically in CI on push or pull requests. They can be triggered manually:

- **GitHub Actions** — go to *Actions > Runtime Benchmark > Run workflow* and select the branch.
- **Locally** — run all benchmarks (JMH + Python) from the repository root:

  ```bash
  ./mill benchmarks.runAll
  ```

  Or run individual JMH suites directly:

  ```bash
  ./mill benchmarks.alpaca.runJmh
  ./mill benchmarks.fastparse.runJmh
  ```

  Results are written to `benchmarks/outputs/`.

## Building from Source

### Prerequisites

- JDK 21 or later
- Mill 1.1.3 or later

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

## Thesis

This project was developed as a Bachelor's Thesis. The full text is available in
the [thesis.pdf](https://github.com/halotukozak/alpaca/blob/master/thesis.pdf) file. The LaTeX source files are on
the `thesis` [branch](https://github.com/halotukozak/alpaca/tree/thesis). The thesis is written in Polish
and does not represent the current state of the project.

## Contributing

Contributions are welcome. Please feel free to submit a Pull Request.

## Authors

Created by [halotukozak](https://github.com/halotukozak) and [Corvette653](https://github.com/Corvette653)

---

Made with ❤️ and coffee
