# Error Messages

Alpaca surfaces errors at three distinct points -- compile time, lex time, and parse time -- each with different behavior and handling strategies.

> **Compile-time processing:** The `lexer` block is a Scala 3 macro. `ShadowException`, invalid regex patterns, and unsupported guards are all detected at compile time and reported as compiler errors, not runtime exceptions. These errors cannot be caught with `try`/`catch` -- they prevent compilation entirely.

## Compile-Time Errors

Compile-time errors are emitted by the Alpaca macro when it processes your `lexer` or `parser` definition. They appear as ordinary compiler errors in your IDE or build output. Because they occur at compile time, there is no way to handle them at runtime -- you must fix the definition and recompile.

### ShadowException

A `ShadowException` occurs when an earlier pattern always matches everything a later pattern would match, making the later pattern unreachable. The macro performs pairwise regex inclusion checks and fails compilation if any pattern is shadowed.

```scala sc:nocompile
import alpaca.*

// This does NOT compile -- ShadowException
val BadLexer = lexer:
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]  // general pattern
  case "[a-zA-Z]+"               => Token["ALPHABETIC"]   // ERROR: shadowed by IDENTIFIER

// Fix: more-specific patterns before more-general ones
val GoodLexer = lexer:
  case "if"                      => Token["IF"]           // keyword first
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]   // general pattern last
  case "\\s+" => Token.Ignored
```

The compile error reads: `Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*`. The fix is always the same: move the more specific pattern before the more general one.

### Guards Are Not Supported

Scala pattern guards (`case "regex" if condition =>`) are not supported in lexer rule definitions. Using one produces a compile-time error:

```scala sc:nocompile
import alpaca.*

// WRONG -- compile error: "Guards are not supported yet"
case class MyCtx(var text: CharSequence = "", var flag: Boolean = false) extends LexerCtx
val GuardedLexer = lexer[MyCtx]:
  case "token" if ctx.flag => Token["A"]

// Fix: move the condition inside the rule body
val CorrectLexer = lexer[MyCtx]:
  case "token" =>
    if ctx.flag then Token["A"] else Token["B"]
```

## Runtime Lexer Errors

If `tokenize()` encounters a character that does not match any pattern, it throws a `RuntimeException` immediately. There is no skip-and-continue behavior -- lexing stops at the first unrecognized character and the exception propagates to the caller.

```scala sc:nocompile
import alpaca.*

val NumLexer = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\s+" => Token.Ignored

try
  val (_, lexemes) = NumLexer.tokenize("42 abc")
catch
  case e: RuntimeException =>
    println(e.getMessage)  // "Unexpected character: 'a'"
```

The exception message contains the unexpected character but not its position in the input. For position information, use a context that tracks position -- see [Lexer Context](../lexer-context.html).

There is no custom error handler API yet ([GitHub issue #21](https://github.com/bkozak-scancode/alpaca/issues/21) is open).

## Parser Failure

`parse()` returns `T | Null`. A `null` result means the input token sequence did not match the grammar. This is not an exception -- it is a normal return value. Always check for `null` before using the result.

```scala sc:nocompile
import alpaca.*

val (_, lexemes) = CalcLexer.tokenize("1 + + 2")
val (_, result) = CalcParser.parse(lexemes)
if result == null then
  println("Parse failed: input did not match the grammar")
else
  println(s"Result: $result")
```

There is no structured parser error reporting yet -- `null` is the only signal that parsing failed ([GitHub issue #51](https://github.com/bkozak-scancode/alpaca/issues/51), [#65](https://github.com/bkozak-scancode/alpaca/issues/65) are open).

## See Also

- [Lexer Error Recovery](../lexer-error-recovery.html) -- full reference: `ShadowException`, runtime errors, pattern ordering
- [Lexer Context](../lexer-context.html) -- `PositionTracking` and `LineTracking` for position-aware error reporting
- [Parser](../parser.html) -- `parse()` return type, `T | Null` contract
