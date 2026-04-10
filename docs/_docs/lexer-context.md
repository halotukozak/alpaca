# Lexer Context

Every Alpaca lexer carries a **context** object that evolves as the input is processed. Context lets you do stateful lexing: counting brackets, tracking indentation, recording whether you are inside a string literal, or anything else that depends on the tokens seen so far.

By default, the lexer uses `LexerCtx.Default`, which gives you position and line tracking with no extra setup.

<details>
<summary>Under the hood: BetweenStages composition</summary>

When you write `lexer[MyCtx]:`, the Alpaca macro inspects `MyCtx`'s type hierarchy at compile time. It discovers all `BetweenStages` instances from parent traits (e.g., `PositionTracking`, `LineTracking`) and composes them into a single hook via `BetweenStages.auto`. The resulting hook is wired into the generated tokenizer -- at runtime, context field updates happen automatically after each token match.

</details>

## Default Context

When you write a `lexer:` block without a type parameter, the lexer uses `LexerCtx.Default`. It tracks two fields:

- `position` -- 1-based character offset, incremented by the length of each matched token
- `line` -- 1-based line number, incremented on each newline character

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\s+" => Token.Ignored

val (ctx, lexemes) = BrainLexer.tokenize("+ - +")
// ctx.position == 6
// ctx.line     == 1
//
// Each lexeme carries a snapshot of context fields at match time:
// inc: text="+", position=2, line=1
// dec: text="-", position=4, line=1
// inc: text="+", position=6, line=1
```

Position advances by the matched length after each token. The snapshot captures values *after* the token was consumed, not before.

## The LexerCtx Trait

`LexerCtx` is the base trait for all lexer contexts. Any custom context must satisfy two rules:

1. **It must be a case class** -- `LexerCtx` has a `this: Product =>` self-type, and the auto-derivation machinery requires a `Product` instance.
2. **All fields must have default values** -- The `Empty[T]` derivation macro reads default parameter values from the companion to construct the initial context. If any parameter lacks a default, the macro fails at compile time.

> **Warning:** Do not declare `var text`, `var lastLexeme`, or `var lastRawMatched` in your case class. These fields are provided by the `LexerCtx` trait and managed internally by the lexer. Redeclaring them shadows the internal fields and breaks tokenization.

Mutable state fields must be `var`, not `val` -- the lexer assigns to them directly. Exception: a field of a mutable collection type (e.g., `scala.collection.mutable.Stack`) can be `val` because you mutate the collection itself, not the reference.

## Custom Context

The BrainFuck lexer from [Getting Started](getting-started.md) does not validate bracket matching -- it tokenizes `]` even without a prior `[`. To fix that, we track bracket depth in a custom context:

```scala sc:nocompile
import alpaca.*

case class BrainLexContext(
  var brackets: Int = 0,
  var squareBrackets: Int = 0,
) extends LexerCtx

val BrainLexer = lexer[BrainLexContext]:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "\\[" =>
    ctx.squareBrackets += 1
    Token["jumpForward"]
  case "\\]" =>
    require(ctx.squareBrackets > 0, "Mismatched brackets")
    ctx.squareBrackets -= 1
    Token["jumpBack"]
  case name @ "[A-Za-z]+" => Token["functionName"](name)
  case "\\(" =>
    ctx.brackets += 1
    Token["functionOpen"]
  case "\\)" =>
    require(ctx.brackets > 0, "Mismatched brackets")
    ctx.brackets -= 1
    Token["functionClose"]
  case "!" => Token["functionCall"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored
```

The type parameter `lexer[BrainLexContext]` tells the macro which context to use. The final context state is returned as the `ctx` component of the named tuple from `tokenize()`:

```scala sc:nocompile
val (finalCtx, lexemes) = BrainLexer.tokenize("[>+<-]")
// finalCtx.squareBrackets == 0  -- balanced
```

## Accessing Context in Patterns

Inside a `lexer[Ctx]:` block, the name `ctx` is implicitly available and refers to the current context object. You can read and write any `var` field on it:

```scala sc:nocompile
case "\\[" =>
  ctx.squareBrackets += 1       // write
  Token["jumpForward"]
case "\\]" =>
  require(ctx.squareBrackets > 0, "Mismatched brackets")  // read + validate
  ctx.squareBrackets -= 1       // write
  Token["jumpBack"]
```

> **Note on guards:** Guards (`case "regex" if condition =>`) are not supported in lexer rules. Use the rule body to read context state and decide what to emit -- you cannot filter matches before they occur.

## Context Snapshots in Lexemes

Each `Lexeme` carries a snapshot of all context fields at the moment of the match. Access them by name via `Selectable`:

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case "\\+" => Token["inc"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = BrainLexer.tokenize("+ +")
lexemes(0).position  // 2: Int (post-match position)
lexemes(0).line      // 1: Int
lexemes(0).text      // "+": String (the matched text, not remaining input)
```

The type safety comes from `Selectable`: the `tokenize()` return type carries a structural refinement that encodes every context field and its type. If you access a field that does not exist on the context type (e.g., `.brackets` when using `LexerCtx.Default`), the compiler reports a type error.

Two important details:

- **`text` is the matched string**, not the remaining input. The snapshot replaces `text` with the actual matched characters for that token.
- **Snapshots are independent.** Each lexeme captures the context state at its own match time. Modifying the context after a match does not retroactively change earlier lexemes.

For custom contexts, all case class fields appear in the snapshot:

```scala sc:nocompile
import alpaca.*

case class BrainLexContext(
  var squareBrackets: Int = 0,
) extends LexerCtx

val BrainLexer = lexer[BrainLexContext]:
  case "\\[" =>
    ctx.squareBrackets += 1
    Token["jumpForward"]
  case "\\]" =>
    ctx.squareBrackets -= 1
    Token["jumpBack"]
  case "\\+" => Token["inc"]
  case "." => Token.Ignored

val (_, lexemes) = BrainLexer.tokenize("[+[+]]")
// lexemes(0).squareBrackets == 1  -- after first [
// lexemes(2).squareBrackets == 2  -- after second [
// lexemes(4).squareBrackets == 1  -- after first ]
```

## Built-in Tracking Traits

Alpaca provides two stackable traits for common tracking needs:

**`PositionTracking`** adds a `var position: Int` field and increments it by the matched length after each token. On a newline match, position resets to 1 (start of next line).

**`LineTracking`** adds a `var line: Int` field and increments it when the matched token is a newline.

You can use these traits independently or together. `LexerCtx.Default` extends both. To add them to a custom context:

```scala sc:nocompile
import alpaca.*

case class BrainLexContext(
  var squareBrackets: Int = 0,
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx with PositionTracking with LineTracking
```

With this context, every lexeme carries `squareBrackets`, `position`, and `line` -- all updated automatically.

## The BetweenStages Hook

After every successful token match, Alpaca runs the **BetweenStages** hook for the context type. This hook updates tracking fields and captures the lexeme snapshot.

For custom context types, the hook is auto-derived. The macro inspects all parent traits, summons their `BetweenStages` instances, and composes them. For a context extending `PositionTracking` and `LineTracking`:

1. `BetweenStages[LexerCtx]` -- advances the text cursor, records the lexeme
2. `BetweenStages[PositionTracking]` -- updates the `position` field
3. `BetweenStages[LineTracking]` -- updates the `line` field

All three run automatically after every token match.

<details>
<summary>Under the hood: custom BetweenStages traits</summary>

If you define your own trait extending `LexerCtx` and provide a `given BetweenStages[MyTrait]`, the auto macro will compose it into any context that extends `MyTrait`. This pattern mirrors how `PositionTracking` and `LineTracking` work internally.

Define the `given` in the **trait companion**, not the case class companion. Putting it on the case class bypasses auto-composition: the lexer uses your hook but skips the default hook that advances the text cursor, and tokenization loops indefinitely.

```scala sc:nocompile
import alpaca.*
import alpaca.internal.lexer.BetweenStages

// Step 1: Trait extending LexerCtx
trait IndentTracking extends LexerCtx:
  this: Product =>
  var indentLevel: Int

// Step 2: given in TRAIT COMPANION
object IndentTracking:
  given BetweenStages[IndentTracking] =
    case (_, "\t", ctx) => ctx.indentLevel += 1
    case (_, "\n", ctx) => ctx.indentLevel = 0
    case _ => ()

// Step 3: Case class extends the trait
case class MyCtx(
  var indentLevel: Int = 0,
) extends LexerCtx with IndentTracking

// Step 4: Auto composition happens at compile time
val Lexer = lexer[MyCtx]:
  case "\t" => Token.Ignored
  case "\n" => Token.Ignored
  case id @ "[a-z]+" => Token["ID"](id)
```

</details>

## LexerCtx.Empty

For cases where you need no tracking at all -- no position, no line counter, no custom fields -- use `LexerCtx.Empty`:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer[LexerCtx.Empty]:
  case "\\+" => Token["inc"]
  case "." => Token.Ignored

val (_, lexemes) = Lexer.tokenize("+ +")
// lexemes(0).fields == Map("text" -> "+")  -- only the text field
```

See [Between Stages](between-stages.md) to learn how context snapshots in lexemes flow into the parser.
