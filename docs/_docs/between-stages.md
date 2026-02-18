# Between Stages

The Alpaca lexer and parser are two independent compilation stages connected by a single data contract: the `Lexeme`.
When you call `tokenize()`, the lexer matches tokens against the input, runs the `BetweenStages` hook after each match, and collects the results into a `List[Lexeme]`.
When you call `parse()`, the parser consumes that list.
The `BetweenStages` hook is responsible for advancing the text cursor, constructing each lexeme with its context snapshot, and applying any custom side effects you configure.

Understanding this boundary helps you connect the two stages correctly and, when needed, customize what happens between them.

Most Alpaca programs only need one thing from this layer: pass `tokenize().lexemes` to `parse()`.
The rest of this page explains what is inside those lexemes, how context state is embedded in each one,
and how to extend the pipeline when the defaults are not enough.

## The Lexeme Structure

A `Lexeme` is the data record that crosses the lexer-to-parser boundary.
Every matched and non-ignored token produces one lexeme; `Token.Ignored` tokens produce none.
A `Lexeme` has three user-visible elements:

- **`name`** -- the token name as a string literal type (e.g., `"NUM"`, `"PLUS"`), as declared in the `Token["NAME"]` constructor.
- **`value`** -- the extracted value produced by the rule body (e.g., `Int` for `Token["NUM"](num.toInt)`, `Unit` for keyword tokens produced by `Token["KW"]`).
- **`fields`** -- a `Map[String, Any]` containing a snapshot of all context fields at the time the token was matched.

`Lexeme` extends `Selectable`, which lets you access fields by name with precise types through the tokenization result type, rather than extracting `Any` from the map.
The type refinement is part of the `tokenize()` return type -- it encodes the exact set of context fields and their types, enabling compile-time field access.

Here is a minimal lexer that produces three lexemes from `"42 + 13"`:

```scala sc:nocompile
import alpaca.*

val MiniLang = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = MiniLang.tokenize("42 + 13")
// lexemes == List(
//   Lexeme("NUM",  42, Map("text" -> "42", "position" -> 3, "line" -> 1)),
//   Lexeme("PLUS", (), Map("text" -> "+",  "position" -> 5, "line" -> 1)),
//   Lexeme("NUM",  13, Map("text" -> "13", "position" -> 8, "line" -> 1)),
// )
```

Because the result type carries structural refinement, field access is type-safe:

```scala sc:nocompile
lexemes(0).position  // 3: Int  (not Any)
lexemes(0).line      // 1: Int
lexemes(0).text      // "42": String
```

Two details to keep in mind:

- **`lexeme.text` is the matched string, not the remaining input.** During lexing, `ctx.text` holds the remaining input at each step. The snapshot overrides the `text` field with `ctx.lastRawMatched` -- the characters the rule actually consumed. After matching `"42"` from `"42 + 13"`, the snapshot records `text = "42"`, not `"+ 13"`.
- **`lexeme.position` is the post-match position.** The cursor advances by the token length before the snapshot is taken. The token `"42"` starts at column 1 but the snapshot records `position = 3` (1 + 2 characters consumed).

The parser appends `Lexeme.EOF` (name `""`, value `""`, empty fields) internally before running. The `tokenize()` call itself does not include it.
You do not need to handle EOF in your lexer rules -- the parser manages it automatically.

## Context Snapshots in Lexemes

After each token match, `BetweenStages` captures a snapshot of **all** context fields and stores them in `lexeme.fields`.
The snapshot includes every field from the context's `Product` element names, with the `"text"` field overridden by the matched string (as described above).

For the default context (`LexerCtx.Default`), this means every lexeme carries `text`, `position`, and `line`.
For a custom context, every additional `var` field you declare also appears in the snapshot.

Here is a counter example showing how the snapshot captures the state *after* the rule body runs:

```scala sc:nocompile
import alpaca.*

case class StateCtx(
  var text: CharSequence = "",
  var count: Int = 0,
) extends LexerCtx

val CountingLexer = lexer[StateCtx]:
  case "inc" =>
    ctx.count += 1
    Token["inc"](ctx.count)
  case "check" =>
    Token["check"](ctx.count)
  case " " => Token.Ignored

val (_, lexemes) = CountingLexer.tokenize("inc check inc inc check")
// lexemes.map(_.value)  == List(1, 1, 2, 3, 3)
// lexemes(0).fields     == Map("text" -> "inc", "count" -> 1)
// lexemes(1).fields     == Map("text" -> "check", "count" -> 1)
// count captured AFTER the rule body ran (ctx.count += 1 happens before snapshot)
```

Note that `Token.Ignored` tokens (here `" "`) still trigger `BetweenStages` for context tracking -- position and line counters advance, and the cursor moves forward -- but they do **not** appear in the output list.
`Token.Ignored` is how you consume whitespace or comments without producing a lexeme.

Snapshots are also independent: each lexeme holds its own map, captured at its own match time.
Modifying context fields after a match does not retroactively alter earlier lexemes.

## Connecting Lexer Output to Parser Input

The `tokenize()` method returns a named tuple:

```
(ctx: Ctx, lexemes: List[Lexeme])
```

