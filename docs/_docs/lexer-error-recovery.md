# Lexer Error Recovery

The Alpaca lexer provides two layers of error feedback: compile-time validation that catches many problems before your program runs, and a runtime error for input that does not match any pattern. Understanding what can go wrong -- and what feedback you get -- helps you build reliable tokenizers and debug failures quickly.

## Compile-Time Errors

The `lexer` block is a Scala 3 macro. Several classes of errors are caught at compile time, before any input is ever processed.

### ShadowException

A `ShadowException` occurs when the lexer macro detects that one pattern can never match because an earlier pattern always matches first. This is called *shadowing*: if every string that pattern B can match is also matched by pattern A, and A appears before B, then B is unreachable.

The following lexer definition does **not** compile:

```scala sc:nocompile
import alpaca.*

// This does NOT compile -- ShadowException
val BadLexer = lexer:
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]  // general: letter/underscore, then alphanumerics
  case "[a-zA-Z]+"               => Token["ALPHABETIC"]   // ERROR: every string matching this also matches IDENTIFIER
```

The compile error reads:

```
Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*
```

**Reading the message:** The message names the shadowed pattern first (the later-defined one, `[a-zA-Z]+`) and the shadowing pattern second (the earlier one, `[a-zA-Z_][a-zA-Z0-9_]*`). This can feel backwards -- you might expect "A shadows B", but the message says "B is shadowed by A". The fix is always the same: move the more specific pattern before the more general one, or remove the duplicate.

A similar issue occurs with prefix shadowing. If the pattern `"i"` appears before `"if"`, then the string `"if"` will always be consumed character-by-character, and the keyword pattern `"if"` will never match:

```scala sc:nocompile
import alpaca.*

// This does NOT compile -- "if" is shadowed by "i"
val BadKeywords = lexer:
  case "i"  => Token["I"]
  case "if" => Token["IF"]   // ERROR: Pattern if is shadowed by i
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "\\s+" => Token.Ignored
```

### Invalid Regex

Malformed Java regex patterns -- unmatched parentheses, invalid quantifiers, bad character class syntax -- produce a compile-time error. The message identifies the invalid pattern and includes the underlying regex engine error. No special action is needed: fix the pattern string and recompile.

### Guards Not Supported

Scala pattern guards (`case "regex" if condition =>`) are not supported in lexer rule definitions. Attempting to use one produces a compile-time error:

```scala sc:nocompile
import alpaca.*

// WRONG -- compile-time error: "Guards are not supported yet"
val GuardedLexer = lexer:
  case "token" if ctx.someCondition => Token["A"]
```

The workaround is to move the condition inside the rule body, after the match:

```scala sc:nocompile
import alpaca.*

case class MyCtx(
  var text: CharSequence = "",
  var someCondition: Boolean = false,
) extends LexerCtx

// RIGHT -- check condition inside the rule body
val CorrectLexer = lexer[MyCtx]:
  case "token" =>
    if ctx.someCondition then Token["A"]
    else Token["B"]
```

This limitation is a known constraint of the current macro implementation.

## Pattern Ordering

Pattern ordering is the primary strategy for preventing shadowing errors and controlling which tokens match ambiguous input. Patterns are tried in the order they appear in the `lexer` block. The first matching pattern wins.

The general rule: **more specific patterns before more general ones.**

```scala sc:nocompile
import alpaca.*

// WRONG -- "if" and "when" are shadowed by the general IDENTIFIER pattern
val WrongOrder = lexer:
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]  // general pattern first -- shadows keywords
  case "if"   => Token["IF"]                             // ERROR: unreachable
  case "when" => Token["WHEN"]                           // ERROR: unreachable
  case "\\s+" => Token.Ignored
```

```scala sc:nocompile
import alpaca.*

// RIGHT -- keywords and short patterns before the general identifier pattern
val CorrectOrder = lexer:
  case "if"   => Token["IF"]                            // specific keyword first
  case "when" => Token["WHEN"]                          // specific keyword first
  case "i"    => Token["I"]                             // short single-character pattern
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"] // general pattern last
  case "\\s+" => Token.Ignored
```

