# Lexer Error Recovery

The Alpaca lexer provides two layers of error feedback: compile-time validation that catches many problems before your program runs, and a runtime error for input that does not match any pattern. Understanding what can go wrong -- and what feedback you get -- helps you build reliable tokenizers and debug failures quickly.

> **Compile-time processing:** The `lexer` block is a Scala 3 macro that validates your token definitions at compile time. Pattern shadowing (`ShadowException`), invalid regex syntax, and unsupported guards are all caught during compilation. The macro performs pairwise regex inclusion checks to ensure every pattern is reachable. At runtime, only unmatched input characters can cause errors.

## Compile-Time Errors

The `lexer` block is a Scala 3 macro. Several classes of errors are caught at compile time, before any input is ever processed.

### ShadowException

A `ShadowException` occurs when the lexer macro detects that one pattern can never match because an earlier pattern always matches first. This is called *shadowing*: if every string that pattern B can match is also matched by pattern A, and A appears before B, then B is unreachable.

The following lexer definition does **not** compile:

```scala sc:nocompile
import alpaca.*

// This does NOT compile -- ShadowException
val Lexer = lexer:
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]  // general: letter/underscore, then alphanumerics
  case "[a-zA-Z]+"               => Token["ALPHABETIC"]   // ERROR: every string matching this also matches IDENTIFIER
```

The compile error reads:

```
Pattern [a-zA-Z]+ is shadowed by [a-zA-Z_][a-zA-Z0-9_]*
```

The fix is always the same: move the more specific pattern before the more general one or remove the duplicate.

A similar issue occurs with prefix shadowing. If the pattern `"i"` appears before `"if"`, then the string `"if"` will always be consumed character-by-character, and the keyword pattern `"if"` will never match:

```scala sc:nocompile
import alpaca.*

// This does NOT compile -- "if" is shadowed by "i"
val Lexer = lexer:
  case "i"  => Token["I"]
  case "if" => Token["IF"]   // ERROR: Pattern if is shadowed by i
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "\\s+" => Token.Ignored
```

### Invalid Regex

Malformed Java regex patterns -- unmatched parentheses, invalid quantifiers, bad character class syntax -- produce a compile-time error. The message identifies the invalid pattern and includes the underlying regex engine error. No special action is needed: fix the pattern string and recompile.

### Guards Not Supported

Scala pattern guards (`case "regex" if condition =>`) are not supported (yet?) in lexer rule definitions. Attempting to use one produces a compile-time error:

```scala sc:nocompile
import alpaca.*

// WRONG -- compile-time error: "Guards are not supported yet"
val Lexer = lexer:
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
val Lexer = lexer[MyCtx]:
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

## Limitations and Current State

The Alpaca lexer's error handling is intentionally minimal in the current release:

- **No custom error handling.** There is no API to register an error handler or recover from an unexpected character. Any unmatched input is a fatal error ([GitHub issue #21](https://github.com/halotukozak/alpaca/issues/21) tracks custom error recovery; it is open and not yet implemented).

- **No skip-and-continue behavior.** The lexer cannot skip the unexpected character and resume tokenizing. It stops at the first failure.

- **Guards are not supported.** As described above, pattern guards in lexer rules are a compile-time error. This is a known limitation of the current macro implementation.

- **Error position is inferred, not reported directly.** The `RuntimeException` message contains the unexpected character but not its position in the input. Position context must be reconstructed from the successfully matched lexemes and their snapshots.
