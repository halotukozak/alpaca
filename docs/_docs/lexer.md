# Lexer

ALPACA provides a powerful, macro-based lexer generator that combines the ease of use of Scala's pattern matching with the performance of a generated lexer.

## Defining a Lexer

To define a lexer, use the `lexer` macro. You specify rules using partial functions where the keys are regular expression patterns and the values are token definitions.

```scala
import alpaca.*

val myLexer = lexer {
  case "\\s+" => Token.Ignored
  case "if" => Token["IF"]
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
}
```

### Pattern Matching Rules

- **Regular Expressions**: Patterns are standard Java/Scala regular expressions.
- **Priority**: Rules are checked in the order they are defined. The first matching pattern wins.
- **Named Groups**: You can use named groups in your regex if needed, although simple capturing is often enough.

## Token Types

### `Token["NAME"]`
Defines a token with a specific name. By default, tokens defined this way have a `Unit` value, which is common for keywords and operators.

```scala
case "if" => Token["IF"] // value is ()
```

### `Token["NAME"](value)`
Defines a token that carries a value. You can pass the matched string or any expression derived from it.

```scala
case id @ "[a-zA-Z]+" => Token["ID"](id)        // value is String
case num @ "[0-9]+" => Token["NUM"](num.toInt)  // value is Int
```

If you pass the matched string variable directly, ALPACA optimizes this to use the already captured match.

### `Token.Ignored`
Specifies that the matched text should be skipped (e.g., whitespace or comments).

```scala
case "\\s+" => Token.Ignored
case "#.*" => Token.Ignored
```

### Literal Tokens
You can use the pattern itself as the token type if it's a fixed string. This is useful for defining many simple tokens at once:

```scala
case keyword @ ("if" | "else" | "while") => Token[keyword.type]
```
In this case, the token name will be the matched string itself.

## Stateful Lexing

You can maintain state during tokenization by providing a custom `LexerCtx`.

```scala
case class MyCtx(var indent: Int = 0) extends LexerCtx

val myLexer = lexer[MyCtx] {
  case "  " => 
    ctx.indent += 1
    Token.Ignored
  case "\\n" =>
    ctx.indent = 0
    Token.Ignored
  // ... other rules
}
```

The `ctx` variable is automatically available inside the `lexer` block and refers to the current state of the tokenization process.

## Compile-Time Validation

ALPACA's lexer generator performs several checks at compile time to ensure your lexer is well-defined.

### Shadowing Detection
The `RegexChecker` detects if a rule can never be matched because a previous rule always matches the same input or its prefix.

For example, if you define:
```scala
case "[a-z]+" => Token["ID"]
case "if" => Token["IF"] // This will NEVER match
```
ALPACA will report a compile error: `Pattern <if> is shadowed by <[a-z]+>`.

### Prefix Coverage
It also detects if a rule might accidentally "consume" a prefix of another rule, leading to unexpected behavior.

```scala
case "<" => Token["LT"]
case "<=" => Token["LE"] // Shadowed by <
```
Error: `Pattern <= is shadowed by <`. To fix this, swap the order:
```scala
case "<=" => Token["LE"]
case "<" => Token["LT"]
```

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
val myLexer = lexer[LexerCtx.Empty] { ... }
```

## Performance

ALPACA's lexer uses a `LazyReader` internally to process input in chunks, making it suitable for large files without loading them entirely into memory. The generated lexer uses a single consolidated regular expression with named groups for efficient matching.
