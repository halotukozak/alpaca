# Lexer

The Alpaca lexer transforms raw text into a stream of structured tokens using a pattern-matching DSL.
You define lexical rules as regex patterns paired with token constructors, and the library generates a tokenizer that processes input strings into typed token sequences.

Everything you need is available through a single import:

```scala sc:nocompile
import alpaca.*
```

> **Compile-time processing:** The `lexer` block is a Scala 3 macro. When you write a `lexer` definition, the compiler validates your regex patterns, checks for overlapping (shadowing) patterns, and generates the tokenization code -- all at compile time. At runtime, calling `tokenize()` simply executes the generated code. If a pattern is invalid or shadows another, you get a compile error, not a runtime surprise.

## Defining a Lexer

A lexer is defined with the `lexer` block. Each `case` branch maps a regex pattern to a token constructor.
Patterns are tried in order; the first match wins.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"]
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\s+" => Token.Ignored
```

The result is a `Tokenization` object that can tokenize input strings and provides typed accessors for each defined token.

## Regular Expressions

Patterns are Java regex strings, validated at compile time.
Because patterns live inside Scala string literals, backslashes must be doubled: `"\\+"` matches a literal `+`, and `"\\d+"` matches one or more digits.

Common patterns:

```scala sc:nocompile
// Digits
case num @ "[0-9]+" => Token["NUM"](num.toInt)

// Identifiers (letter followed by alphanumerics)
case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)

// Operators (escape regex metacharacters)
case "\\+" => Token["PLUS"]          // literal +
case "\\*" => Token["STAR"]          // literal *
case "\\(" => Token["LPAREN"]        // literal (
case "<=" => Token["LE"]             // no escaping needed

// Multi-line: \n, \r\n
case "\\r?\\n" => Token.Ignored
```

An invalid regex (unmatched parentheses, bad quantifiers) produces a compile-time error.
Two patterns that match the same input produce a compile-time shadowing error -- reorder or merge them to fix it.

## Tokens

Tokens come in three forms.

### Named Tokens

`Token["NAME"]` creates a token with no extracted value. The matched text is still captured internally, but no custom value is attached to the resulting lexeme.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case "if" => Token["IF"]
  case "else" => Token["ELSE"]
  case "while" => Token["WHILE"]
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("if x else y")
// lexemes: IF, ID, ELSE, ID
```

### Value-Bearing Tokens

`Token["NAME"](value)` attaches a value extracted from the matched text. The value can be any Scala expression that uses the bound variable.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case n @ "[0-9]+" => Token["INT"](n.toInt)
  case s @ "\"[^\"]*\"" => Token["STR"](s.substring(1, s.length - 1))
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("""42 "hello" 7""")
// lexemes: INT(42), STR("hello"), INT(7)
```

The type system tracks the value type: `WithValues.INT` has type `Token["INT", LexerCtx.Default, Int]`, and `WithValues.STR` has type `Token["STR", LexerCtx.Default, String]`.

### Ignored Tokens

`Token.Ignored` matches text but excludes it from the token stream. Use it for whitespace, comments, and other syntactically irrelevant input.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case "[0-9]+" => Token["NUM"]
  case "#.*" => Token.Ignored         // line comments
  case "\\s+" => Token.Ignored        // whitespace

val (_, lexemes) = Lexer.tokenize("42 # a comment\n7")
// lexemes: NUM, NUM  (comment and whitespace are gone)
```

## Variable Binding

The `@` syntax binds the matched text to a variable, giving you a `String` you can transform before passing to the token constructor.

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"](id)
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("count 42")
// lexemes: ID("count"), NUM(42)
```

Without `@`, you cannot access the matched text. A case branch like `case "[0-9]+" => Token["NUM"]` creates a token with no extracted value -- the match succeeds but the matched digits are not available for transformation.

## Token Naming Rules

This section explains the complete pipeline from token creation to Scala accessor.

### The Pipeline

1. You write `Token["NAME"]` or `Token[variable.type]` in the lexer definition
2. The string inside the type parameter becomes the **token name**
3. To access the token on the lexer object (e.g., in parser rules), Scala's standard name encoding applies
4. If the resulting name is not a valid Scala identifier (a keyword, or contains operator characters), you access it with **backticks**

### Dynamic Token Names

When several patterns share the same token structure, you can use alternation with `variable.type` to create one token per alternative:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case keyword @ ("if" | "else" | "while") => Token[keyword.type]
  case op @ ("\\+" | "-" | "\\*") => Token[op.type]
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case "\\s+" => Token.Ignored

// Each alternative becomes a separate token:
// Lang.`if`    : Token["if", LexerCtx.Default, Unit]
// Lang.`else`  : Token["else", LexerCtx.Default, Unit]
// Lang.`while` : Token["while", LexerCtx.Default, Unit]
// Lang.`\\+`   : Token["\\+", LexerCtx.Default, Unit]
// Lang.-       : Token["-", LexerCtx.Default, Unit]
// Lang.`\\*`   : Token["\\*", LexerCtx.Default, Unit]
// Lang.NUM     : Token["NUM", LexerCtx.Default, Int]
// Lang.ID      : Token["ID", LexerCtx.Default, String]
```

