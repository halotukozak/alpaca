# Lexer

The Alpaca lexer transforms raw text into a stream of structured tokens. You define lexical rules as regex patterns paired with token constructors, and the macro generates a tokenizer at compile time.

```scala sc:nocompile
import alpaca.*
```

<details>
<summary>Under the hood: compile-time processing</summary>

The `lexer` block is a Scala 3 macro. At compile time, it:

1. Validates every regex pattern
2. Checks for overlapping (shadowing) patterns using the [dregex](https://github.com/marianobarrios/dregex) library
3. Merges all patterns into a single combined regex with named capture groups
4. Generates the tokenization loop

At runtime, `tokenize()` executes the generated code. If a pattern is invalid or shadows another, you get a compile error, not a runtime surprise.

</details>

## Defining a Lexer

A lexer is defined with the `lexer` block. Each `case` branch maps a regex pattern to a token constructor. Patterns are tried in order; the first match wins.

```scala sc:nocompile sc-name:BrainLexer.scala
import alpaca.*

val BrainLexer = lexer:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "\\[" => Token["jumpForward"]
  case "\\]" => Token["jumpBack"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored
```

The result is a `Tokenization` object. It can tokenize input strings and provides typed accessors for each defined token (e.g., `BrainLexer.inc`).

## Regular Expressions

Patterns are Java regex strings, validated at compile time. Backslashes must be doubled inside Scala string literals: `"\\+"` matches a literal `+`, and `"\\d+"` matches one or more digits.

```scala sc:nocompile
import alpaca.*

// Literals that are regex metacharacters need escaping
case "\\+" => Token["inc"]         // literal +
case "\\." => Token["print"]       // literal .
case "\\[" => Token["jumpForward"] // literal [

// Non-metacharacters need no escaping
case ">" => Token["next"]          // literal >
case "-" => Token["dec"]           // literal -
case "," => Token["read"]          // literal ,

// Character classes and quantifiers
case "[0-9]+" => Token["NUM"]      // one or more digits
case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"] // identifier
case "\\s+" => Token.Ignored       // whitespace
case "\\r?\\n" => Token.Ignored    // newline (Unix or Windows)
```

An invalid regex (unmatched parentheses, bad quantifiers) produces a compile-time error. Two patterns that match the same input produce a compile-time shadowing error -- reorder or merge them to fix it.

## Tokens

Tokens come in three forms.

### Named Tokens

`Token["NAME"]` creates a token whose value is the matched text as a `String`. The token name becomes both the lexeme's `.name` field and the accessor on the lexer object.

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = BrainLexer.tokenize("> < +")
// lexemes: next, prev, inc
```

### Value-Bearing Tokens

`Token["NAME"](value)` attaches a computed value. Bind the matched text with `@` and transform it:

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case name @ "[A-Za-z]+" => Token["functionName"](name)
  case "!" => Token["functionCall"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = BrainLexer.tokenize("foo!")
// lexemes: functionName("foo"), functionCall
```

The type system tracks the value type: `BrainLexer.functionName` has type `Token["functionName", ..., String]`.

### Ignored Tokens

`Token.Ignored` matches text but excludes it from the token stream. Use it for whitespace, comments, and anything syntactically irrelevant.

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case "\\+" => Token["inc"]
  case "." => Token.Ignored   // any non-command character
  case "\n" => Token.Ignored  // newlines

val (_, lexemes) = BrainLexer.tokenize("+ hello +\n+")
// lexemes: inc, inc, inc (everything else is ignored)
```

## Variable Binding

The `@` syntax binds the matched text to a variable, giving you a `String` to transform before passing to the token constructor.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case name @ "[A-Za-z]+" => Token["functionName"](name)
  case "\\s+" => Token.Ignored
```

Without `@`, you cannot access the matched text. `Token["inc"]` without a binding creates a token whose value is the matched string (here `"+"`), but you cannot transform it.

## Token Naming Rules

### The Pipeline

1. You write `Token["NAME"]` or `Token[variable.type]` in the lexer definition
2. The string inside the type parameter becomes the **token name**
3. To access the token on the lexer object (e.g., in parser rules), Scala's standard name encoding applies
4. If the name is a Scala keyword or contains operator characters, use **backticks**: `` BrainLexer.`\\+` ``

### Dynamic Token Names

When several patterns share the same structure, use alternation with `variable.type` to create one token per alternative:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case keyword @ ("if" | "else" | "while") => Token[keyword.type]
  case op @ ("\\+" | "-" | "\\*") => Token[op.type]
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case "\\s+" => Token.Ignored

// Each alternative becomes a separate token:
// Lexer.`if`    : Token["if", ...]
// Lexer.`else`  : Token["else", ...]
// Lexer.`\\+`   : Token["\\+", ...]
// Lexer.-       : Token["-", ...]
```

Keywords like `if` always need backticks. `-` is a valid Scala identifier and does not.

## Tokenization

Call `tokenize()` on your lexer with an input string:

```scala sc:nocompile sc-compile-with:BrainLexer.scala
val (ctx, lexemes) = BrainLexer.tokenize("++[>+<-].")
```

The method returns a named tuple `(ctx: Ctx, lexemes: List[Lexeme])`:

- **`ctx`** -- the final lexer context after processing all input. With `LexerCtx.Default`, this includes `position` and `line`.
- **`lexemes`** -- matched tokens with `Token.Ignored` entries removed. Each `Lexeme` carries the token `name`, extracted `value`, and a snapshot of context fields at match time.

If the input contains a character that matches no pattern, `tokenize` throws a `RuntimeException`. See [Error Recovery](lexer-error-recovery.md) for alternatives.

## The Lexeme Structure

Every non-ignored match produces a `Lexeme`. From the caller's perspective, a lexeme exposes:

- **`name`** -- the token name as a string literal type (e.g., `"inc"`, `"functionName"`)
- **`value`** -- the extracted value (e.g., `String` for `Token["functionName"](name)`)

Each lexeme also carries a snapshot of all context fields at match time. The snapshot is accessed via `Selectable` -- you write `lexeme.position` or `lexeme.line` and the compiler resolves the types:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("42 13")
lexemes(0).position  // 3: Int (post-match position)
lexemes(0).line      // 1: Int
lexemes(0).text      // "42": String (the matched text, not remaining input)
```

<details>
<summary>Under the hood: how context snapshots work</summary>

The `Lexeme` class extends `Selectable` with a structural refinement that encodes every context field and its type. The compiler resolves `lexeme.position` to `Int` at compile time -- not by casting from `Any` at runtime. If you access a field that does not exist on the context type (e.g., `.indent` when using `LexerCtx.Default`), you get a compile error.

The `text` field in the snapshot is the **matched string**, not the remaining input. Even though `LexerCtx.text` holds the remaining input during lexing, the `BetweenStages` hook replaces it with `ctx.lastRawMatched` when building the snapshot.

The `position` value is the **post-match** cursor. The token `"42"` starts at column 1 but the snapshot records `position = 3` (1 + 2 characters consumed).

</details>

The parser appends `Lexeme.EOF` (name `"$"`, value `""`, empty fields) internally before running. You do not need to handle EOF in your lexer rules.

## Running Example: BrainLexer

The BrainFuck lexer introduced in [Getting Started](getting-started.md) tokenizes the eight BrainFuck commands. It uses `Token.Ignored` for everything else -- BrainFuck treats non-command characters as comments.

```scala sc:nocompile
import alpaca.*

val BrainLexer = lexer:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "\\[" => Token["jumpForward"]
  case "\\]" => Token["jumpBack"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored

val (_, lexemes) = BrainLexer.tokenize("++[>+<-].")
// lexemes.map(_.name) == List("inc", "inc", "jumpForward", "next", "inc", "prev", "dec", "jumpBack", "print")
```

Pattern order matters here: `"\\."` (literal dot -- the BF print command) must appear before `"."` (any character -- the catch-all). Otherwise the catch-all shadows the print command and you get a compile error.

Later pages extend this lexer with [custom context](lexer-context.md) (bracket counting), [error recovery](lexer-error-recovery.md), and value-bearing tokens for function names.

See [Debug Settings](debug-settings.md) for compile-time debug output, log levels, and timeout configuration.
