# Guide: Lexer Error Handling

By default, Alpaca's lexer is strict: it throws a `RuntimeException` as soon as it encounters a character that doesn't match any of your defined token patterns.

However, for real-world applications, you often want the lexer to be more resilient, either by reporting multiple errors or by skipping invalid characters and continuing.
This guide explores strategies for implementing custom error handling in your lexer.

## 1. Default Behavior

When `tokenize` fails to find a match, it throws an exception:
```text
java.lang.RuntimeException: Unexpected character: '?'
```

## 2. Strategy: Catch-all Error Token

The most common strategy is to add a pattern at the **end** of your lexer that matches any single character.
This "catch-all" pattern will only be reached if no other token matches.

```scala
val myLexer = lexer:
  case "[0-9]+" => Token["NUM"]
  // ... other tokens ...
  case "\\s+" => Token.Ignored
  
  // Catch-all: matches any single character that wasn't matched above
  case "." => Token["ERROR"]
```

By emitting an `ERROR` token, the lexer can continue tokenizing the rest of the input.
Your parser can then decide how to handle these `ERROR` tokens.

## 3. Strategy: Error Counting in Context

You can use a custom `LexerCtx` to track the number of errors encountered during tokenization.

```scala
case class ErrorCtx(
  var text: CharSequence = "",
  var errorCount: Int = 0
) extends LexerCtx

val myLexer = lexer[ErrorCtx]:
  case "[a-z]+" => Token["ID"]
  case "\\s+" => Token.Ignored
  
  case x @ "." => 
    ctx.errorCount += 1
    println(s"Error: Unexpected character '$x' at position ${ctx.position}")
    Token.Ignored // Skip the character
```

## 4. Strategy: Error Recovery via Ignored Tokens

If you want to simply skip unexpected characters and proceed as if they weren't there, you can use `Token.Ignored` in your catch-all case.

```scala
val resilientLexer = lexer:
  case "[0-9]+" => Token["NUM"]
  case "\s+" => Token.Ignored
  
  // Log and ignore
  case x @ "." => 
    reportError(x)
    Token.Ignored
```

## 5. Implementation Considerations

### Precedence
Always place your catch-all pattern at the **very bottom** of your `lexer` block. Since Alpaca tries to match patterns in order (or uses the longest match with precedence), putting a `.` at the top would match everything and shadow your other rules.

### Performance
A catch-all `.` pattern can slightly impact performance if your input contains many invalid sequences, as it will be matched character-by-character. However, for most use cases, the overhead is negligible.

### Parser Integration
If your lexer produces `ERROR` tokens, your parser rules should be prepared to handle them, or you should filter them out before passing the lexeme list to `parser.parse()`.

```scala
val (finalCtx, lexemes) = myLexer.tokenize(input)
val validLexemes = lexemes.filter(_.name != "ERROR")
val result = myParser.parse(validLexemes)
```

## Summary

- **Default**: Fails fast with an exception.
- **Catch-all**: Use `case "."` to capture invalid characters.
- **Resilience**: Use `LexerCtx` to track and report errors without stopping.
- **Emitters**: Choose between emitting an `ERROR` token or using `Token.Ignored` to skip.
