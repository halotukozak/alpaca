# Lexer Quickstart Guide

This guide provides practical, copy-paste-ready examples for building lexers with Alpaca. Start here if you want to get up and running quickly.

## Table of Contents

1. [Hello World: A Simple Lexer](#hello-world-a-simple-lexer)
2. [Math Expression Lexer](#math-expression-lexer)
3. [Programming Language Lexer](#programming-language-lexer)
4. [Stateful Lexer: Tracking Nesting](#stateful-lexer-tracking-nesting)
5. [Common Patterns](#common-patterns)

---

## Hello World: A Simple Lexer

The simplest lexer recognizes a few basic patterns:

```scala
import alpaca.*

val SimpleLexer = lexer {
  case "[0-9]+"         => Token["number"]
  case "[a-z]+"         => Token["word"]
  case "\\s+"           => Token.Ignored
}

// Usage
@main def main(): Unit =
  val (_, lexemes) = SimpleLexer.tokenize("hello 123 world")
  
  lexemes.foreach { lexeme =>
    println(s"${lexeme.name}: ${lexeme.value}")
  }
  
  // Output:
  // word: hello
  // number: 123
  // word: world
```

### Key Takeaways

- `case "[0-9]+" => Token["number"]` defines a token matching one or more digits
- `case "\\s+" => Token.Ignored` matches whitespace but produces no output
- `tokenize(input: String)` returns `(context, List[Lexeme])`
- Ignore the context (`_`) if you don't need it

---

## Math Expression Lexer

Lexers for mathematical expressions often need to parse numbers (including floats) and operators:

```scala
import alpaca.*

val MathLexer = lexer {
  // Comments
  case "#.*" => Token.Ignored
  
  // Multi-character operators (must come before single-char)
  case "\\*\\*" => Token["power"]
  case "//" => Token["intDiv"]
  case "<=" => Token["lessEqual"]
  case ">=" => Token["greaterEqual"]
  case "==" => Token["equal"]
  case "!=" => Token["notEqual"]
  
  // Numbers (integer and floating-point)
  case x @ "[0-9]+\\.[0-9]+" => Token["float"](x.toDouble)
  case x @ "[0-9]+" => Token["int"](x.toLong)
  
  // Single-character operators
  case "+" => Token["plus"]
  case "-" => Token["minus"]
  case "\\*" => Token["mul"]
  case "/" => Token["div"]
  case "%" => Token["mod"]
  case "=" => Token["assign"]
  case "<" => Token["less"]
  case ">" => Token["greater"]
  
  // Grouping
  case "\\(" => Token["lparen"]
  case "\\)" => Token["rparen"]
  case "\\{" => Token["lbrace"]
  case "\\}" => Token["rbrace"]
  
  // Identifiers (variables, function names)
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
  
  // Whitespace
  case "\\s+" => Token.Ignored
}

// Usage
@main def mathDemo(): Unit =
  val code = "x = 3.14 + 2 * (y - 1)"
  val (_, lexemes) = MathLexer.tokenize(code)
  
  println("Tokens:")
  lexemes.foreach { lexeme =>
    val valueStr = lexeme.value match
      case d: Double => s": $d"
      case l: Long => s": $l"
      case s: String => s": $s"
      case _ => ""
    println(s"  ${lexeme.name}$valueStr")
  }
```

### Key Takeaways

- **Order matters**: `\\*\\*` must come before `\\*` to avoid partial matches
- **Capture groups**: Use `case x @ "pattern" =>` to bind the matched text
- **Type extraction**: `Token["int"](x.toLong)` produces a `Long` value
- **Comments**: Comments often use `Token.Ignored` since they don't contribute to parsing

---

## Programming Language Lexer

Here's a more complete lexer suitable for a simple programming language:

```scala
import alpaca.*

val ProgramLexer = lexer {
  // Skip comments
  case "//.*$" => Token.Ignored
  case "/\\*.*?\\*/" => Token.Ignored  // block comment (be careful with greediness!)
  
  // String and character literals
  case x @ "\"[^\"]*\"" => Token["string"](x.drop(1).dropRight(1))  // remove quotes
  case x @ "'[^']*'" => Token["char"](x.drop(1).dropRight(1))
  
  // Keywords (must come before identifiers)
  case "if" => Token["if"]
  case "else" => Token["else"]
  case "while" => Token["while"]
  case "for" => Token["for"]
  case "return" => Token["return"]
  case "break" => Token["break"]
  case "continue" => Token["continue"]
  case "fn" => Token["fn"]
  case "var" => Token["var"]
  case "val" => Token["val"]
  case "true" => Token["true"]
  case "false" => Token["false"]
  case "null" => Token["null"]
  
  // Numbers
  case x @ "0x[0-9a-fA-F]+" => Token["hexInt"](Integer.parseInt(x.substring(2), 16))
  case x @ "[0-9]+\\.[0-9]+([eE][+-]?[0-9]+)?" => Token["float"](x.toDouble)
  case x @ "[0-9]+" => Token["int"](x.toLong)
  
  // Operators (longer patterns first)
  case "->" => Token["arrow"]
  case "=>" => Token["fatArrow"]
  case "&&" => Token["and"]
  case "||" => Token["or"]
  case "==" => Token["equal"]
  case "!=" => Token["notEqual"]
  case "<=" => Token["lessEqual"]
  case ">=" => Token["greaterEqual"]
  case "<<" => Token["shiftLeft"]
  case ">>" => Token["shiftRight"]
  
  // Single-char operators
  case "+" => Token["plus"]
  case "-" => Token["minus"]
  case "\\*" => Token["mul"]
  case "/" => Token["div"]
  case "%" => Token["mod"]
  case "&" => Token["bitwiseAnd"]
  case "|" => Token["bitwiseOr"]
  case "^" => Token["bitwiseXor"]
  case "!" => Token["not"]
  case "=" => Token["assign"]
  case "<" => Token["less"]
  case ">" => Token["greater"]
  case "." => Token["dot"]
  case "," => Token["comma"]
  case ";" => Token["semicolon"]
  case ":" => Token["colon"]
  case "\\?" => Token["question"]
  
  // Grouping
  case "\\(" => Token["lparen"]
  case "\\)" => Token["rparen"]
  case "\\[" => Token["lbracket"]
  case "\\]" => Token["rbracket"]
  case "\\{" => Token["lbrace"]
  case "\\}" => Token["rbrace"]
  
  // Identifiers and constructors
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
  
  // Whitespace
  case "\\s+" => Token.Ignored
}

// Usage
@main def programDemo(): Unit =
  val code = """
    var x = 10
    if (x > 5) {
      print("x is greater")
    }
  """.stripMargin
  
  val (ctx, lexemes) = ProgramLexer.tokenize(code)
  
  println(s"Total tokens: ${lexemes.size}")
  println("\nFirst 10 tokens:")
  lexemes.take(10).foreach { lexeme =>
    println(s"  ${lexeme.name}")
  }
```

### Key Takeaways

- **Keywords before identifiers**: Always define keywords like `"if"`, `"var"` before the generic identifier pattern
- **Multi-character operators before single-character**: `"->"` before `"-"`, `"&&"` before `"&"`
- **Escape special regex characters**: `\\(`, `\\[`, `\\{` for literal parentheses and brackets
- **String and character literals**: Remove quotes after capturing: `x.drop(1).dropRight(1)`

---

## Stateful Lexer: Tracking Nesting

Some lexers need to track state. This example tracks parenthesis nesting depth:

```scala
import alpaca.*

// Custom context that tracks nesting
case class NestingContext(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
  var parenDepth: Int = 0,
  var braceDepth: Int = 0,
  var bracketDepth: Int = 0,
  var errors: List[String] = Nil,
) extends LexerCtx
    with PositionTracking
    with LineTracking

val NestingLexer = lexer[NestingContext] {
  // Track opening brackets
  case "\\(" =>
    ctx.parenDepth += 1
    Token["lparen"]
  
  case "\\[" =>
    ctx.bracketDepth += 1
    Token["lbracket"]
  
  case "\\{" =>
    ctx.braceDepth += 1
    Token["lbrace"]
  
  // Track closing brackets
  case "\\)" =>
    ctx.parenDepth -= 1
    if ctx.parenDepth < 0 then
      ctx.errors :+= s"Unexpected ')' at line ${ctx.line}"
    Token["rparen"]
  
  case "\\]" =>
    ctx.bracketDepth -= 1
    if ctx.bracketDepth < 0 then
      ctx.errors :+= s"Unexpected ']' at line ${ctx.line}"
    Token["rbracket"]
  
  case "\\}" =>
    ctx.braceDepth -= 1
    if ctx.braceDepth < 0 then
      ctx.errors :+= s"Unexpected '}}' at line ${ctx.line}"
    Token["rbrace"]
  
  // Track newlines for line numbers
  case "\\n" =>
    ctx.line += 1
    ctx.position = 1
    Token.Ignored
  
  // Other tokens
  case x @ "[a-zA-Z_][\\w]*" => Token["id"](x)
  case x @ "[0-9]+" => Token["int"](x.toLong)
  
  // Whitespace
  case " " => Token.Ignored
  case "\\t" => Token.Ignored
}

// Usage
@main def nestingDemo(): Unit =
  val code = "f(x, [1, 2, {a: 3}])"
  val (finalCtx, lexemes) = NestingLexer.tokenize(code)
  
  println(s"Final nesting depths:")
  println(s"  Parentheses: ${finalCtx.parenDepth}")
  println(s"  Brackets: ${finalCtx.bracketDepth}")
  println(s"  Braces: ${finalCtx.braceDepth}")
  
  if finalCtx.parenDepth != 0 || finalCtx.bracketDepth != 0 || finalCtx.braceDepth != 0 then
    println("\nError: Unmatched brackets!")
  
  if finalCtx.errors.nonEmpty then
    println("\nLexing errors:")
    finalCtx.errors.foreach(println(_))
  else
    println("\nNo lexing errors.")
    println(s"Produced ${lexemes.size} tokens.")
```

### Key Takeaways

- **Custom context**: Extend `LexerCtx` and add custom fields (mutable with `var`)
- **Use context in rules**: Access `ctx` implicitly to read and modify state
- **Error accumulation**: Store errors in context for reporting after lexing
- **Preservation across tokens**: Context changes persist across all tokens in a single `tokenize()` call
- **Position tracking**: Manually update `line` and `position` as needed

---

## Common Patterns

### Pattern: Keyword Recognition

```scala
// Define keywords explicitly, then fall back to identifier
val Lexer = lexer {
  case "if" => Token["if"]
  case "else" => Token["else"]
  case "while" => Token["while"]
  // ... more keywords ...
  
  case x @ "[a-zA-Z_][\\w]*" => Token["id"](x)
}
```

**Why?** Because regex patterns are matched in order. If you had the identifier pattern first, `"if"` would match as `Token["id"]("if")` instead of `Token["if"]`.

### Pattern: Numeric Literals

```scala
val Lexer = lexer {
  // Floating-point must come before integer to avoid partial matches
  case x @ "[0-9]+\\.[0-9]+" => Token["float"](x.toDouble)
  case x @ "0x[0-9a-fA-F]+" => Token["hexInt"](Integer.parseInt(x.drop(2), 16))
  case x @ "0b[01]+" => Token["binInt"](Integer.parseInt(x.drop(2), 2))
  case x @ "[0-9]+" => Token["int"](x.toLong)
}
```

**Why?** Pattern specificity avoids ambiguity. Hexadecimal and binary numbers are more specific than plain integers.

### Pattern: String Literals

```scala
val Lexer = lexer {
  // Simple: no escape sequences
  case x @ "\"[^\"]*\"" => Token["string"](x.drop(1).dropRight(1))
  
  // Better: handle escaped quotes (careful with regex!)
  case x @ "\"(?:[^\"\\\\]|\\\\\\.)*\"" => 
    Token["string"](
      x.drop(1).dropRight(1)
        .replace("\\\\\"", "\"")
        .replace("\\\\\\\\", "\\\\")
    )
}
```

**Why?** String handling is surprisingly complex. The simple pattern works for most cases; the "better" version handles some escape sequences.

### Pattern: Comments

```scala
val Lexer = lexer {
  // Line comments
  case "//.*" => Token.Ignored
  
  // Block comments (be careful with nesting!)
  case "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/" => Token.Ignored
  
  // Other rules...
}
```

**Warning:** Nested block comments require stateful handling. For `/* /* nested */ */`, you need to track nesting depth in a custom context.

### Pattern: Whitespace

```scala
val Lexer = lexer {
  // Standard whitespace
  case "\\s+" => Token.Ignored
  
  // Or be more specific
  case " " => Token.Ignored
  case "\\t" => Token.Ignored
  case "\\n" => 
    ctx.line += 1
    ctx.position = 1
    Token.Ignored
}
```

**Why?** Sometimes you want to track newlines explicitly for error reporting.

### Pattern: Operators by Precedence

```scala
val Lexer = lexer {
  // Longest operators first
  case ">>>" => Token["unsignedShift"]
  case ">>" => Token["signedShift"]
  case "<<" => Token["leftShift"]
  
  // Then shorter variants
  case ">" => Token["greater"]
  case "<" => Token["less"]
  
  // Same for other multi-char operators
  case "&&" => Token["and"]
  case "||" => Token["or"]
  case "&" => Token["bitwiseAnd"]
  case "|" => Token["bitwiseOr"]
}
```

**Why?** First match wins. `>>>` must come before `>>`, which must come before `>`.

---

## Next Steps

1. **Test your lexer**: Write tests following the patterns in [Testing Lexers](./lexer-development.md#testing-lexers)
2. **Build a parser**: Use your lexer with [Parser Development Guide](./parser-development.md)
3. **Reference**: See [Lexer Development Guide](./lexer-development.md) for comprehensive coverage
4. **Examples**: Check the `example/` directory for real-world usage

---

## Tips and Tricks

### Debugging Lexer Issues

**Issue:** Token not recognized

```scala
// Check the token list
MyLexer.tokens.foreach { t =>
  println(s"${t.name}: ${t.pattern}")
}

// Verify pattern order
assert(MyLexer.tokens.map(_.pattern).indexOf("[0-9]+") < 
       MyLexer.tokens.map(_.pattern).indexOf("[a-zA-Z_][\\w]*"))
```

**Issue:** Wrong pattern matches first

```scala
// Reorder: more specific before general
val Lexer = lexer {
  case "[0-9]+\\.[0-9]+" => Token["float"]  // specific first
  case "[0-9]+" => Token["int"]              // general second
}
```

**Issue:** Type mismatch in value extraction

```scala
// Good
case x @ "[0-9]+" => Token["int"](x.toInt)  // String -> Int

// Wrong
case x @ "[0-9]+" => Token["int"](x)  // String != Int
```

### Performance Considerations

1. **Regex compilation**: Patterns are compiled at macro time, not runtime (free)
2. **Pattern matching**: Order matters; frequently-used patterns should be early
3. **Context copying**: Custom contexts are copied between tokens; keep fields minimal
4. **Large files**: For files with millions of tokens, consider streaming or batching

### Testing Interactively

```scala
// Use the REPL
scala> val Lexer = lexer { case "[0-9]+" => Token["int"] }
scala> Lexer.tokenize("123")
val res0: (LexerCtx.Default, List[Lexeme[?, ?]]) = ...

scala> res0._2.head.name
val res1: String = int
```
