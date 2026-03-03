# Whitespace-Sensitive Lexing

Use a custom `LexerCtx` to track indentation depth and emit `INDENT` or `DEDENT` tokens when the indentation level changes between lines.

> **Compile-time processing:** The `lexer[MyCtx]` macro inspects `MyCtx` at compile time; it auto-composes `BetweenStages` hooks from parent traits; the `ctx` value is available in rule bodies as a compile-time alias that is replaced by field accesses in the generated code.

## The LexerCtx Contract

A valid custom context must satisfy three rules:

1. It must be a **case class** -- `LexerCtx` has a `this: Product =>` self-type; the auto-derivation machinery requires a `Product` instance and regular classes do not satisfy it.
2. It must include **`var text: CharSequence = ""`** -- `LexerCtx` declares this field as abstract; omitting it produces a compile error.
3. **All fields must have default values** -- the `Empty[T]` derivation macro reads default parameter values from the companion object to construct the initial context.

Mutable state fields must be `var` so the lexer can assign to them directly.

## Tracking Indentation

Define a context with `currentIndent` and `prevIndent` fields; when a newline followed by spaces is matched, count the spaces to determine the new indentation level and compare it against the previous level.
Guards are not supported in lexer rules (`case "regex" if condition =>` is a compile error); check the condition inside the rule body instead.
Emit `Token["INDENT"]` when indentation increases, `Token["DEDENT"]` when it decreases, and `Token.Ignored` when it stays the same.

```scala sc:nocompile
import alpaca.*

case class IndentCtx(
  var text: CharSequence = "",
  var currentIndent: Int = 0,
  var prevIndent: Int = 0,
) extends LexerCtx

val IndentLexer = lexer[IndentCtx]:
  case "\\n( *)" =>
    val newIndent = ctx.text.toString.count(_ == ' ')
    val prev = ctx.prevIndent
    ctx.prevIndent = newIndent
    ctx.currentIndent = newIndent
    // Guards are not supported -- check condition in body
    if newIndent > prev then Token["INDENT"](newIndent)
    else if newIndent < prev then Token["DEDENT"](newIndent)
    else Token.Ignored
  case word @ "[a-z_][a-z0-9_]*" => Token["WORD"](word)
  case "\\s+" => Token.Ignored
```

The `\\n( *)` pattern matches a newline followed by zero or more spaces.
`ctx.text` contains the full match text at the time the rule body runs; counting spaces in it gives the new indentation level.
`Token["INDENT"](newIndent)` and `Token["DEDENT"](newIndent)` carry the new depth as their value, which the parser can read.
Because guards are not supported, the `if`/`else` is inside the rule body rather than after the pattern.

## Reading INDENT and DEDENT in the Parser

The parser sees `INDENT` and `DEDENT` tokens in the lexeme list just like any other token.
Use `IndentLexer.INDENT(n)` to extract the new depth from the lexeme value -- `n` is a `Lexeme` and `n.value` is the `Int` depth passed to `Token["INDENT"](newIndent)`.

```scala sc:nocompile
import alpaca.*

object IndentParser extends Parser:
  val Block: Rule[List[String]] = rule(
    { case (IndentLexer.INDENT(_), Block(inner), IndentLexer.DEDENT(_)) => inner },
    { case IndentLexer.WORD(w) => List(w.value.asInstanceOf[String]) },
  )
  val root: Rule[List[String]] = rule:
    case Block(b) => b
```

## Key Points

- `case class` with `var text: CharSequence = ""` is mandatory; other mutable fields must be `var`
- Guards (`case "regex" if condition =>`) are a compile error; move conditions into the rule body
- `ctx` is a compile-time construct available only inside lexer rule bodies
- `Token["INDENT"](value)` and `Token["DEDENT"](value)` are distinct named token types; the value carries the new depth for use in the parser
- `Token.Ignored` produces no lexeme; the newline pattern emits either `INDENT`, `DEDENT`, or nothing depending on the depth change

## See Also

- [Lexer Context](../lexer-context.html) -- full `LexerCtx` reference: case class contract, `BetweenStages`, `PositionTracking`, `LineTracking`
- [Lexer Error Recovery](../lexer-error-recovery.html) -- guards limitation and workaround
- [Lexer](../lexer.html) -- `Token["NAME"](value)` constructor, `Token.Ignored`
