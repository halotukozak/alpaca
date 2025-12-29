# Lexer Development Guide

This guide provides a comprehensive overview of the Alpaca lexer system, its architecture, API, and patterns for extending it.

## Table of Contents

1. [Overview](#overview)
2. [Public API Surface](#public-api-surface)
3. [Token DSL](#token-dsl)
4. [Lexer Context](#lexer-context)
5. [Runtime Tokenization](#runtime-tokenization)
6. [Context Manipulation](#context-manipulation)
7. [Adding and Modifying Tokens](#adding-and-modifying-tokens)
8. [Architecture Deep Dive](#architecture-deep-dive)
9. [Testing Lexers](#testing-lexers)
10. [Development Checklist](#development-checklist)

---

## Overview

The Alpaca lexer is a compile-time code generation system that transforms a small DSL (Domain Specific Language) into an efficient tokenizer. Rather than implementing traditional NFA/DFA automaton matching, Alpaca leverages Scala 3 macros to emit specialized matching logic at compile time.

### Key Features

- **Strongly typed tokens** with compile-time validation of names and payload types
- **Pattern matching syntax** for defining lexical rules in declaration order
- **Extensible context** supporting custom state tracking during lexing
- **Zero-runtime overhead** for unused features via macro-based code generation
- **Context-aware lexing** enabling stateful token recognition

### Design Philosophy

Alpaca separates concerns into distinct layers:

- **Public API** (`lexer.scala`) — User-facing DSL and entry points
- **Type-level contracts** (Token, Lexeme, LexerCtx) — Type safety and static validation
- **Macro implementation** (Lexer.scala) — Compile-time code generation
- **Runtime support** (Tokenization, LazyReader) — Efficient string scanning

This layering allows internal implementation changes without affecting the public DSL.

---

## Public API Surface

### The `lexer` Macro

The primary entry point is the `lexer` macro:

```scala
transparent inline def lexer[Ctx <: LexerCtx](
  using Ctx withDefault LexerCtx.Default,
)(
  inline rules: Ctx ?=> LexerDefinition[Ctx],
)(using
  copy: Copyable[Ctx],
  betweenStages: BetweenStages[Ctx],
  lexerRefinement: LexerRefinement[Ctx],
)(using inline debugSettings: DebugSettings
): Tokenization[Ctx] { type LexemeRefinement = lexerRefinement.Lexeme }
```

#### Parameters

| Parameter | Purpose |
|-----------|---------|
| `Ctx <: LexerCtx` | Context type; defaults to `LexerCtx.Default` |
| `rules` | Partial function matching regex patterns to tokens |
| `copy: Copyable[Ctx]` | Type class for cloning context (auto-derived for case classes) |
| `betweenStages: BetweenStages[Ctx]` | Hook for updating context between lexing stages |
| `lexerRefinement: LexerRefinement[Ctx]` | Type-level refinement for lexeme values |
| `debugSettings` | Compile-time debug output control |

#### Return Type

Returns a `Tokenization[Ctx]` instance that:
- Exposes token accessors (e.g., `Lexer.int`, `Lexer.id`)
- Provides the `tokenize(input: String): (Ctx, List[Lexeme[...]])`  method
- Maintains `tokens: List[TokenInfo]` for introspection

#### Basic Usage

```scala
import alpaca.*

val MyLexer = lexer {
  case "[0-9]+"           => Token["int"]
  case "[a-zA-Z_][\\w]*"  => Token["id"]
  case "\\s+"             => Token.Ignored
}

val (ctx, lexemes) = MyLexer.tokenize("x123 456")
```

---

## Token DSL

The `lexer` body is a partial function expressing token rules. Each `case` consists of:

1. A regex pattern (as a string literal)
2. An action producing a `Token[...]`

### Pattern Syntax

Patterns are standard Java regular expressions interpreted by `java.util.regex.Pattern`:

```scala
val Lexer = lexer {
  // Operators
  case "\\+" => Token["plus"]
  case "\\-" => Token["minus"]
  case "\\*" => Token["mul"]
  case "/" => Token["div"]
  
  // Literals
  case "[0-9]+" => Token["int"]
  case "[0-9]+\\.[0-9]+" => Token["float"]
  case "\"[^\"]*\"" => Token["string"]
  
  // Identifiers and keywords
  case "if" | "else" | "while" => Token["keyword"]
  case "[a-zA-Z_][\\w]*" => Token["id"]
  
  // Comments and whitespace
  case "#.*" => Token.Ignored
  case "\\s+" => Token.Ignored
}
```

**Important:** Regex patterns are matched in **declaration order**. More specific patterns must appear before general ones.

### Token Factory Methods

The `Token` object provides three constructors:

#### `Token.Ignored`

```scala
case "#.*" => Token.Ignored
```

Matches text but produces **no lexeme** in the output. Used for whitespace, comments, or other throwaway tokens.

#### `Token["name"]`

```scala
case "[0-9]+" => Token["int"]
```

Produces a token with name `"int"` carrying the matched text as a `String` payload. The type is:

```scala
Token["int", ctx.type, String]
```

#### `Token["name"](value)`

```scala
case num @ "[0-9]+" => Token["int"](num.toInt)
case str @ "\"[^\"]*\"" => Token["string"](str.drop(1).dropRight(1))
```

Produces a token with custom value. The value type is inferred from the argument:

```scala
Token["int", ctx.type, Int]
Token["string", ctx.type, String]
```

### Generated Token Accessors

For each distinct token name, the macro generates a public accessor on the resulting `Lexer` object. These serve dual purposes:

1. **Type-level documentation** — Shows the token's full type signature
2. **Parser integration** — Used as references in downstream parsing rules

Example:

```scala
val MyLexer = lexer {
  case "[0-9]+" => Token["int"](_.toInt)
  case "[a-zA-Z]+" => Token["id"]
}

// Generated accessors
MyLexer.int : Token["int", LexerCtx.Default, Int]
MyLexer.id  : Token["id", LexerCtx.Default, String]
```

These can be inspected at runtime via `MyLexer.tokens`.

---

## Lexer Context

### Purpose and Semantics

`LexerCtx` is the stateful component of the lexer. It tracks:

- **Current position** in the input string
- **Accumulated metadata** (line numbers, indentation, etc.)
- **Custom state** for language-specific behavior

Every lexing step mutates the context to reflect progress and semantic decisions.

### Base Trait

```scala
trait LexerCtx:
  this: Product =>

  var lastLexeme: Lexeme[?, ?] | Null = compiletime.uninitialized
  var lastRawMatched: String = compiletime.uninitialized
  var text: CharSequence
```

**Required fields:**

| Field | Type | Purpose |
|-------|------|---------|
| `text` | `CharSequence` | Remaining input to tokenize |
| `lastRawMatched` | `String` | Raw matched substring (set by `BetweenStages`) |
| `lastLexeme` | `Lexeme[?, ?] \| Null` | Last produced lexeme (set by `BetweenStages`) |

### Built-in Contexts

#### `LexerCtx.Default`

```scala
final case class Default(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx
    with PositionTracking
    with LineTracking
```

**Features:**
- Tracks 1-based character position and line number
- Suitable for error reporting in most languages
- Default when no `[Ctx]` parameter is provided to `lexer`

**Usage:**

```scala
val Lang = lexer {
  case "\\n" =>
    ctx.line += 1
    ctx.position = 1
    Token.Ignored
  case _ => ...
}
```

#### `LexerCtx.Empty`

```scala
final case class Empty(
  var text: CharSequence = "",
) extends LexerCtx
```

**Features:**
- Minimal context; only tracks remaining text
- No position or line tracking
- Useful for simple, position-insensitive scanning

### Custom Contexts

Create a custom context by extending `LexerCtx` and adding fields:

```scala
case class MyLangCtx(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
  var indentLevel: Int = 0,
  var parenDepth: Int = 0,
  var errors: List[String] = Nil,
) extends LexerCtx
    with PositionTracking
    with LineTracking

val MyLangLexer = lexer[MyLangCtx] {
  case "\\(" =>
    ctx.parenDepth += 1
    Token["lparen"]
  
  case "\\)" =>
    ctx.parenDepth -= 1
    if ctx.parenDepth < 0 then
      ctx.errors :+= s"Unexpected ) at line ${ctx.line}"
    Token["rparen"]
  
  case "\\n" =>
    ctx.line += 1
    ctx.position = 1
    Token.Ignored
  
  case _ => ...
}
```

### Context Copyability

The macro needs a `Copyable[Ctx]` instance to clone context during lexing. For case classes, this is auto-derived:

```scala
given [Ctx <: LexerCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
  Copyable.derived
```

If your context is not a case class, implement `Copyable` manually:

```scala
case class NonStandardCtx(...) extends LexerCtx

given Copyable[NonStandardCtx] = new Copyable[NonStandardCtx] {
  def copy(ctx: NonStandardCtx): NonStandardCtx =
    ctx.copy()  // implement as needed
}
```

### Between-Stages Behavior

After each regex match, the `BetweenStages[Ctx]` type class processes the match and updates the context. The default implementation:

```scala
given BetweenStages[LexerCtx] =
  case (DefinedToken(info, modifyCtx, remapping), m, ctx) =>
    ctx.lastRawMatched = m.matched.nn
    ctx.text = ctx.text.from(m.end)
    modifyCtx(ctx)
    
    val ctxAsProduct = ctx.asInstanceOf[Product]
    val fields = ctxAsProduct.productElementNames
      .zip(ctxAsProduct.productIterator).toMap +
      ("text" -> ctx.lastRawMatched)
    ctx.lastLexeme = Lexeme(info.name, remapping(ctx), fields)
  
  case (IgnoredToken(_, modifyCtx), m, ctx) =>
    ctx.lastRawMatched = m.matched.nn
    ctx.text = ctx.text.from(m.end)
    modifyCtx(ctx)
```

**Key behaviors:**
- Updates `text` to skip past the match
- Stores raw matched string in `lastRawMatched`
- For defined tokens, creates a `Lexeme` with the token value
- For ignored tokens, updates context but produces no output

You can override this by providing a custom `given BetweenStages[MyCtx]` instance.

---

## Runtime Tokenization

### The `Tokenization` Interface

After compilation, the `Lexer` object implements the `Tokenization[Ctx]` interface:

```scala
trait Tokenization[Ctx <: LexerCtx] {
  type LexemeRefinement
  
  def tokenize(input: String): (Ctx, List[Lexeme[?, ?]])
  def tokens: List[TokenInfo]
}
```

### Tokenizing Input

```scala
val input = "123 + 456"
val (finalCtx, lexemes) = MyLexer.tokenize(input)

// Inspect results
lexemes.foreach { lexeme =>
  println(s"${lexeme.name}: ${lexeme.value}")
}

// Inspect final context
println(s"Final line: ${finalCtx.line}")
println(s"Final position: ${finalCtx.position}")
```

### The `Lexeme` Data Structure

Each produced lexeme is a `Lexeme[Name, Value]`:

```scala
case class Lexeme[Name <: String, Value](
  name: Name,
  value: Value,
  contextSnapshot: Map[String, Any],
)
```

| Field | Purpose |
|-------|---------|
| `name` | Token name (e.g., `"int"`, `"id"`) |
| `value` | Token value (type depends on Token definition) |
| `contextSnapshot` | Snapshot of context fields at the moment the lexeme was created |

### Introspection

Access token definitions via the `tokens` field:

```scala
MyLexer.tokens.foreach { tokenInfo =>
  println(s"${tokenInfo.name}: ${tokenInfo.pattern}")
}
```

This is useful for meta-programming, documentation generation, or error reporting.

---

## Context Manipulation

### Mutation During Lexing

The DSL allows arbitrary mutation of the current context (`ctx`) within token actions. This enables state machines and semantic processing:

```scala
case class CounterCtx(
  var text: CharSequence = "",
  var count: Int = 0,
) extends LexerCtx

val CountingLexer = lexer[CounterCtx] {
  case "x" =>
    ctx.count += 1
    Token["x"](ctx.count)
  
  case "y" =>
    Token["y"](ctx.count)
  
  case " " => Token.Ignored
}

val (finalCtx, lexemes) = CountingLexer.tokenize("x y x x y")
// lexemes = [("x", 1), ("y", 1), ("x", 2), ("x", 3), ("y", 3)]
// finalCtx.count = 3
```

### Context Propagation

Context changes persist across token boundaries within a single `tokenize()` call. This allows:

- **State machines** (tracking nesting depth, indentation levels)
- **Semantic information** (remembering identifiers, tracking scope changes)
- **Error accumulation** (gathering multiple lexical errors without early termination)

### Coordinate with `BetweenStages`

The action function (inside `case`) runs **before** `BetweenStages` processes the match. This means:

1. Your `case` action mutates `ctx`
2. The `modifyCtx` hook is applied (usually a no-op unless customized)
3. The lexeme is created with the updated context
4. The context snapshot is stored in the lexeme

---

## Adding and Modifying Tokens

### Workflow: Adding a New Token

1. **Identify the pattern** — What regex describes the new token?

   ```scala
   case "true" | "false" => Token["bool"](_.toBoolean)
   ```

2. **Choose the value representation** — Raw string or parsed type?

   ```scala
   // Option A: Carry the matched text
   case keyword @ ("for" | "while") => Token[keyword.type]
   
   // Option B: Parse to a domain type
   case hex @ "0x[0-9a-fA-F]+" => Token["hex"](Integer.parseInt(hex.substring(2), 16))
   ```

3. **Position correctly** — More specific patterns before general ones

   ```scala
   val Lexer = lexer {
     // Keywords first (more specific)
     case "true" | "false" => Token["bool"](_.toBoolean)
     case "if" | "else" => Token["keyword"]
     
     // Then identifiers (more general)
     case "[a-zA-Z_][\\w]*" => Token["id"]
   }
   ```

4. **Update tests** — Add assertions for pattern order and type

   ```scala
   test("New token is recognized") {
     Lexer.tokens.map(_.pattern) should contain("true")
     Lexer.bool : Token["bool", LexerCtx.Default, Boolean]
     
     val (_, lexemes) = Lexer.tokenize("true false")
     lexemes.map(_.value) shouldBe List(true, false)
   }
   ```

### Workflow: Changing Token Precedence

If two patterns can match the same input, the first one wins. Reorder them to fix precedence issues:

```scala
val Lexer = lexer {
  // Problem: "123abc" matches "[0-9]+" first, leaving "abc"
  // Solution: Make sure identifiers don't start with digits
  case "[0-9]+" => Token["int"]
  case "[a-zA-Z_][\\w]*" => Token["id"]
  case "[0-9][a-zA-Z_]\\w]*" => Token["error"]  // garbage, if needed
}
```

### Workflow: Multi-Token Operators

For operators like `<=`, `>=`, `!=`, list them **before** their single-character equivalents:

```scala
val Lexer = lexer {
  case "<=" => Token["lessEqual"]
  case ">=" => Token["greaterEqual"]
  case "==" => Token["equal"]
  case "!=" => Token["notEqual"]
  
  case "<" => Token["less"]
  case ">" => Token["greater"]
  case "=" => Token["assign"]
  case "!" => Token["not"]
}
```

### Workflow: Removing a Token

1. Delete the `case` line
2. Update tests that reference the token
3. Rebuild to catch downstream parser references at compile time

---

## Architecture Deep Dive

### Macro Expansion

The `lexer` macro (in `internal/lexer/Lexer.scala`) performs several stages:

#### Stage 1: Rule Extraction

Analyzes the partial function to extract:
- Regex patterns
- Token names and types
- Custom value extractors

```scala
// Input
case num @ "[0-9]+" => Token["int"](num.toInt)

// Extracted
Pattern("[0-9]+") -> Token(name="int", type=Int, extractor=_.toInt)
```

#### Stage 2: Pattern Compilation

Validates each regex at compile time and creates `java.util.regex.Pattern` instances:

```scala
val pattern = try Pattern.compile("[0-9]+")
catch case e => reportCompileError(s"Invalid regex: ${e.getMessage}")
```

#### Stage 3: Tokenization Function Generation

Generates a specialized method that:
- Takes a context and input string
- Tries each pattern in order
- Calls the matching token action
- Invokes `BetweenStages` to update context
- Returns the final lexeme or continues

#### Stage 4: Accessor Generation

For each token name, generates a public field:

```scala
// For case num @ "[0-9]+" => Token["int"](num.toInt)
def int: Token["int", LexerCtx.Default, Int] = 
  DefinedToken(...)
```

### Key Internal Types

#### `DefinedToken` vs `IgnoredToken`

```scala
sealed trait TokenDef[Ctx <: LexerCtx]

case class DefinedToken[Ctx <: LexerCtx](
  info: TokenInfo,
  modifyCtx: Ctx => Unit,
  remapping: Ctx => Any,
) extends TokenDef[Ctx]

case class IgnoredToken[Ctx <: LexerCtx](
  pattern: String,
  modifyCtx: Ctx => Unit,
) extends TokenDef[Ctx]
```

**DefinedToken:** Produces a lexeme; carries a value remapping function.  
**IgnoredToken:** Does not produce a lexeme; used for whitespace/comments.

#### `TokenInfo`

```scala
case class TokenInfo(
  name: String,
  pattern: String,
  index: Int,  // Declaration order
)
```

Used for introspection and error messages.

#### `LazyReader`

A memory-efficient wrapper around `CharSequence`:

```scala
case class LazyReader(
  input: CharSequence,
  offset: Int,
)

def from(offset: Int): LazyReader = LazyReader(input, offset)
def remaining: String = input.subSequence(offset, input.length).toString
```

Allows efficient substring slicing without copying.

### Compilation Overhead

- Regex patterns are compiled once at macro expansion time
- All pattern matching is unrolled into if-else chains (no runtime decision trees)
- Type information is available to downstream tools (parser, AST builders)
- No reflection or runtime type introspection

---

## Testing Lexers

### Pattern List Assertions

Verify that token patterns are recognized and in the correct order:

```scala
test("Token patterns are in declaration order") {
  MyLexer.tokens.map(_.pattern) shouldBe List(
    "#.*",           // comments
    "<=", ">=",      // multi-char ops
    "<", ">",        // single-char ops
    "[0-9]+",        // int literals
    "[a-zA-Z_][\\w]*", // identifiers
    "\\s+",          // whitespace
  )
}
```

This catches issues when refactoring or adding tokens.

### Type Assertions

Check that generated accessors have correct types:

```scala
test("Token accessors have correct types") {
  MyLexer.int    : Token["int", LexerCtx.Default, Int]
  MyLexer.float  : Token["float", LexerCtx.Default, Double]
  MyLexer.id     : Token["id", LexerCtx.Default, String]
  MyLexer.if     : Token["if", LexerCtx.Default, Unit]
}
```

These compile-time checks prevent silent regressions in token definitions.

### Tokenization Behavior

Test actual output on representative inputs:

```scala
test("Lexer tokenizes expressions") {
  val (_, lexemes) = MyLexer.tokenize("123 + 456")
  
  lexemes should have size 3
  lexemes(0).name shouldBe "int"
  lexemes(0).value shouldBe 123
  
  lexemes(1).name shouldBe "plus"
  lexemes(2).name shouldBe "int"
  lexemes(2).value shouldBe 456
}
```

### Context Tracking

For stateful lexers, verify context evolution:

```scala
test("Lexer tracks nesting depth") {
  case class NestCtx(
    var text: CharSequence = "",
    var depth: Int = 0,
  ) extends LexerCtx
  
  val Lexer = lexer[NestCtx] {
    case "\\(" => ctx.depth += 1; Token["lparen"]
    case "\\)" => ctx.depth -= 1; Token["rparen"]
    case "\\s+" => Token.Ignored
  }
  
  val (finalCtx, lexemes) = Lexer.tokenize("( ( ) )")
  finalCtx.depth shouldBe 0
  lexemes.size shouldBe 4
}
```

### Error Handling

Test malformed input:

```scala
test("Lexer fails on invalid input") {
  assertThrows[TokenizationException] {
    MyLexer.tokenize("###invalid###")
  }
}
```

---

## Development Checklist

When extending or modifying the lexer, follow this checklist:

### Planning Phase

- [ ] Identify all token kinds for your language
- [ ] Classify tokens as "defined" (produce lexemes) or "ignored" (whitespace, comments)
- [ ] Determine what value each defined token should carry
- [ ] Plan the declaration order (specific before general)

### Implementation Phase

- [ ] Define regex patterns for each token
- [ ] Choose context type (`Default`, `Empty`, or custom)
- [ ] Add any custom context fields needed for semantic tracking
- [ ] Implement `case` arms in the lexer DSL
- [ ] Test token recognition with simple inputs

### Testing Phase

- [ ] Add pattern list assertions (`Lexer.tokens.map(_.pattern)`)
- [ ] Add type assertions for accessors
- [ ] Test tokenization on representative inputs
- [ ] Test context evolution if stateful
- [ ] Test error cases (malformed input, ambiguities)

### Integration Phase

- [ ] Document token names and semantics
- [ ] Provide example usage in comments
- [ ] Update downstream parser/AST code if token names or types changed
- [ ] Update CHANGELOG or release notes if this is a visible change

### Maintenance Phase

- [ ] Keep test coverage above 80%
- [ ] Review pattern order when adding similar tokens
- [ ] Consider performance if tokenizing very large files
- [ ] Document any context mutations and their semantics

---

## Next Steps

- Proceed to [Parser Development Guide](./parser-development.md) to learn how to consume lexer output
- See [API Reference](./api-reference.md) for detailed type signatures
- Explore [Examples](../example/) for real-world usage patterns
