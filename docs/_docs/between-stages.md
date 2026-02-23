# Between Stages

The Alpaca lexer and parser are two independent compilation stages connected by a single data contract: the `Lexeme`.
When you call `tokenize()`, the lexer matches tokens against the input, runs the `BetweenStages` hook after each match, and collects the results into a `List[Lexeme]`.
When you call `parse()`, the parser consumes that list.
The `BetweenStages` hook is responsible for advancing the text cursor, constructing each lexeme with its context snapshot, and applying any custom side effects you configure.

Understanding this boundary helps you connect the two stages correctly and, when needed, customize what happens between them.

Most Alpaca programs only need one thing from this layer: pass `tokenize().lexemes` to `parse()`.
The rest of this page explains what is inside those lexemes, how the context state is embedded in each one,
and how to extend the pipeline when the defaults are not enough.

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
val (ctx, lexemes) = Lexer.tokenize("3 + 4 * 2")

// Pass the lexeme list to the parser
val result = Parser.parse(lexemes)
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
valLexer = lexer[MyCtx]:
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
