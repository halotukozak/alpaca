# Between Stages

The Alpaca lexer and parser are two independent stages connected by a single data type: the `Lexeme`. Call `tokenize()` to get a list of lexemes, then pass that list to `parse()`.

Most programs need nothing more than this:

```scala sc:nocompile sc-compile-with:BrainLexer.scala,BrainParser.scala
val (ctx, lexemes) = BrainLexer.tokenize("[>+<-]")
val (_, ast) = BrainParser.parse(lexemes)
```

This page explains what is inside those lexemes and how the data flows between stages.

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

## Data Flow Summary

Each call to `tokenize()` follows this sequence:

1. The lexer matches the remaining input against each pattern in order. The first match wins. If no pattern matches, the `ErrorHandling` strategy runs (default: throw a `RuntimeException`).
2. The rule body executes (e.g., `ctx.squareBrackets += 1`).
3. The `BetweenStages` hook runs: it advances the text cursor, applies tracking updates (position, line), and takes a snapshot of all context fields.
4. If the token is a `DefinedToken` (any `Token["NAME"]`), a `Lexeme` is built from the name, value, and snapshot, then appended to the output list.
5. If the token is `Token.Ignored`, `BetweenStages` still runs (keeping position and line current) but no `Lexeme` is produced. The token is invisible to the parser.
6. This repeats until the input is consumed. `tokenize()` returns `(ctx, lexemes)`.
7. `parse(lexemes)` receives the list, appends `Lexeme.EOF`, and runs the grammar.

The lexeme list is immutable after `tokenize()` returns. The parser does not alter the lexeme data.

<details>
<summary>Under the hood: the BetweenStages hook</summary>

`BetweenStages[Ctx]` is a function `(Token[?, Ctx, ?], String, Ctx) => Unit` called after every successful match. The default implementation for `LexerCtx`:

1. Runs the token's context modification function (your rule body side effects)
2. Builds a `Lexeme` with `name`, `value`, and a `fields` map containing all context case class fields
3. Replaces the `text` field in the snapshot with the matched string (not the remaining input)

For contexts extending `PositionTracking` or `LineTracking`, additional hooks run automatically via compile-time composition (see [Lexer Context](lexer-context.md#the-betweenstages-hook)).

</details>

See [Lexer](lexer.md) for lexer definition, [Lexer Context](lexer-context.md) for custom contexts and tracking traits, and [Parser](parser.md) for grammar rules.
