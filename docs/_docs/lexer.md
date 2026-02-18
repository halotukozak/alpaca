# Lexer

The Alpaca lexer transforms raw text into a stream of structured tokens using a pattern-matching DSL.
You define lexical rules as regex patterns paired with token constructors, and the library generates a tokenizer that processes input strings into typed token sequences.

Everything you need is available through a single import:

```scala sc:nocompile
import alpaca.*
```

> **Compile-time macro**
>
> The `lexer` block is a Scala 3 macro. When you write a `lexer` definition, the compiler validates your regex patterns,
> checks for overlapping (shadowing) patterns, and generates the tokenization code -- all at compile time. At runtime,
> calling `tokenize()` simply executes the generated code. If a pattern is invalid or shadows another, you get a compile
> error, not a runtime surprise.

## Defining a Lexer

A lexer is defined with the `lexer` block. Each `case` branch maps a regex pattern to a token constructor.
Patterns are tried in order; the first match wins.

```scala sc:nocompile
import alpaca.*

val SimpleLang = lexer:
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

val Keywords = lexer:
  case "if" => Token["IF"]
  case "else" => Token["ELSE"]
  case "while" => Token["WHILE"]
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = Keywords.tokenize("if x else y")
// lexemes: IF, ID, ELSE, ID
```

### Value-Bearing Tokens

`Token["NAME"](value)` attaches a value extracted from the matched text. The value can be any Scala expression that uses the bound variable.

```scala sc:nocompile
import alpaca.*

val WithValues = lexer:
  case n @ "[0-9]+" => Token["INT"](n.toInt)
  case s @ "\"[^\"]*\"" => Token["STR"](s.substring(1, s.length - 1))
  case "\\s+" => Token.Ignored

val (_, lexemes) = WithValues.tokenize("""42 "hello" 7""")
// lexemes: INT(42), STR("hello"), INT(7)
```

The type system tracks the value type: `WithValues.INT` has type `Token["INT", LexerCtx.Default, Int]`, and `WithValues.STR` has type `Token["STR", LexerCtx.Default, String]`.

### Ignored Tokens

`Token.Ignored` matches text but excludes it from the token stream. Use it for whitespace, comments, and other syntactically irrelevant input.

```scala sc:nocompile
import alpaca.*

val Skipping = lexer:
  case "[0-9]+" => Token["NUM"]
  case "#.*" => Token.Ignored         // line comments
  case "\\s+" => Token.Ignored        // whitespace

val (_, lexemes) = Skipping.tokenize("42 # a comment\n7")
// lexemes: NUM, NUM  (comment and whitespace are gone)
```

## Variable Binding

The `@` syntax binds the matched text to a variable, giving you a `String` you can transform before passing to the token constructor.

```scala sc:nocompile
import alpaca.*

val Binding = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"](id)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val (_, lexemes) = Binding.tokenize("count 42")
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

val Lang = lexer:
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
import alpaca.*

val MiniLang = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "[a-zA-Z][a-zA-Z0-9]*" => Token["ID"]
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val (ctx, lexemes) = MiniLang.tokenize("x + 42")
```

The `tokenize` method returns a Scala 3 **named tuple** `(ctx: Ctx, lexemes: List[Lexeme])`:

- **`ctx`** -- the final lexer context state after processing all input. With the default context (`LexerCtx.Default`), this includes `position` (character offset) and `line` (line number).
- **`lexemes`** -- the list of matched tokens, with `Token.Ignored` entries already removed. Each `Lexeme` carries the token `name`, extracted `value`, and a snapshot of the context fields at the time of the match.

You can also destructure with field names:

```scala sc:nocompile
val result = MiniLang.tokenize("x + 42")
val context = result.ctx
val tokens = result.lexemes
```

If the input contains a character that does not match any pattern, `tokenize` throws a `RuntimeException` with a message like `Unexpected character: '!'`.

## Lexer Context

By default, the `lexer` block uses `LexerCtx.Default`, which tracks `position` (1-based character offset) and `line` (1-based line number) as it processes the input. These values are available in the `ctx` returned by `tokenize()` and are captured in each lexeme's context snapshot.

You can define a custom context for stateful lexing (tracking indentation, nesting depth, etc.) by providing a type parameter:

```scala sc:nocompile
import alpaca.*

case class IndentCtx(
  var text: CharSequence = "",
  var indent: Int = 0,
) extends LexerCtx

val IndentLexer = lexer[IndentCtx]:
  case "\\t" =>
    ctx.indent += 1
    Token.Ignored
  case "\\n" =>
    ctx.indent = 0
    Token.Ignored
  case id @ "[a-z]+" => Token["ID"](id)
```

See [Lexer Context](lexer-context.html) for full details on custom contexts.
