# OnTokenMatch

The Alpaca lexer and parser are two independent compilation stages connected by a single data contract: the `Lexeme`.
When you call `tokenize()`, the lexer matches tokens against the input, runs the `OnTokenMatch` hook after each match, and collects the results into a `List[Lexeme]`.
When you call `parse()`, the parser consumes that list.
The `OnTokenMatch` hook is responsible for advancing the text cursor, constructing each lexeme with its context snapshot, and applying any custom side effects you configure.

Most programs need nothing more than this:

```scala sc:nocompile sc-compile-with:BrainLexer.scala,BrainParser.scala
val (ctx, lexemes) = BrainLexer.tokenize("[>+<-]")
val (_, ast) = BrainParser.parse(lexemes)
```

This page explains what is inside those lexemes, how to customize the pipeline, and how the data flows between stages.

## Connecting Lexer Output to Parser Input

The `tokenize()` method returns a named tuple `(ctx: Ctx, lexemes: List[Lexeme])`:

```scala sc:nocompile
import alpaca.*

val (ctx, lexemes) = BrainLexer.tokenize("++[>+<-].")

// ctx holds the final lexer context state
// lexemes holds the matched tokens (Token.Ignored entries are excluded)

val (_, ast) = BrainParser.parse(lexemes)
```

The parser accepts `List[Lexeme[?, ?]]` and appends `Lexeme.EOF` internally before processing begins. You do not need to add an end-of-input marker yourself.

The final context (`ctx`) is useful for post-tokenization checks. For example, the BrainFuck lexer tracks bracket depth — after tokenization, you can verify all brackets are balanced:

```scala sc:nocompile
val (ctx, lexemes) = BrainLexer.tokenize("[>+<-]")
require(ctx.squareBrackets == 0, "Mismatched brackets")
val (_, ast) = BrainParser.parse(lexemes)
```

## Custom OnTokenMatch

The default `OnTokenMatch[LexerCtx]` handles cursor advancement, snapshot construction, and context updates for all standard use cases. Customize it only when you need per-token side effects beyond what context fields can express -- for example, writing to an external log, emitting metrics, or computing aggregate statistics that must update outside the context object.

The correct pattern mirrors how Alpaca's built-in `PositionTracking` and `LineTracking` traits work. It requires a trait (not a case class) so that the auto-composition macro can discover your hook by inspecting the context's linearized parent types at compile time.

1. Define a custom **trait** extending `LexerCtx`.
2. Provide `given OnTokenMatch[YourTrait]` in that trait's **companion object**.
3. Have your case class extend the trait.
4. The `auto` macro at compile time finds all `OnTokenMatch` instances from parent traits and composes them automatically.

```scala sc:nocompile
import alpaca.*

// Step 1: Trait extending LexerCtx
trait IndentTracking extends LexerCtx:
  this: Product =>
  var indentLevel: Int

// Step 2: given in TRAIT COMPANION (not case class companion)
object IndentTracking:
  given OnTokenMatch[IndentTracking] = (token, matched, ctx) =>
    if matched == "\n" then ctx.indentLevel = 0

// Step 3: Case class extends both
case class MyCtx(
  var indentLevel: Int = 0,
) extends LexerCtx with IndentTracking

// Step 4: Pass MyCtx to lexer -- auto composition happens at compile time
val Lexer = lexer[MyCtx]:
  case "\\n" => Token.Ignored
  case id @ "[a-z]+" => Token["ID"](id)
```

Do **not** define `given OnTokenMatch[MyCtx]` directly on the concrete case class. Doing so bypasses the auto-composition mechanism: the lexer will use your hook but skip the default hook that advances the text cursor, and tokenization will loop indefinitely. Always put the `given` in a **trait companion**, not a case class companion.

For reference, the `OnTokenMatch` type is a function:

```scala sc:nocompile
import alpaca.*

// OnTokenMatch[Ctx] extends ((Token[?, Ctx, ?], String, Ctx) => Unit)
// token:   Token[?, Ctx, ?]  -- either DefinedToken or IgnoredToken
// matched: String             -- the matched text
// ctx:     Ctx                -- the current context (mutable, updated in place)
```

See the [Lexer Context](lexer-context.md) page for the full reference on `OnTokenMatch` composition and the built-in `PositionTracking` and `LineTracking` traits.

## Data Flow Summary

Each call to `tokenize()` follows this sequence:

1. The lexer attempts to match the remaining input against each rule pattern in order. The first match wins. If no pattern matches, a `RuntimeException` is thrown with the unexpected character.
2. `Tokenization.tokenize` advances the text cursor (`ctx.text`) past the matched string and records the matched text in `ctx.lastRawMatched`.
3. `OnTokenMatch` runs. The default `OnTokenMatch[LexerCtx]` applies any rule-body context changes (`modifyCtx`), derives a snapshot from the context's `Product` elements (case class fields), overrides the snapshot's `text` field with the matched string, and — for `DefinedToken`s — builds a `Lexeme` from the token name, value, and snapshot.
4. If the matched token is `Token.Ignored` (or a recovery token), `OnTokenMatch` still runs the context modifications and tracking updates but does not emit a `Lexeme`. The token is invisible to the parser.
5. Tracking hooks (`PositionTracking`, `LineTracking`, custom traits) run as part of the composed `OnTokenMatch`, updating `position`, `line`, etc.
5. This repeats until the entire input is consumed. `tokenize()` then returns the named tuple `(ctx, lexemes)` -- the final context state and the complete lexeme list.
6. `parse(lexemes)` receives the list, appends `Lexeme.EOF` internally, and runs the parser grammar against the sequence.

The `Lexeme` list is immutable after `tokenize()` returns. The parser does not alter the lexeme data.

See [Lexer](lexer.md) for lexer definition, [Lexer Context](lexer-context.md) for custom contexts and tracking traits, and [Parser](parser.md) for grammar rules.
