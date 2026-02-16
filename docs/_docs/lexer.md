# Lexer

The Lexer in Alpaca is responsible for transforming a raw string of characters into a stream of structured tokens.
It uses a powerful DSL based on Scala 3 macros to define lexical rules using regular expressions.

## Defining a Lexer

A lexer is defined using the `lexer` block.
Within this block, you use pattern matching where the patterns are regular expression strings and the results are token definitions.

```scala
import alpaca.*

val myLexer = lexer:
  case "[0-9]+" => Token["NUM"]
  case "[a-zA-Z]+" => Token["ID"]
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored
```

### Regular Expressions

Alpaca uses standard regular expression syntax. Patterns are validated at compile-time.

### Tokens

Tokens are created using the `Token` object:

- `Token["NAME"]`: Creates a token with the given name. By default, it captures the matched string.
- `Token["NAME"](extractor)`: Creates a token and applies a value extractor.
- `Token.Ignored`: Creates a token that is matched but excluded from the final token stream (useful for e.g., whitespace and comments).

## Token Value Extraction

You can extract values from matched patterns using variable binding or custom extractors.

### Variable Binding

```scala
val myLexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case id @ "[a-zA-Z]+" => Token["ID"](id)
```

### Custom Extractors

The value passed to `Token["NAME"](...)` can be any Scala expression that uses variables bound in the pattern.

## Lexer Context

The Lexer can maintain state during tokenization using a `LexerCtx`. This is useful for e.g., tracking line numbers, character positions, or handling nested structures.

### Using the Default Context

The `lexer` block uses `LexerCtx.Default` by default, which tracks `line` and `position`.

```scala
val myLexer = lexer:
  case "\n" => 
    ctx.line += 1
    ctx.position = 1
    Token.Ignored
  case "." => 
    ctx.position += 1
    Token["CHAR"]
```

### Defining a Custom Context

You can define your own context by extending `LexerCtx`.

```scala
case class MyCtx(
  var text: CharSequence = "",
  var indentLevel: Int = 0
) extends LexerCtx

val myLexer = lexer[MyCtx]:
  case "\t" => 
    ctx.indentLevel += 1
    Token.Ignored
  case "\n" => 
    ctx.indentLevel = 0
    Token.Ignored
  case "[a-z]+" => Token["ID"]
```

## Tokenization

To tokenize a string, call the `tokenize` method on your lexer:

```scala
val input = "foo 123"
val (finalCtx, lexemes) = myLexer.tokenize(input)
```

The `tokenize` method returns a tuple containing the final state of the context and a list of `Lexeme` objects.
Each `Lexeme` contains the token name, the extracted value, and the context state at the time of matching.

## Internal Working

At compile-time, the `lexer` macro:
1. Validates all regex patterns.
2. Checks for overlapping patterns (shadowing).
3. Generates an efficient scanner.
4. Produces type-safe `Lexeme` structures that carry context information.
