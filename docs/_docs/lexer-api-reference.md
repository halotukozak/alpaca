# Lexer API Reference

Complete type signatures and API documentation for the Alpaca lexer system.

## Core API

### `lexer` Macro

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

**Parameters:**

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `Ctx` | Type Parameter | `LexerCtx.Default` | Context type for stateful lexing |
| `rules` | Function | Required | Partial function mapping patterns to tokens |
| `copy` | Given | Auto-derived | Type class for context cloning |
| `betweenStages` | Given | Built-in | Hook for context updates between matches |
| `lexerRefinement` | Given | Built-in | Type-level refinement for lexemes |
| `debugSettings` | Given | Provided | Compile-time debug output control |

**Returns:** `Tokenization[Ctx]` with fields:

```scala
trait Tokenization[Ctx <: LexerCtx] {
  def tokenize(input: String): (Ctx, List[Lexeme[?, ?]])
  def tokenize(input: CharSequence): (Ctx, List[Lexeme[?, ?]])
  def tokens: List[TokenInfo]
}
```

---

## Token DSL

### `Token` Factory

#### `Token.Ignored`

```scala
@compileTimeOnly("Should never be called outside the lexer definition")
def Ignored(using ctx: LexerCtx): Token[?, ctx.type, Nothing]
```

Matches text but produces no lexeme.

**Example:**

```scala
case "#.*" => Token.Ignored
case "\\s+" => Token.Ignored
```

#### `Token[Name]`

```scala
@compileTimeOnly("Should never be called outside the lexer definition")
def apply[Name <: ValidName](using ctx: LexerCtx): Token[Name, ctx.type, String]
```

Produces a token with the matched text as a `String` value.

**Example:**

```scala
case "[0-9]+" => Token["int"]
case "[a-zA-Z_][\\w]*" => Token["id"]
```

#### `Token[Name](value)`

```scala
@compileTimeOnly("Should never be called outside the lexer definition")
def apply[Name <: ValidName](value: Any)(using ctx: LexerCtx): Token[Name, ctx.type, value.type]
```

Produces a token with a custom-extracted value.

**Example:**

```scala
case x @ "[0-9]+" => Token["int"](x.toInt)
case x @ "[0-9]+\\.[0-9]+" => Token["float"](x.toDouble)
case s @ "\"[^\"]*\"" => Token["string"](s.drop(1).dropRight(1))
```

---

## Context Types

### `LexerCtx` Trait

```scala
trait LexerCtx:
  this: Product =>

  var lastLexeme: Lexeme[?, ?] | Null = compiletime.uninitialized
  var lastRawMatched: String = compiletime.uninitialized
  var text: CharSequence
```

**Fields:**

| Field | Type | Purpose |
|-------|------|----------|
| `text` | `CharSequence` | Remaining input to tokenize (required for custom contexts) |
| `lastRawMatched` | `String` | Raw matched substring; set by `BetweenStages` |
| `lastLexeme` | `Lexeme[?, ?] \| Null` | Last produced lexeme; set by `BetweenStages` |

### `LexerCtx.Default`

```scala
final case class Default(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx
    with PositionTracking
    with LineTracking
```

Default context with position and line tracking.

**Fields:**

| Field | Type | Purpose |
|-------|------|----------|
| `text` | `CharSequence` | Remaining input (1-indexed) |
| `position` | `Int` | Current character position (1-based) |
| `line` | `Int` | Current line number (1-based) |

### `LexerCtx.Empty`

```scala
final case class Empty(
  var text: CharSequence = "",
) extends LexerCtx
```

Minimal context with no position tracking.

---

## Lexeme and Token Info

### `Lexeme[Name, Value]`

```scala
case class Lexeme[Name <: String, Value](
  name: Name,
  value: Value,
  contextSnapshot: Map[String, Any],
)
```

Runtime representation of a matched token.

**Fields:**

