# Context & State Management

Alpaca provides a robust system for maintaining and updating state during both the lexing and parsing phases. This is achieved through "Context" objects (`LexerCtx` and `ParserCtx`) and the "Between Stages" hook.

## 1. Lexer Context (`LexerCtx`)

The `LexerCtx` maintains state during tokenization. It's used to track the remaining input, current position, and any custom metadata (like indentation level or brace depth).

### The Default Context
Alpaca provides `LexerCtx.Default`, which automatically tracks:
- `text`: The remaining input string.
- `line`: Current line number (1-based).
- `position`: Current character position (1-based).

```scala
val myLexer = lexer: // Uses LexerCtx.Default by default
  case "
" => 
    // The ctx is automatically updated for line and position
    Token.Ignored
```

### Custom Lexer Context
You can define a custom context by extending `LexerCtx` (and optionally `LineTracking` or `PositionTracking`).

```scala
case class MyCtx(
  var text: CharSequence = "",
  var braceDepth: Int = 0
) extends LexerCtx

val myLexer = lexer[MyCtx]:
  case "\(" => 
    ctx.braceDepth += 1
    Token["("]
```

## 2. Between Stages Hook

The `BetweenStages` hook is a function that Alpaca calls after **every** token match (even for `Token.Ignored`). Its primary purpose is to update the context before the next match occurs.

### How it Works
When a pattern matches, Alpaca:
1. Executes the rule action (e.g., creating a `Token`).
2. Calls the `BetweenStages` hook with the matched token, the regex matcher, and the current context.

### Automatic Derivation
Alpaca can automatically derive a `BetweenStages` instance for your context class. It combines the update logic from all parent traits.

For example, if your context extends `LineTracking`, the derived `BetweenStages` will include the logic to increment the line count when a newline character is matched.

## 3. Parser Context (`ParserCtx`)

The `ParserCtx` is used to maintain state during the parsing phase. It's typically used for:
- Building a symbol table.
- Tracking scope.
- Accumulating semantic errors.

### Defining a Parser Context
Extend the `ParserCtx` trait and provide it to your `Parser`.

```scala
case class MyParserCtx(
  var symbols: Set[String] = Set()
) extends ParserCtx

object MyParser extends Parser[MyParserCtx]:
  val root = rule:
    case MyLexer.ID(id) => 
      ctx.symbols += id.value
      id.value
```

## 4. Interaction between Lexer and Parser Contexts

The lexer and parser maintain **separate** contexts.
- `LexerCtx` is updated after each token match during `tokenize`.
- `ParserCtx` is updated during the `parse` phase as rules are reduced.

When you call `myParser.parse(lexemes)`, the lexemes themselves carry a "snapshot" of the lexer context as it was when the token was matched. This allows the parser to access position information (like line numbers) for error reporting, even though the lexer has finished its job.

```scala
// In the parser, accessing lexer context info:
case MyLexer.NUM(n) => 
  println(s"Matched number on line ${n.fields.line}")
  n.value
```

## Summary

- **`LexerCtx`**: Tracks state during tokenization.
- **`ParserCtx`**: Tracks state during parsing.
- **`BetweenStages`**: The engine that updates `LexerCtx` after each match.
- **Snapshots**: `LexerCtx` state is preserved in each `Lexeme` for the parser to use.
