# Lexer Error Recovery

The Alpaca lexer provides two layers of error feedback: compile-time validation that catches many problems before your program runs, and runtime error strategies for input that does not match any pattern.

<details>
<summary>Under the hood: compile-time validation</summary>

The `lexer` macro validates token definitions at compile time. Pattern shadowing (`ShadowException`), invalid regex syntax, and unsupported guards are caught during compilation. The macro performs pairwise regex inclusion checks using the dregex library to ensure every pattern is reachable.

</details>

## Compile-Time Errors

### ShadowException

A `ShadowException` occurs when one pattern can never match because an earlier pattern always matches first. If every string that pattern B can match is also matched by pattern A, and A appears before B, then B is unreachable:

```scala sc:fail
import alpaca.*

// This does NOT compile -- ShadowException
val Lexer = lexer:
  case "[A-Za-z]+" => Token["ID"]            // general: any letters
  case "[A-Za-z][A-Za-z0-9]*" => Token["WORD"]  // ERROR: shadowed by ID
```

The fix: move the more specific pattern before the more general one, or remove the duplicate.

### Invalid Regex

Malformed Java regex patterns -- unmatched parentheses, invalid quantifiers, bad character class syntax -- produce a compile-time error. The message identifies the pattern and includes the underlying regex engine error.

### Guards Not Supported

Pattern guards (`case "regex" if condition =>`) are not supported in lexer rules. The workaround is to move the condition into the rule body:

```scala sc:nocompile
import alpaca.*

case class BrainLexContext(
  var squareBrackets: Int = 0,
) extends LexerCtx

val BrainLexer = lexer[BrainLexContext]:
  case "\\[" =>
    ctx.squareBrackets += 1
    Token["jumpForward"]
  case "\\]" =>
    // Validate inside the body, not as a guard
    require(ctx.squareBrackets > 0, "Mismatched brackets")
    ctx.squareBrackets -= 1
    Token["jumpBack"]
  case "\\+" => Token["inc"]
  case "." => Token.Ignored
```

## Pattern Ordering

Patterns are tried in the order they appear. The first match wins. The general rule: **more specific patterns before more general ones.**

In the BrainFuck lexer, this matters for the print command vs the catch-all:

```scala sc:nocompile
import alpaca.*

// RIGHT -- literal dot before catch-all dot
val BrainLexer = lexer:
  case "\\." => Token["print"]   // specific: literal dot (BF print)
  case "." => Token.Ignored      // general: any character (catch-all)
```

If you reverse the order, `"."` shadows `"\\."` and you get a `ShadowException`.

The same applies to keywords vs identifiers. Function names in the extended BrainFuck lexer must come after command tokens:

```scala sc:nocompile
import alpaca.*

// RIGHT -- single-char commands before the general name pattern
val BrainLexer = lexer:
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  // ... other single-char commands ...
  case name @ "[A-Za-z]+" => Token["functionName"](name)  // general: after commands
  case "." => Token.Ignored
```

## Runtime Error Handling

When `tokenize()` hits a character that matches no pattern, the lexer consults the `ErrorHandling` strategy for the context type.

### Default Behavior

The default strategy throws a `RuntimeException`:

```
Unexpected character at line 1, position 5: '@'
```

With `LexerCtx.Default`, the error message includes line and position. With `LexerCtx.Empty` or a custom context without tracking, it shows only the character.

### Error Handling Strategies

You can provide a custom `ErrorHandling` instance for your context type. Four strategies are available:

| Strategy | Behavior |
|----------|----------|
| `Throw(ex)` | Throw the given exception, aborting tokenization immediately |
| `IgnoreChar` | Skip the single unmatched character and continue |
| `IgnoreToken` | Skip to the next successful match and continue |
| `Stop` | Stop tokenization gracefully, returning lexemes collected so far |

```scala sc:nocompile
import alpaca.*
import alpaca.internal.lexer.ErrorHandling

case class BrainLexContext(
  var squareBrackets: Int = 0,
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx with PositionTracking with LineTracking

// Custom error handler: report position and throw
given ErrorHandling[BrainLexContext] = ctx =>
  ErrorHandling.Strategy.Throw:
    RuntimeException(s"Unexpected character at line ${ctx.line}, position ${ctx.position}: '${ctx.text.charAt(0)}'")
```

To silently skip unknown characters (useful for BrainFuck where non-command characters are comments):

```scala sc:nocompile
import alpaca.*
import alpaca.internal.lexer.ErrorHandling

// Skip unrecognized characters instead of throwing
given ErrorHandling[LexerCtx.Default] = _ =>
  ErrorHandling.Strategy.IgnoreChar
```

Note that the BrainFuck lexer from [Getting Started](getting-started.md) already handles this more explicitly with a `"." => Token.Ignored` catch-all pattern, which is the recommended approach when you want to ignore unknown input.

## Limitations

- **No skip-and-continue by default.** The default strategy aborts on the first unmatched character. Use a custom `ErrorHandling` or a catch-all pattern for resilience.
- **Guards are not supported.** Pattern guards in lexer rules are a compile-time error. Move conditions into rule bodies.
- **Error position is only available with tracking traits.** Without `PositionTracking` or `LineTracking`, the error message shows only the character, not its location.