| Field | Type | Purpose |
|-------|------|----------|
| `name` | `Name` | Token name (e.g., `"int"`, `"id"`) |
| `value` | `Value` | Token value (type depends on token definition) |
| `contextSnapshot` | `Map[String, Any]` | Snapshot of context fields at creation time |

### `TokenInfo`

```scala
case class TokenInfo(
  name: String,
  pattern: String,
  index: Int,
)
```

Metadata about a token type.

**Fields:**

| Field | Type | Purpose |
|-------|------|----------|
| `name` | `String` | Token name (e.g., `"int"`) |
| `pattern` | `String` | Regex pattern that matches this token |
| `index` | `Int` | Declaration order (0-based) |

---

## Type Classes

### `Copyable[Ctx]`

```scala
trait Copyable[Ctx <: LexerCtx] {
  def copy(ctx: Ctx): Ctx
}
```

Enables context cloning. Auto-derived for case classes.

**Manual Implementation:**

```scala
given [Ctx <: LexerCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
  Copyable.derived

given Copyable[CustomCtx] = new Copyable[CustomCtx] {
  def copy(ctx: CustomCtx): CustomCtx = ???
}
```

### `BetweenStages[Ctx]`

```scala
trait BetweenStages[Ctx <: LexerCtx] {
  def apply(
    tokenDef: TokenDef[Ctx],
    match: MatchResult,
    ctx: Ctx,
  ): Unit
}
```

Handles context updates after each regex match.

**Default Implementation:**

For `DefinedToken`, creates a `Lexeme`; for `IgnoredToken`, skips lexeme creation.

**Custom Implementation:**

```scala
given BetweenStages[MyCtx] = new BetweenStages[MyCtx] {
  def apply(
    tokenDef: TokenDef[MyCtx],
    m: MatchResult,
    ctx: MyCtx,
  ): Unit = {
    // Custom logic
    ctx.lastRawMatched = m.matched
    ctx.text = ctx.text.from(m.end())
    // ... etc
  }
}
```

### `LexerRefinement[Ctx]`

```scala
trait LexerRefinement[Ctx <: LexerCtx] {
  type Lexeme[Name <: String, Value] <: alpaca.internal.lexer.Lexeme[Name, Value]
}
```

Type-level refinement for lexemes. Usually left at default.

---

## Position and Line Tracking Traits

### `PositionTracking`

```scala
trait PositionTracking {
  self: LexerCtx =>
  var position: Int
}
```

Mixin trait for contexts that track character position.

### `LineTracking`

```scala
trait LineTracking {
  self: LexerCtx =>
  var line: Int
}
```

Mixin trait for contexts that track line number.

**Usage:**

```scala
case class MyCtx(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx
    with PositionTracking
    with LineTracking
```

---

## Common Patterns and Idioms

### Keyword Detection

```scala
val keywords = "if" | "else" | "while" | "for" | "return"

val Lexer = lexer {
  case keyword @ keywords => Token[keyword.type]  // Singleton type for keyword
  case x @ "[a-zA-Z_][\\w]*" => Token["id"](x)
}
```

### Numeric Literals with Type Coercion

```scala
val Lexer = lexer {
  case x @ "[0-9]+\\.[0-9]+([eE][+-]?[0-9]+)?" => Token["float"](x.toDouble)
  case x @ "0x[0-9a-fA-F]+" => Token["hexInt"](Integer.parseInt(x.substring(2), 16))
  case x @ "0o[0-7]+" => Token["octInt"](Integer.parseInt(x.substring(2), 8))
  case x @ "0b[01]+" => Token["binInt"](Integer.parseInt(x.substring(2), 2))
  case x @ "[0-9]+" => Token["int"](x.toLong)
}
```

### String Literals with Escape Handling

```scala
val Lexer = lexer {
  case x @ "\"(?:[^\"\\\\]|\\\\\\.)*\"" => 
    Token["string"](
      x.drop(1).dropRight(1)
        .replace("\\\\\"", "\"")
        .replace("\\\\\\\\", "\\\\")
        // ... handle other escapes
    )
}
```