The `CorrectOrder` lexer compiles and tokenizes as expected: the input `"if"` matches the `IF` token, not `IDENTIFIER`.

**Why the `lexer` macro enforces this:** The macro checks every pair of patterns using a regex inclusion test. If pattern A is listed before pattern B and A matches a superset of what B matches (or an identical set), B can never win and a `ShadowException` is thrown. Ordering your patterns correctly is both a correctness requirement and a guard against subtle tokenization bugs.

## Runtime Errors

Compile-time validation cannot catch all problems. If the lexer encounters a character in the input that does not match any pattern, it throws a `RuntimeException` immediately:

```scala sc:nocompile
import alpaca.*

val NumLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)

// Tokenizing "123abc": digits match, but 'a' does not match any pattern
// Throws: RuntimeException("Unexpected character: 'a'")
val (_, lexemes) = NumLexer.tokenize("123abc")
```

When `tokenize()` hits an unrecognized character, lexing stops immediately. There is no skip-and-continue behavior: the partial results are discarded and the exception propagates to the caller. The lexer does not attempt to recover.

The error message format is:

```
Unexpected character: 'X'
```

where `X` is the first character that did not match any pattern.

## Error Position Information

When a runtime error occurs, knowing *where* in the input the failure happened is essential for diagnosing the problem. The default lexer context (`LexerCtx.Default`) tracks `position` (1-based character offset) and `line` (1-based line number) as it processes input. These values advance with every matched token.

Because the `RuntimeException` is thrown at the point of failure, you can use the context returned from a *successful* partial tokenization or instrument your code to catch and inspect the failure point:

```scala sc:nocompile
import alpaca.*

val MiniLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\s+"            => Token.Ignored

try
  val (ctx, lexemes) = MiniLexer.tokenize("42 99 abc 7")
catch
  case e: RuntimeException =>
    // The exception message identifies the unexpected character.
    // The lexemes processed before the error represent successful matches.
    println(e.getMessage)  // "Unexpected character: 'a'"
```

Each successfully matched lexeme carries a snapshot of the context at the time of that match. If you need fine-grained position information for error reporting, inspect the last lexeme's context fields:

```scala sc:nocompile
import alpaca.*

val MiniLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\s+"            => Token.Ignored

// On a successful tokenization, the final context tells you the position after the last token
val (ctx, lexemes) = MiniLexer.tokenize("42 99")
// ctx.position == 6  (after "42 99": 2 digits + 1 space + 2 digits = 5 chars, position is 1-based so 6)
// ctx.line     == 1
```

See [Lexer](lexer.html) for the full description of context tracking and the named-tuple return value of `tokenize()`. For custom contexts that track additional position information, see [Lexer Context](lexer-context.html).

## Limitations and Current State

The Alpaca lexer's error handling is intentionally minimal in the current release:

- **No custom error handling.** There is no API to register an error handler or recover from an unexpected character. Any unmatched input is a fatal error ([GitHub issue #21](https://github.com/bkozak-scancode/alpaca/issues/21) tracks custom error recovery; it is open and not yet implemented).

- **No skip-and-continue behavior.** The lexer cannot skip the unexpected character and resume tokenizing. It stops at the first failure.

- **Guards are not supported.** As described above, pattern guards in lexer rules are a compile-time error. This is a known limitation of the current macro implementation.

- **Error position is inferred, not reported directly.** The `RuntimeException` message contains the unexpected character but not its position in the input. Position context must be reconstructed from the successfully matched lexemes and their snapshots.

These constraints reflect the current state of the library. The compile-time validation (ShadowException, invalid regex) provides strong guarantees before runtime, and pattern ordering eliminates the most common class of tokenization errors. Runtime error recovery is on the roadmap.
