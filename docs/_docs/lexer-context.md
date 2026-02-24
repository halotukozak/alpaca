# Lexer Context

Every Alpaca lexer carries a **context** object that evolves as the input is processed.
Context lets you do stateful lexing: tracking indentation depth, counting tokens, recording whether you are inside a string literal, or any other state that depends on the token stream seen so far.
By default, the lexer uses `LexerCtx.Default`, which gives you position and line tracking with no extra setup.

> **Compile-time processing:** When you write `lexer[MyCtx]:`, the Alpaca macro inspects `MyCtx`'s type hierarchy at compile time. It discovers all `BetweenStages` instances from parent traits (e.g., `PositionTracking`, `LineTracking`) and composes them into a single hook via `BetweenStages.auto`. The resulting hook is wired into the generated tokenizer -- at runtime, context field updates happen automatically after each token match.

## Default Context

When you write a `lexer:` block without a type parameter, the lexer automatically uses `LexerCtx.Default`.
It tracks two fields: `position` (1-based character offset, incremented by the length of each matched token) and `line` (1-based line number, incremented on each newline character).

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val (ctx, lexemes) = Lexer.tokenize("42 + 13")
// ctx.position == 8  -- final position after consuming all input
// ctx.line     == 1  -- still on line 1
//
// Each lexeme carries a snapshot of context fields at match time:
// NUM(42):  text="42", position=3, line=1
// PLUS:     text="+",  position=5, line=1
// NUM(13):  text="13", position=8, line=1
```

Position advances by the matched length after each token.
The snapshot in each lexeme captures the values *after* the token was consumed, not before.

## The LexerCtx Trait

`LexerCtx` is the base trait for all lexer contexts.
Any custom context must satisfy three rules:

1. **It must be a case class** -- `LexerCtx` has a `this: Product =>` self-type, and the auto-derivation machinery requires a `Product` instance. Regular classes do not work (yet?).
2. **It must include `var text: CharSequence = ""`** -- `LexerCtx` declares this field as abstract. The lexer sets it to the remaining input before each match. Forgetting it produces a compile error.
3. **All fields must have default values** -- The `Empty[T]` derivation macro reads default parameter values from the companion object to construct the initial context. If any parameter lacks a default, the macro fails at compile time.

Mutable state fields must be `var`, not `val` -- the lexer assigns to them directly.
Exception: a field of a mutable collection type (e.g., `scala.collection.mutable.Stack`) can be `val` because you mutate the collection itself, not the reference.

## Custom Context

To track additional state, define a case class extending `LexerCtx` with your extra `var` fields:

```scala sc:nocompile
import alpaca.*

case class StateCtx(
  var text: CharSequence = "",   // required
  var count: Int = 0,            // custom state
) extends LexerCtx

val Lexer = lexer[StateCtx]:
  case "inc" =>
    ctx.count += 1              // modify context state
    Token["inc"](ctx.count)     // capture current count as value
  case "check" =>
    Token["check"](ctx.count)   // read without modifying
  case " " => Token.Ignored

val (finalCtx, lexemes) = Lexer.tokenize("inc check inc inc check")
// finalCtx.count == 3
//
// lexemes.map(_.value) == List(1, 1, 2, 3, 3)
// Each lexeme captured the count at the moment of its match
```

The type parameter `lexer[StateCtx]` tells the macro which context type to use.
The final context state is returned as the `ctx` component of the named tuple from `tokenize()`.

## Accessing Context in Patterns

Inside a `lexer[Ctx]:` block, the name `ctx` is implicitly available and refers to the current context object.
You can read and write any `var` field on it:

```scala sc:nocompile
import alpaca.*

case class IndentCtx(
  var text: CharSequence = "",
  var indent: Int = 0,
  var depth: Int = 0,
) extends LexerCtx

val Lexer = lexer[IndentCtx]:
  case "\\t" =>
    ctx.indent += 1             // write: increment indentation counter
    Token.Ignored
  case "\\n" =>
    ctx.depth = ctx.indent      // read and write: save indent level at end of line
    ctx.indent = 0              // reset for next line
    Token.Ignored
  case word @ "[a-z]+" => Token["WORD"](word)