The `lexemes` component is the `List[Lexeme]` that the parser expects.
You can destructure or access it by name:

```scala sc:nocompile
import alpaca.*

// tokenize() returns a named tuple: (ctx: Ctx, lexemes: List[Lexeme])
val (ctx, lexemes) = CalcLexer.tokenize("3 + 4 * 2")

// Pass the lexeme list to the parser
val result = CalcParser.parse(lexemes)

// Or inline -- access the named tuple field directly:
CalcParser.parse(CalcLexer.tokenize("3 + 4 * 2").lexemes)
```

The parser accepts `List[Lexeme[?, ?]]` -- the type refinement from the tokenization result is widened at the `parse()` call site.
The parser appends `Lexeme.EOF` internally before processing begins.

The final context (`ctx`) is also useful after tokenization: it holds the accumulated state from all rule bodies (e.g., the final line count, the last recorded indentation level).
You can inspect it before or after calling `parse()` -- the parser does not modify it.

See the [Lexer](lexer.html) page for how to define a lexer and the token types it accepts.

## Custom BetweenStages

The default `BetweenStages[LexerCtx]` handles cursor advancement, snapshot construction, and context updates for all standard use cases.
Customize it only when you need per-token side effects beyond what context fields can express -- for example, writing to an external log, emitting metrics, or computing aggregate statistics that must update outside the context object.

The correct pattern mirrors how Alpaca's built-in `PositionTracking` and `LineTracking` traits work.
It requires a trait (not a case class) so that the auto-composition macro can discover your hook by inspecting the context's linearized parent types at compile time.
This means one `given` definition covers every case class that extends your trait -- you write the hook once and reuse it across contexts.

1. Define a custom **trait** extending `LexerCtx`.
2. Provide `given BetweenStages[YourTrait]` in that trait's **companion object**.
3. Have your case class extend the trait.
4. The `auto` macro at compile time finds all `BetweenStages` instances from parent traits and composes them automatically.

```scala sc:nocompile
import alpaca.*

// Step 1: Trait extending LexerCtx
trait IndentTracking extends LexerCtx:
  this: Product =>
  var indentLevel: Int

// Step 2: given in TRAIT COMPANION (not case class companion)
object IndentTracking:
  given BetweenStages[IndentTracking] = (token, matcher, ctx) =>
    if matcher.group(0) == "\n" then ctx.indentLevel = 0

// Step 3: Case class extends both
case class MyCtx(
  var text: CharSequence = "",
  var indentLevel: Int = 0,
) extends LexerCtx with IndentTracking

// Step 4: Pass MyCtx to lexer -- auto composition happens at compile time
val MyLexer = lexer[MyCtx]:
  case "\\n" => Token.Ignored
  case id @ "[a-z]+" => Token["ID"](id)
```

Do **not** define `given BetweenStages[MyCtx]` directly on the concrete case class.
Doing so bypasses the auto-composition mechanism: the lexer will use your hook but skip the default hook that advances the text cursor, and tokenization will loop indefinitely.
Always put the `given` in a **trait companion**, not a case class companion.

For reference, the `BetweenStages` type is a function:

```scala sc:nocompile
// BetweenStages[Ctx] extends ((Token[?, Ctx, ?], Matcher, Ctx) => Unit)
// token:   Token[?, Ctx, ?]  -- either DefinedToken or IgnoredToken
// matcher: java.util.regex.Matcher  -- the successful match for this token
// ctx:     Ctx  -- the current context (mutable, updated in place)
```

Note: The `BetweenStages` trait may be renamed in a future release (see [GitHub #235](https://github.com/alpaca-scala/alpaca/issues/235)).

See the [Lexer Context](lexer-context.html) page for the full reference on `BetweenStages` composition and the built-in `PositionTracking` and `LineTracking` traits.

## Data Flow Summary

The following sequence describes what happens each time `tokenize()` processes input:

1. The lexer attempts to match the remaining input against each rule pattern in order. The first match wins. If no pattern matches, a `RuntimeException` is thrown with the unexpected character.
2. `BetweenStages` runs: it advances the text cursor (`ctx.text`), applies any rule-body context changes that ran before it was called, and takes a snapshot of all context fields.
3. If the matched token is a `DefinedToken` (any `Token["NAME"]` with or without a value), a `Lexeme` is constructed with the token's name, value, and the snapshot, then appended to the output list.
4. If the matched token is `Token.Ignored`, `BetweenStages` still runs (keeping position, line, and custom counters current) but no `Lexeme` is produced. The token is invisible to the parser.
5. This repeats until the entire input is consumed. `tokenize()` then returns the named tuple `(ctx, lexemes)` -- the final context state and the complete lexeme list.
6. `parse(lexemes)` receives the list, appends `Lexeme.EOF` internally, and runs the parser grammar against the sequence.

The `Lexeme` list is immutable after `tokenize()` returns -- nothing that happens in `parse()` alters the lexeme data.
If you need to inspect what the lexer produced before parsing, iterate over `lexemes` freely; the parser will see exactly the same list.

See the [Parser](parser.html) page for how to define grammar rules, productions, and EBNF operators that consume the lexeme list.