### Context Mutation for State Tracking

```scala
case class CountingCtx(
  var text: CharSequence = "",
  var count: Int = 0,
) extends LexerCtx

val Lexer = lexer[CountingCtx] {
  case "x" =>
    ctx.count += 1
    Token["x"](ctx.count)
  
  case "y" => Token["y"](ctx.count)
}
```

---

## Error Handling

### Tokenization Exceptions

When no pattern matches the current input position:

```scala
val (ctx, lexemes) = Lexer.tokenize("invalid???")
// Throws TokenizationException if "invalid???" has no matching pattern
```

**Catching Errors:**

```scala
try {
  val (ctx, lexemes) = Lexer.tokenize(input)
  // Process lexemes
} catch {
  case e: TokenizationException =>
    println(s"Lexing failed: ${e.getMessage}")
    // Handle error
}
```

### Custom Error Handling with Context

```scala
case class ErrorTrackingCtx(
  var text: CharSequence = "",
  var errors: List[String] = Nil,
) extends LexerCtx

val Lexer = lexer[ErrorTrackingCtx] {
  case "??" =>
    ctx.errors :+= "Unexpected token"
    Token["error"]
  
  // ... other patterns ...
}

val (finalCtx, _) = Lexer.tokenize(input)
if finalCtx.errors.nonEmpty then
  finalCtx.errors.foreach(println(_))
```

---

## Testing Utilities

Common patterns for testing lexers:

### Pattern Order Verification

```scala
test("Token patterns in declaration order") {
  Lexer.tokens.map(_.pattern) shouldBe List(
    "<=", ">=", "<", ">",
    "[0-9]+",
    "[a-zA-Z_][\\w]*",
  )
}
```

### Type Assertion

```scala
test("Token types are correct") {
  val intToken: Token["int", LexerCtx.Default, Int] = Lexer.int
  val idToken: Token["id", LexerCtx.Default, String] = Lexer.id
  // Compiles only if types match
}
```

### Tokenization Assertion

```scala
test("Tokenize arithmetic expression") {
  val (_, lexemes) = Lexer.tokenize("1 + 2")
  
  lexemes.map(_.name) shouldBe List("int", "plus", "int")
  lexemes.map(_.value) shouldBe List(1, (), 2)
}
```

---

## Debugging

### Introspection

```scala
// List all tokens
Lexer.tokens.foreach { t =>
  println(s"${t.name} (pattern: ${t.pattern}, index: ${t.index})")
}

// Inspect a specific token
val token = Lexer.int
println(s"Token type: ${token}")
```

### Context Snapshots

```scala
val (ctx, lexemes) = Lexer.tokenize("1 + 2")

lexemes.foreach { lexeme =>
  println(s"${lexeme.name}:")
  lexeme.contextSnapshot.foreach { (k, v) =>
    println(s"  $k = $v")
  }
}
```

### Verbose Output

```scala
@main def debug(): Unit =
  val Lexer = lexer(using DebugSettings(verbose = true)) {
    // ... rules ...
  }
  // Prints generated code and intermediate representations
```

---

## Imports

### Minimal

```scala
import alpaca.*
```

This imports:
- `lexer` macro
- `Token` object and methods
- `LexerCtx` base trait
- `Lexeme` data class
- `Tokenization` interface

### Comprehensive

```scala
import alpaca.*
import alpaca.internal.lexer.*
import alpaca.internal.{Copyable, Showable}
```

For advanced usage with custom type classes.

---

## Version History

### 0.0.1 (Initial Release)

- Core `lexer` macro
- `Token` DSL (Ignored, basic, with value)
- `LexerCtx.Default` and `LexerCtx.Empty`
- `Tokenization` runtime API
- `Copyable` auto-derivation

---

## See Also

- [Lexer Development Guide](./lexer-development.md) — Comprehensive reference
- [Lexer Quickstart](./lexer-quickstart.md) — Practical examples
- [Lexer Internals](./lexer-internals.md) — Macro implementation details
