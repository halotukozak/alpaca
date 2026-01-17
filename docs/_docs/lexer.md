# Lexer

ALPACA provides a powerful, macro-based lexer generator that combines the ease of use of Scala's pattern matching with
the performance of a generated lexer.

## Defining a Lexer

To define a lexer, use the `lexer` macro. You specify rules using partial functions where the keys are regular
expression patterns and the values are token definitions.

```scala
import alpaca.*

val myLexer = lexer {
  case "\\s+" => Token.Ignored
  case "if" => Token["IF"]
  case id@"[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case num@"[0-9]+" => Token["NUM"](num.toInt)
}
```

### Pattern Matching Rules

| Pattern  | Example          | Description          |
|----------|------------------|----------------------|
| Literal  | `"if"`           | Exact string match   |
| Regex    | `"[0-9]+"`       | Character classes    |
| Captured | `num @ "[0-9]+"` | Extract matched text |
| Ignored  | `Token.Ignored`  | Skip token           |

- **Regular Expressions**: Patterns are standard Java/Scala regular expressions.
- **Priority**: Rules are checked in the order they are defined. The first matching pattern wins.
- **Named Groups**: You can use named groups in your regex if needed, although simple capturing is often enough.

## Token Types

### `Token["NAME"]`

Defines a token with a specific name. By default, tokens defined this way have a `Unit` value, which is common for
keywords and operators.

```scala
case "if" => Token["IF"] // value is ()
```

### `Token["NAME"](value)`

Defines a token that carries a value. You can pass the matched string or any expression derived from it.

```scala
case id@"[a-zA-Z]+" => Token["ID"](id) // value is String
case num@"[0-9]+" => Token["NUM"](num.toInt) // value is Int
```

If you pass the matched string variable directly, ALPACA optimizes this to use the already captured match.

### `Token.Ignored`

Specifies that the matched text should be skipped (e.g., whitespace or comments).

```scala
case "\\s+" => Token.Ignored
case "#.*" => Token.Ignored
```

### Literal Tokens

You can use the pattern itself as the token type if it's a fixed string. This is useful for defining many simple tokens
at once:

```scala
case keyword @("if" | "else" | "while") => Token[keyword.type]
```

In this case, the token name will be the matched string itself.

## Common Lexer Patterns

### Keywords vs Identifiers

Always match keywords **before** identifiers:

```scala
val MyLexer = lexer:
  case "if" => Token["IF"] // Specific
  case "else" => Token["ELSE"]
  case "while" => Token["WHILE"]
  case id@"[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id) // General
  case "\\s+" => Token.Ignored
```

### Numbers (Multiple Bases)

```scala
val LexerWithRadix = lexer:
  case hex@"0x[0-9a-fA-F]+" =>
    Token["NUM"](java.lang.Integer.parseInt(hex.drop(2), 16).toDouble)
  case bin@"0b[01]+" =>
    Token["NUM"](java.lang.Integer.parseInt(bin.drop(2), 2).toDouble)
  case dec@"[0-9]+" =>
    Token["NUM"](dec.toDouble)
```

### Comments

```scala
val LexerWithComments = lexer:
  case "//.*" => Token.Ignored // Line comment
  case "/\\*.*?\\*/" => Token.Ignored // Block comment
  case "\\s+" => Token.Ignored
// ... other rules
```

### String Literals

```scala
val StringLexer = lexer:
  case str@"\"[^\"]*\"" =>
    Token["STRING"](str.drop(1).dropRight(1)) // Remove quotes
  case str@"'[^']*'" =>
    Token["CHAR"](str.drop(1).dropRight(1))
```

## Context-Aware Lexing

Alpaca supports context-aware lexing, allowing you to maintain state during tokenization. Here's an example that tracks
brace matching:

```scala
import alpaca.*
import scala.collection.mutable

case class BraceContext(
  var text: CharSequence = "",
  braces: mutable.Stack[Char] = mutable.Stack()
) extends LexerCtx

val braceLexer = lexer[BraceContext]:
  case "\\(" =>
    ctx.braces.push('(')
    Token["LPAREN"]
  case "\\)" =>
    if ctx.braces.isEmpty || ctx.braces.pop() != '(' then
      throw RuntimeException("Mismatched parenthesis")
    Token["RPAREN"]
  case "\\{" =>
    ctx.braces.push('{')
    Token["LBRACE"]
  case "\\}" =>
    if ctx.braces.isEmpty || ctx.braces.pop() != '{' then
      throw RuntimeException("Mismatched brace")
    Token["RBRACE"]
  case "\\s+" => Token.Ignored
  case "[a-zA-Z]+" => Token["ID"]

// Usage
val input = "{ foo ( bar ) }"
val (finalCtx, lexemes) = braceLexer.tokenize(input)
if finalCtx.braces.nonEmpty then
  throw RuntimeException("Unclosed braces: " + finalCtx.braces.mkString)
```

The `ctx` variable is automatically available inside the `lexer` block and refers to the current state of the
tokenization process.

## Compile-Time Validation

ALPACA's lexer generator performs several checks at compile time to ensure your lexer is well-defined.

### Shadowing Detection

The `RegexChecker` detects if a rule can never be matched because a previous rule always matches the same input or its
prefix.

For example, if you define:

```scala
case "[a-z]+" => Token["ID"]
case "if" => Token["IF"] // This will NEVER match
```

ALPACA will report a compile error: `Pattern if is shadowed by [a-z]+`.

## Usage

To use the lexer, call the `tokenize` method:

```scala
val input = "if x then 10"
val (finalCtx, lexemes) = myLexer.tokenize(input)

lexemes.foreach { lexeme =>
  // Access basic properties
  println(s"Token: ${lexeme.name}, Value: ${lexeme.value}")

  // Access context-specific properties (e.g., from Default context)
  println(s"Line: ${lexeme.line}, Position: ${lexeme.position}")
}
```

### Lexer Contexts

ALPACA comes with a few built-in contexts:

- `LexerCtx.Default`: Tracks the current `line` and `position`. This is the default if no context is specified.
- `LexerCtx.Empty`: Minimal context that only tracks the remaining text.

You can specify the context when defining the lexer:

```scala
val myLexer = lexer[LexerCtx.Empty] {
...
}
```