```

> **Note on guards:** Guards (`case "regex" if condition =>`) are not supported in lexer rules.
> Use the rule body to read context state and decide what to emit -- you cannot filter matches before they occur.

## Context Snapshots in Lexemes

Each `Lexeme` carries a `fields` map that captures all context fields at the moment of the match:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case id @ "[a-zA-Z]+" => Token["ID"](id)
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("hi there")
// lexemes(0).fields == Map("text" -> "hi",    "position" -> 3, "line" -> 1)
// lexemes(1).fields == Map("text" -> "there", "position" -> 9, "line" -> 1)
//
// Access fields by name via Selectable:
// lexemes(0).position  // 3  (Int, not Any -- type-safe via Selectable refinement)
// lexemes(0).line      // 1
// lexemes(0).text      // "hi"  (the matched string, not remaining input)
```

The type safety comes from `Selectable`: the `tokenize()` return type carries a structural refinement that encodes every context field and its type. The compiler resolves `lexemes(0).position` to `Int` at compile time — not by casting from `Any` at runtime. If you access a field that does not exist on the context type (e.g., `.indent` when the lexer uses `LexerCtx.Default`), the compiler reports a type error.

Two important details:

- **`text` is the matched string**, not the remaining input. Even though `LexerCtx.text` holds the remaining input during lexing, the snapshot replaces it with the actual matched characters for that token.
- **Snapshots are independent.** Each lexeme captures the context state at its own match time. Modifying the context after a match does not retroactively change earlier lexemes.

For custom contexts, all case class fields appear in the snapshot:

```scala sc:nocompile
import alpaca.*

case class MyCtx(
  var text: CharSequence = "",
  var count: Int = 0,
) extends LexerCtx

val Lexer = lexer[MyCtx]:
  case n @ "[0-9]+" =>
    ctx.count += 1
    Token["NUM"](n.toInt)
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("1 2 3")
// lexemes(0).fields == Map("text" -> "1", "count" -> 1)
// lexemes(1).fields == Map("text" -> "2", "count" -> 2)
// lexemes(2).fields == Map("text" -> "3", "count" -> 3)
//
// Each snapshot captures the count *after* that token incremented it
```

## Built-in Tracking Traits

Alpaca provides two stackable traits for common tracking needs:

**`PositionTracking`** adds a `var position: Int` field and increments it by the matched length after each token.
On a newline match, position resets to 1 (start of next line).

**`LineTracking`** adds a `var line: Int` field and increments it when the matched token is a newline.

You can use these traits independently or together. `LexerCtx.Default` extends both.

## The BetweenStages Hook

After every successful token match, Alpaca runs the **BetweenStages** hook for the context type.
This hook is responsible for updating tracking fields and capturing the lexeme snapshot.

For custom context types, the hook is auto-derived: the macro inspects all parent traits of Ctx, summons their BetweenStages instances, and composes them into a single hook.

For a context extending PositionTracking and LineTracking:
  1. BetweenStages[LexerCtx]          -- updates text, records lexeme
  2. BetweenStages[PositionTracking]  -- updates position field
  3. BetweenStages[LineTracking]      -- updates line field
All three run automatically after every token match

Composability is automatic: extending `PositionTracking` and `LineTracking` gives you both hooks with no extra code.

> **Advanced:** If you define your own trait extending `LexerCtx` and provide a `given BetweenStages[MyTrait]`, the auto macro will compose it into any context that extends `MyTrait`. This pattern mirrors how `PositionTracking` and `LineTracking` work internally.

## LexerCtx.Empty

For cases where you need no tracking at all -- no position, no line counter, no custom fields -- Alpaca provides `LexerCtx.Empty`:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer[LexerCtx.Empty]:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("1 2 3")
// lexemes(0).fields == Map("text" -> "1")  -- only the text field, nothing else
```

See [Between Stages](between-stages.html) to learn how context snapshots embedded in lexemes flow into the parser.
