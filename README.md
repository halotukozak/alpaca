# Alpaca 🦙 — A Lexer Parser And Compiler

A modern, type-safe lexer and parser library for Scala 3, featuring compile-time validation and elegant DSL syntax.

## Features

- 🔍 **Type-safe lexer and parser** — Catch errors at compile time with Scala 3's powerful type system
- 🎯 **Elegant DSL** — Define lexers and parsers using intuitive pattern matching syntax
- ⚡ **Compile-time validation** — Regex patterns and grammar rules are validated during compilation
- 🧪 **Macro-based** — Leverages Scala 3 macros for efficient code generation
- 📚 **Context-aware** — Support for lexical and parsing contexts with type-safe state management
- 🛠️ **LR Parsing** — Uses LR parsing algorithm with automatic parse table generation

## Quick Navigation

### For Getting Started
- 📖 [Getting Started Guide](./docs/_docs/getting-started.md) — Installation and basic usage
- 🚀 [Lexer Quickstart](./docs/_docs/lexer-quickstart.md) — Copy-paste examples for common patterns

### For Lexer Development
- 📚 [Lexer Development Guide](./docs/_docs/lexer-development.md) — Comprehensive reference covering API, context, and testing
- 🔧 [Lexer Internals](./docs/_docs/lexer-internals.md) — Macro implementation, type system, and advanced customization
- 📋 [Lexer API Reference](./docs/_docs/lexer-api-reference.md) — Complete type signatures and API documentation

### For Parser Development
- 📘 Parser Development Guide (Coming Soon)

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
    mvn"io.github.halotukozak::alpaca:0.0.1"
  )
}
```

### SBT

Add Alpaca to your `build.sbt`:

```sbt
libraryDependencies += "io.github.halotukozak" %% "alpaca" % "0.0.1"
scalaVersion := "3.7.4"
```

### Scala CLI

Use Alpaca directly in your Scala CLI scripts:

```scala
//> using scala "3.7.4"
//> using dep "io.github.halotukozak::alpaca:0.0.1"

import alpaca.*

// Your code here
```

## 30-Second Example

### Defining a Lexer

```scala
import alpaca.*

val Lexer = lexer {
  case num @ "[0-9]+" => Token["int"](num.toInt)
  case "\\+" => Token["plus"]
  case "-" => Token["minus"]
  case "\\s+" => Token.Ignored
}
```

### Using the Lexer

```scala
val (ctx, lexemes) = Lexer.tokenize("1 + 2 - 3")

lexemes.foreach { lexeme =>
  println(s"${lexeme.name}: ${lexeme.value}")
}

// Output:
// int: 1
// plus: ()
// int: 2
// minus: ()
// int: 3
```

See the [Lexer Quickstart](./docs/_docs/lexer-quickstart.md) for more examples.

## Project Structure

```text
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
│   ├── LexerApiTest.scala    # Lexer tests
│   ├── ParserApiTest.scala   # Parser tests
│   └── integration/          # Integration tests
├── example/                  # Example projects
├── docs/                     # Documentation
│   ├── _docs/
│   │   ├── getting-started.md
│   │   ├── lexer-development.md          # Comprehensive lexer guide
│   │   ├── lexer-quickstart.md           # Practical examples
│   │   ├── lexer-internals.md            # Macro and implementation details
│   │   ├── lexer-api-reference.md        # Complete API reference
│   │   └── ...
│   └── _assets/              # Documentation assets
├── build.mill                # Mill build configuration
└── README.md                 # This file
```

## Documentation

### Lexer Documentation

The Alpaca lexer system is thoroughly documented:

| Document | Audience | Level |
|----------|----------|-------|
| [Lexer Quickstart](./docs/_docs/lexer-quickstart.md) | All users | Beginner |
| [Lexer Development Guide](./docs/_docs/lexer-development.md) | DSL users, language designers | Intermediate |
| [Lexer Internals](./docs/_docs/lexer-internals.md) | Contributors, advanced users | Advanced |
| [Lexer API Reference](./docs/_docs/lexer-api-reference.md) | API consumers | Reference |

### Typical Learning Path

1. **Start with** [Getting Started](./docs/_docs/getting-started.md) for installation
2. **Explore** [Lexer Quickstart](./docs/_docs/lexer-quickstart.md) for hands-on examples
3. **Deep dive** [Lexer Development Guide](./docs/_docs/lexer-development.md) for comprehensive knowledge
4. **Reference** [Lexer API Reference](./docs/_docs/lexer-api-reference.md) while coding
5. **Understand internals** [Lexer Internals](./docs/_docs/lexer-internals.md) for extending or contributing

## Key Concepts

### Tokens

Tokens are the building blocks of a lexer. Define them using regex patterns and extract values:

```scala
val Lexer = lexer {
  case num @ "[0-9]+" => Token["int"](num.toInt)      // Extract Int
  case id @ "[a-z]+" => Token["id"](id)                // Extract String
  case "#.*" => Token.Ignored                           // Skip comments
}
```

### Context

Lexical context tracks state during tokenization:

```scala
case class MyCtx(
  var text: CharSequence = "",
  var line: Int = 1,
  var parenDepth: Int = 0,
) extends LexerCtx

val Lexer = lexer[MyCtx] {
  case "(" => ctx.parenDepth += 1; Token["lparen"]
  case ")" => ctx.parenDepth -= 1; Token["rparen"]
  case "\n" => ctx.line += 1; Token.Ignored
}
```

### Pattern Ordering

Patterns match in declaration order. More specific patterns must come first:

```scala
val Lexer = lexer {
  case "[0-9]+\\.[0-9]+" => Token["float"]   // Specific: floats
  case "[0-9]+" => Token["int"]               // General: integers
  
  case "if" | "else" => Token["keyword"]     // Keywords before
  case "[a-z]+" => Token["id"]                // Identifiers
}
```

## Examples

See the `example/` directory for complete working projects, including:

- **Math Expression Parser**: Simple arithmetic with precedence
- **Configuration Language**: TOML-like format with nesting
- **Mini Language**: Function definitions, loops, and variables

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

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

For more details, see the [Contributing Guidelines](./CONTRIBUTING.md) (if present).

## Authors

Created by [halotukozak](https://github.com/halotukozak) and [Corvette653](https://github.com/Corvette653)

## License

MIT License — See [LICENSE](./LICENSE) for details

---

Made with ❤️ and coffee →
