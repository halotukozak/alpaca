# Lexer

The Alpaca lexer transforms input text into a sequence of tokens using regex patterns. It's built on Scala 3 macros for compile-time validation and type safety.

## Basic Usage

Define a lexer with pattern matching syntax:

```scala
import alpaca.*

val MyLexer = lexer {
  case "[0-9]+" => Token["NUMBER"]
  case "[a-zA-Z]+" => Token["IDENTIFIER"]
  case "\\s+" => Token.Ignored
}

// Tokenize input
val (ctx, lexemes) = MyLexer.tokenize("hello 123")
```

## Token Types

### Simple Tokens

Match patterns without capturing values:

```scala
val Lexer = lexer {
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["MULTIPLY"]
}
```

### Tokens with Values

Capture and transform matched text:

```scala
val Lexer = lexer {
  case num @ "[0-9]+" => Token["NUMBER"](num.toInt)
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
  case str @ "\"[^\"]*\"" => Token["STRING"](str.slice(1, str.length - 1))
}
```

### Ignored Tokens

Skip whitespace and comments:

```scala
val Lexer = lexer {
  case "\\s+" => Token.Ignored
  case "#.*" => Token.Ignored
  case "//.*" => Token.Ignored
}
```

### Pattern Alternatives

Use pattern alternatives for keywords:

```scala
val Lexer = lexer {
  case keyword @ ("if" | "else" | "while" | "for") => Token[keyword.type]
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
}
```

## Tokenization

The `tokenize` method returns a tuple of the final context and lexemes:

```scala
val (finalCtx, lexemes) = MyLexer.tokenize("42 + 13")
// lexemes: List[Lexeme[?, ?]]
```

Each lexeme contains:
- `name` - Token name
- `value` - Extracted value
- `fields` - Metadata (text, position, line)

## Context Management

### Default Context

Tracks position and line numbers:

```scala
val Lexer = lexer {  // Uses LexerCtx.Default
  case num @ "[0-9]+" => Token["NUMBER"](num.toInt)
}

val (ctx, lexemes) = Lexer.tokenize("123")
// ctx.position: Int
// ctx.line: Int
```

### Custom Context

Add stateful behavior:

```scala
case class CounterCtx(
  var text: CharSequence = "",
  var count: Int = 0
) extends LexerCtx

val Lexer = lexer[CounterCtx] {
  case "inc" =>
    ctx.count += 1
    Token["INC"](ctx.count)
  case "reset" =>
    ctx.count = 0
    Token["RESET"]
}

val (finalCtx, lexemes) = Lexer.tokenize("inc inc reset inc")
// finalCtx.count: Int = 1
```

### Empty Context

Minimal context without tracking:

```scala
import alpaca.LexerCtx

val Lexer = lexer[LexerCtx.Empty] {
  case "[a-zA-Z]+" => Token["WORD"]
}
```

## Pattern Matching

### Accessing Tokens

Access tokens by field name:

```scala
val Lexer = lexer {
  case "\\+" => Token["PLUS"]
  case num @ "[0-9]+" => Token["NUMBER"](num.toInt)
}

// Access tokens
Lexer.PLUS     // Token["PLUS", ...]
Lexer.NUMBER   // Token["NUMBER", ...]
```

### Special Characters

Escape regex metacharacters:

```scala
val Lexer = lexer {
  case "\\+" => Token["PLUS"]      // Literal +
  case "\\*" => Token["STAR"]      // Literal *
  case "\\(" => Token["LPAREN"]    // Literal (
  case "\\)" => Token["RPAREN"]    // Literal )
  case "\\[" => Token["LBRACKET"]  // Literal [
  case "\\]" => Token["RBRACKET"]  // Literal ]
}
```

### Numbers

```scala
// Integers
case num @ "[0-9]+" => Token["INT"](num.toInt)

// Floats
case num @ "\\d+\\.\\d+" => Token["FLOAT"](num.toDouble)

// Scientific notation
case num @ "(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?" => Token["NUMBER"](num.toDouble)
```

### Identifiers

```scala
// Simple identifiers
case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)

// Camel case
case id @ "[a-z][a-zA-Z0-9]*" => Token["ID"](id)
```

### Strings

```scala
// Double-quoted strings
case str @ "\"[^\"]*\"" => Token["STRING"](str)

// Single-quoted strings
case str @ "'[^']*'" => Token["STRING"](str)
```

### Operators

```scala
// Multi-character operators first
case "<=" => Token["LEQ"]
case ">=" => Token["GEQ"]
case "==" => Token["EQ"]
case "!=" => Token["NEQ"]

// Single-character operators last
case "<" => Token["LT"]
case ">" => Token["GT"]
case "=" => Token["ASSIGN"]
```

### Comments

```scala
// Line comments
case "#.*" => Token.Ignored
case "//.*" => Token.Ignored

// Block comments (single line)
case "/\\*.*?\\*/" => Token.Ignored
```

## Error Handling

Unexpected characters throw a `RuntimeException`:

```scala
val Lexer = lexer {
  case "[0-9]+" => Token["NUMBER"]
}

// Throws: RuntimeException("Unexpected character: 'a'")
Lexer.tokenize("123abc")
```

## Compile-Time Validation

### Pattern Overlaps

Overlapping patterns are detected at compile time:

```scala
// Compile error: overlapping patterns
val Lexer = lexer {
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "[a-zA-Z]+" => Token["ALPHABETIC"]  // Error!
}
```

### Invalid Regex

Invalid patterns are caught at compile time:

```scala
// Compile error: invalid regex
val Lexer = lexer {
  case "[" => Token["BRACKET"]  // Error: unclosed character class
}
```

## Tips

1. **Order matters** - Define longer patterns before shorter ones:
   ```scala
   case "==" => Token["EQ"]    // Before "="
   case "=" => Token["ASSIGN"]
   ```

2. **Use named captures** - Bind matched text with `@`:
   ```scala
   case num @ "[0-9]+" => Token["NUMBER"](num.toInt)
   ```

3. **Escape metacharacters** - Use `\\` for regex special chars:
   ```scala
   case "\\+" => Token["PLUS"]  // Matches literal '+'
   ```

4. **Keep context mutable** - Only use `var` fields in custom contexts:
   ```scala
   case class MyCtx(
     var text: CharSequence = "",  // Required
     var count: Int = 0              // Custom mutable state
   ) extends LexerCtx
   ```