Note that `-` does not need backticks (it is a valid Scala identifier), while `\\+` and `\\*` do.
Keywords like `if`, `else`, `while` always need backticks.

## Tokenization

Call `tokenize()` on your lexer with an input string:

```scala sc:nocompile
val (ctx, lexemes) = Lexer.tokenize("x + 42")
```

The `tokenize` method returns a Scala 3 **named tuple** `(ctx: Ctx, lexemes: List[Lexeme])`:

- **`ctx`** -- the final lexer context state after processing all input. With the default context (`LexerCtx.Default`), this includes `position` (character offset) and `line` (line number).
- **`lexemes`** -- the list of matched tokens, with `Token.Ignored` entries already removed. Each `Lexeme` carries the token `name`, extracted `value`, and a snapshot of the context fields at the time of the match.

You can also destructure with field names:

```scala sc:nocompile
val result = Lexer.tokenize("x + 42")
val context = result.ctx
val tokens = result.lexemes
```

If the input contains a character that does not match any pattern, `tokenize` throws a `RuntimeException` with a message like `Unexpected character: '!'`.

## Running Example: CalcLexer

The following lexer tokenizes arithmetic expressions. It appears throughout the documentation as a running example -- the [Between Stages](between-stages.html) page shows how its output feeds a parser, the [Parser](parser.html) page defines the grammar, and the [Extractors](extractors.html) page shows how to access values from the parsed result.

```scala sc:nocompile
import alpaca.*

val CalcLexer = lexer:
  case num @ "[0-9]+(\\.[0-9]+)?" => Token["NUMBER"](num.toDouble)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["TIMES"]
  case "/" => Token["DIVIDE"]
  case "\\(" => Token["LPAREN"]
  case "\\)" => Token["RPAREN"]
  case "\\s+" => Token.Ignored
```

`CalcLexer` defines seven token types: `NUMBER` (with a `Double` value), four arithmetic operators (`PLUS`, `MINUS`, `TIMES`, `DIVIDE`), and two parentheses (`LPAREN`, `RPAREN`). Whitespace is ignored.

```scala sc:nocompile
val (_, lexemes) = CalcLexer.tokenize("3 + 4 * 2")
// lexemes: NUMBER(3.0), PLUS, NUMBER(4.0), TIMES, NUMBER(2.0)
```

This lexer uses the techniques covered above: regex patterns for digits and operators, variable binding (`num @`) to extract the matched text, `Token.Ignored` for whitespace, and value-bearing tokens (`Token["NUMBER"](num.toDouble)`). The next pages build a parser for these tokens.

## Lexer Context

By default, the `lexer` block uses `LexerCtx.Default`, which tracks `position` (1-based character offset) and `line` (1-based line number) as it processes the input. These values are available in the `ctx` returned by `tokenize()` and are captured in each lexeme's context snapshot.

You can define a custom context for stateful lexing (tracking indentation, nesting depth, etc.) by providing a type parameter:

```scala sc:nocompile
import alpaca.*

case class IndentCtx(
  var text: CharSequence = "",
  var indent: Int = 0,
) extends LexerCtx

val Lexer = lexer[IndentCtx]:
  case "\\t" =>
    ctx.indent += 1
    Token.Ignored
  case "\\n" =>
    ctx.indent = 0
    Token.Ignored
  case id @ "[a-z]+" => Token["ID"](id)
```


## The Lexeme Structure

A `Lexeme` is the data record that crosses the lexer-to-parser boundary.
Every matched and non-ignored token produces one lexeme; `Token.Ignored` tokens produce none.

From the caller's perspective, a `Lexeme` exposes two primary elements:

- **`name`** -- the token name as a string literal type (e.g., `"NUM"`, `"PLUS"`), as declared in the `Token["NAME"]` constructor.
- **`value`** -- the extracted value produced by the rule body (e.g., `Int` for `Token["NUM"](num.toInt)`, `Unit` for keyword tokens produced by `Token["KW"]`).

In addition, each lexeme carries a snapshot of all context fields at the time the token was matched. Rather than exposing this snapshot as a raw `Map[String, Any]`, `Lexeme` extends `Selectable`, which lets you access those fields by name with precise types through the tokenization result type.
The type refinement is part of the `tokenize()` return type -- it encodes the exact set of context fields and their types, enabling compile-time field access such as `lexeme.position`, `lexeme.line`, or `lexeme.text`.

Here is a minimal lexer that produces three lexemes from `"42 + 13"`:

```scala sc:nocompile
import alpaca.*

val Lexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = Lexer.tokenize("42 + 13")
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

The parser appends `Lexeme.EOF` (name `"$"`, value `""`, empty fields) internally before running. The `tokenize()` call itself does not include it.
You do not need to handle EOF in your lexer rules -- the parser manages it automatically.

text fields after a match does not retroactively alter earlier lexemes.

See [Lexer Context](lexer-context.html) for full details on custom contexts, and [Between Stages](between-stages.html) for how tokenized output flows into the parser.
