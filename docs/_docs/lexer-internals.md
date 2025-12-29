# Lexer Internals and Macro Implementation

This document describes the internal architecture of the Alpaca lexer, including the macro implementation, type-level contracts, and runtime execution model. This is intended for contributors and those who need to extend or customize the lexer beyond the public API.

## Table of Contents

1. [Macro Expansion Pipeline](#macro-expansion-pipeline)
2. [Type-Level Architecture](#type-level-architecture)
3. [Runtime Execution Model](#runtime-execution-model)
4. [Internal Type Classes](#internal-type-classes)
5. [LazyReader and String Scanning](#lazyreader-and-string-scanning)
6. [Copyable Type Class](#copyable-type-class)
7. [Extending the Lexer](#extending-the-lexer)

---

## Macro Expansion Pipeline

The `lexer` macro (located in `src/alpaca/internal/lexer/Lexer.scala`) transforms the DSL into efficient code through several stages:

### Stage 1: Extracting Rules

The macro analyzes the partial function to extract:

```scala
// Input (in the DSL)
case num @ "[0-9]+" => Token["int"](num.toInt)
case "\\s+" => Token.Ignored

// Extracted as
LexerRule(
  pattern = "[0-9]+",
  tokenDef = DefinedToken(
    name = "int",
    valueType = Int,
    extractor = { matched => matched.toInt }
  )
)

LexerRule(
  pattern = "\\s+",
  tokenDef = IgnoredToken()
)
```

The macro uses quoted expressions and splicing to analyze the case patterns.

### Stage 2: Pattern Compilation and Validation

Each regex pattern is validated and compiled at macro time:

```scala
val patterns: List[java.util.regex.Pattern] = rules.map { rule =>
  try {
    Pattern.compile(rule.pattern)
  } catch {
    case e: PatternSyntaxException =>
      report.errorAndAbort(
        s"Invalid regex '${rule.pattern}': ${e.getMessage}"
      )
  }
}
```

This ensures invalid regexes are caught at compile time, not runtime.

### Stage 3: Type Inference for Token Values

The macro infers the value type from the token definition:

```scala
// Case 1: Token["name"] -> type is String (the matched text)
case x @ "[0-9]+" => Token["int"]
// Inferred: Token["int", Ctx, String]

// Case 2: Token["name"](value) -> type is typeof(value)
case x @ "[0-9]+" => Token["int"](x.toInt)
// Inferred: Token["int", Ctx, Int]

// Case 3: Token.Ignored -> no type, produces no lexeme
case "\\s+" => Token.Ignored
// Inferred: IgnoredToken
```

The macro uses Scala 3's type narrowing to ensure type safety at compile time.

### Stage 4: Tokenization Function Generation

The macro generates a specialized `tokenize` function:

```scala
// Pseudocode of generated function
def tokenize(input: String): (Ctx, List[Lexeme[?, ?]]) = {
  val ctx = Ctx()  // Initialize context
  val lexemes = scala.collection.mutable.ListBuffer[Lexeme[?, ?]]()
  
  while (ctx.text.nonEmpty) {
    var matched = false
    
    // Pattern 1: "[0-9]+" -> Token["int"]
    if (!matched) {
      val m = Pattern.compile("[0-9]+").matcher(ctx.text)
      if (m.lookingAt()) {
        val matched_str = m.group(0)
        val value = matched_str.toInt
        ctx.text = ctx.text.substring(m.end())
        // Apply BetweenStages to create lexeme
        lexemes += Lexeme("int", value, ...)
        matched = true
      }
    }
    
    // Pattern 2: "\\s+" -> Token.Ignored
    if (!matched) {
      val m = Pattern.compile("\\s+").matcher(ctx.text)
      if (m.lookingAt()) {
        ctx.text = ctx.text.substring(m.end())
        // No lexeme created
        matched = true
      }
    }
    
    if (!matched) {
      // No pattern matched; error
      throw TokenizationException(...)
    }
  }
  
  (ctx, lexemes.toList)
}
```

Key points:
- All pattern matching is unrolled into `if-else` chains
- Each pattern uses `lookingAt()` for prefix matching (not `find()`)
- Context is updated after each successful match
- Lexemes are created only for defined tokens

### Stage 5: Accessor Generation

For each token name, a public field is generated:

```scala
// Generated for case ... => Token["int"]
def int: Token["int", Ctx, Int] =
  DefinedToken(
    info = TokenInfo("int", "[0-9]+", 0),
    modifyCtx = ctx => {},  // or custom mutations
    remapping = ctx => ???  // value extractor
  )
```

These accessors provide type-safe references for downstream code (e.g., parsers).

---

## Type-Level Architecture

### Token Type Signature

Tokens are parameterized by three type parameters:

```scala
sealed trait Token[Name <: String, Ctx <: LexerCtx, Value]
```

| Parameter | Purpose | Example |
|-----------|---------|----------|
| `Name` | The token's literal name as a singleton type | `"int"` |
| `Ctx` | The context type used during lexing | `LexerCtx.Default` |
| `Value` | The type of the token's carried value | `Int`, `String`, `Unit` |

Example:

```scala
case x @ "[0-9]+" => Token["int"](x.toInt)
// Type: Token["int", LexerCtx.Default, Int]

case "return" => Token["return"]
// Type: Token["return", LexerCtx.Default, String]

case "#.*" => Token.Ignored
// Type: IgnoredToken (no value)
```

### Lexeme Type Signature

A lexeme is the runtime representation of a matched token:

```scala
case class Lexeme[Name <: String, Value](
  name: Name,
  value: Value,
  contextSnapshot: Map[String, Any],
)
```

The `contextSnapshot` is a map of context field names to their values at the moment the lexeme was created. This is useful for error reporting and debugging.

### ValidName Constraint

Token names must be valid Scala identifiers:

```scala
type ValidName = String

// Valid
case "[0-9]+" => Token["int"]
case "+" => Token["plus"]
case "my_var" => Token["id"]

// Invalid (would fail at macro time)
case "[0-9]+" => Token["123"]  // Can't start with digit
case "+" => Token["plus+"]      // Can't contain '+'
```

The `ValidName` constraint ensures that generated accessor names are syntactically valid.

---

## Runtime Execution Model

### Initialization Phase

```scala
val Lexer = lexer { ... }
// At this point:
// 1. All patterns are compiled into java.util.regex.Pattern instances
// 2. All token accessors are generated
// 3. The tokenize function is ready
```

### Tokenization Phase

```scala
val input = "x = 123"
val (ctx, lexemes) = Lexer.tokenize(input)
```

Step-by-step:

1. **Initialize context**: `ctx = LexerCtx.Default(text = input, position = 1, line = 1)`
2. **Loop while input remains**:
   - Try each pattern in order (using `Pattern.matcher(...).lookingAt()`)
   - If pattern matches:
     - Call token action (e.g., `x.toInt`)
     - Call `BetweenStages` to update context and create lexeme
     - Add lexeme to output (if defined token)
     - Continue to next iteration
   - If no pattern matches:
     - Raise `TokenizationException`
3. **Return** `(finalContext, lexemeList)`

### Critical Detail: `lookingAt()` vs `find()`

The lexer uses `lookingAt()`, which matches at the **beginning** of the text:

```scala
// Given: text = "123abc"

Pattern.compile("[0-9]+").matcher(text).lookingAt()  // true
Pattern.compile("[0-9]+").matcher(text).find()        // true

Pattern.compile("abc").matcher(text).lookingAt()      // false
Pattern.compile("abc").matcher(text).find()           // true (matches later)
```

This ensures tokens are matched in order from the current position, not from some arbitrary later position.

---

## Internal Type Classes

### BetweenStages[Ctx]

Dictates how the context is updated after each token match:

```scala
trait BetweenStages[Ctx <: LexerCtx] {
  def apply(
    tokenDef: TokenDef[Ctx],
    match: MatchResult,
    ctx: Ctx,
  ): Unit
}
```

The default implementation (for any `LexerCtx`):

```scala
given BetweenStages[LexerCtx] =
  case (DefinedToken(info, modifyCtx, remapping), m, ctx) =>
    // Update raw matched text
    ctx.lastRawMatched = m.matched
    
    // Advance position
    ctx.text = ctx.text.from(m.end())
    
    // Apply user-defined mutations
    modifyCtx(ctx)
    
    // Create lexeme with snapshot
    val ctxAsProduct = ctx.asInstanceOf[Product]
    val fields = ctxAsProduct.productElementNames
      .zip(ctxAsProduct.productIterator)
      .toMap + ("text" -> ctx.lastRawMatched)
    
    ctx.lastLexeme = Lexeme(
      name = info.name,
      value = remapping(ctx),
      contextSnapshot = fields
    )
  
  case (IgnoredToken(_, modifyCtx), m, ctx) =>
    ctx.lastRawMatched = m.matched
    ctx.text = ctx.text.from(m.end())
    modifyCtx(ctx)
```

**Customization:** Provide your own `given BetweenStages[MyCtx]` to override the behavior.

### Copyable[Ctx]

Enables efficient context cloning for backtracking or multi-path exploration:

```scala
trait Copyable[Ctx <: LexerCtx] {
  def copy(ctx: Ctx): Ctx
}
```

For case classes, this is auto-derived:

```scala
given [Ctx <: LexerCtx & Product: Mirror.ProductOf]: Copyable[Ctx] =
  Copyable.derived
```

For custom types, implement manually:

```scala
case class MyCtx(...) extends LexerCtx

given Copyable[MyCtx] = new Copyable[MyCtx] {
  def copy(ctx: MyCtx): MyCtx = ctx.copy()  // or manual cloning
}
```

### LexerRefinement[Ctx]

Defines how lexeme values are refined based on the token and context:

```scala
trait LexerRefinement[Ctx <: LexerCtx] {
  type Lexeme[Name <: String, Value] <: alpaca.internal.lexer.Lexeme[Name, Value]
}
```

This is mostly for type-level refinement and can usually be left at its default.

---

## LazyReader and String Scanning

The `LazyReader` type (in `src/alpaca/internal/lexer/LazyReader.scala`) provides efficient substring operations:

```scala
case class LazyReader(
  input: CharSequence,
  offset: Int = 0,
) {
  def from(newOffset: Int): LazyReader =
    LazyReader(input, newOffset)
  
  def remaining: String =
    input.subSequence(offset, input.length).toString
  
  def length: Int =
    input.length - offset
  
  // For pattern matching
  def matcher(pattern: Pattern): Matcher =
    pattern.matcher(remaining)
}
```

**Key insight:** Instead of creating new strings for each substring, `LazyReader` keeps a reference to the original input and tracks an offset. This avoids O(n) string copies.

### Used in Lexer Context

Alpaca uses `LazyReader` internally but exposes `CharSequence` in the public API:

```scala
trait LexerCtx {
  var text: CharSequence  // Can be String or LazyReader
}

// Usage
ctx.text = ctx.text.from(m.end())  // Efficient slicing
```

---

## Copyable Type Class

The `Copyable[Ctx]` type class is crucial for safe context handling:

```scala
trait Copyable[Ctx <: LexerCtx] {
  def copy(ctx: Ctx): Ctx
}
```

### Auto-Derivation

For case classes (products), Scala 3's `deriving` mechanism provides automatic implementation:

```scala
case class MyCtx(
  var text: CharSequence = "",
  var line: Int = 1,
) extends LexerCtx

// Automatically generates:
// given Copyable[MyCtx] = new Copyable[MyCtx] {
//   def copy(ctx: MyCtx): MyCtx =
//     MyCtx(text = ctx.text, line = ctx.line)
// }
```

### Manual Implementation

For non-case-class contexts:

```scala
class MyCtx extends LexerCtx {
  var text: CharSequence = ""
  var state: State = State.Initial
  
  def copy(): MyCtx = {
    val c = new MyCtx
    c.text = text
    c.state = state  // shallow copy
    c
  }
}

given Copyable[MyCtx] = new Copyable[MyCtx] {
  def copy(ctx: MyCtx): MyCtx = ctx.copy()
}
```

---

## Extending the Lexer

### Custom Context with Advanced State Tracking

```scala
case class AdvancedCtx(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
  var column: Int = 1,
  var indentStack: List[Int] = List(0),
  var nestedComments: Int = 0,
  var diagnostics: List[Diagnostic] = Nil,
) extends LexerCtx
    with PositionTracking
    with LineTracking {
  
  def currentIndent: Int = indentStack.head
  
  def pushIndent(level: Int): Unit =
    indentStack = level :: indentStack
  
  def popIndent(): Unit =
    indentStack = indentStack.tail
  
  def addError(msg: String): Unit =
    diagnostics :+= Diagnostic.Error(msg, line, column)
}

val IndentSensitiveLexer = lexer[AdvancedCtx] {
  case "\\n" =>
    ctx.line += 1
    ctx.column = 1
    Token.Ignored
  
  case "  " =>  // two spaces = one indent level
    ctx.column += 2
    ctx.pushIndent(ctx.currentIndent + 1)
    Token["indent"]
  
  case _ => ...
}
```

### Custom BetweenStages for Instrumentation

```scala
case class InstrumentedCtx(
  var text: CharSequence = "",
  var tokenCount: Int = 0,
  var byteCount: Long = 0L,
) extends LexerCtx

given BetweenStages[InstrumentedCtx] =
  case (DefinedToken(info, modifyCtx, remapping), m, ctx) =>
    ctx.tokenCount += 1
    ctx.byteCount += m.end()
    ctx.lastRawMatched = m.matched
    ctx.text = ctx.text.from(m.end())
    modifyCtx(ctx)
    // ... create lexeme as usual
  
  case (IgnoredToken(_, modifyCtx), m, ctx) =>
    ctx.lastRawMatched = m.matched
    ctx.text = ctx.text.from(m.end())
    modifyCtx(ctx)

val InstrumentedLexer = lexer[InstrumentedCtx] {
  // ... rules ...
}
```

### Custom Copyable for Non-Standard Contexts

```scala
class StatefulCtx extends LexerCtx {
  var text: CharSequence = ""
  val stateMap: mutable.Map[String, Any] = mutable.Map()
  
  def copy(): StatefulCtx = {
    val c = new StatefulCtx
    c.text = text
    c.stateMap.addAll(stateMap)  // shallow copy of map
    c
  }
}

given Copyable[StatefulCtx] = new Copyable[StatefulCtx] {
  def copy(ctx: StatefulCtx): StatefulCtx = ctx.copy()
}
```

---

## Performance Considerations

### Compile-Time vs Runtime

- **Compile-time**: Pattern validation, type checking, code generation
- **Runtime**: Actual tokenization, pattern matching, context updates

No overhead for:
- Pattern compilation (done at macro time)
- Token name/type information (statically known)
- Code path selection (unrolled if-else, not dynamic dispatch)

### Micro-Optimizations

1. **Reuse Matchers**: The current implementation creates a new `Matcher` for each pattern on each character. For large files, consider caching.

2. **Lazy Context**: Use `LazyReader` to avoid substring copies.

3. **Early Termination**: If an error is fatal, return immediately instead of accumulating.

### Benchmarking

For large inputs, measure with a representative sample:

```scala
import scala.util.Using

@main def benchmark(): Unit =
  val largeInput = scala.io.Source.fromFile("large.txt").mkString
  
  val start = System.nanoTime()
  val (_, lexemes) = MyLexer.tokenize(largeInput)
  val end = System.nanoTime()
  
  val timeMs = (end - start) / 1_000_000.0
  val tokensPerMs = lexemes.length / timeMs
  
  println(f"Time: $timeMs%.2f ms")
  println(f"Throughput: $tokensPerMs%.0f tokens/ms")
```

---

## Debugging Tips

### Enable Macro Debug Output

```scala
val Lexer = lexer[MyCtx](using debugSettings = DebugSettings(verbose = true)) {
  // ... rules ...
}
```

This prints the generated tokenization function and other intermediate representations.

### Inspect Generated Code

After compilation, check the generated class file:

```bash
javap -c -classpath out/classes MyLexer$.class | grep -A50 tokenize
```

### Manual Testing

```scala
import scala.util.Using

@main def testLexer(): Unit =
  val test = "x = 123"
  val (ctx, lexemes) = MyLexer.tokenize(test)
  
  println("=== Context ===")
  println(s"Final text: '${ctx.text}'")
  println(s"Position: ${ctx.position}")
  println(s"Line: ${ctx.line}")
  
  println("\n=== Lexemes ===")
  lexemes.foreach { lexeme =>
    println(s"${lexeme.name}: ${lexeme.value}")
  }
  
  println("\n=== Snapshots ===")
  lexemes.foreach { lexeme =>
    println(s"${lexeme.name}:")
    lexeme.contextSnapshot.foreach { (k, v) =>
      println(s"  $k = $v")
    }
  }
```

---

## Further Reading

- `src/alpaca/internal/lexer/Lexer.scala` — Macro implementation
- `src/alpaca/internal/lexer/Token.scala` — Token type definitions
- `src/alpaca/internal/lexer/Tokenization.scala` — Runtime tokenization interface
- `src/alpaca/internal/lexer/BetweenStages.scala` — Context update semantics
- [Lexer Development Guide](./lexer-development.md) — Public API and usage
- [Lexer Quickstart](./lexer-quickstart.md) — Practical examples
