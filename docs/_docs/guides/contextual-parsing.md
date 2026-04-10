# Guide: Contextual Parsing

Contextual parsing refers to the ability of a lexer or parser to change its behavior or maintain state based on the
input it has already seen. Alpaca provides powerful mechanisms for this through `LexerCtx`, `ParserCtx`, and the
`BetweenStages` hook.

## 1. Lexer-Level Context

Most contextual logic in Alpaca happens at the lexer level.
Since the lexer tokenizes the entire input before the parser starts, the lexer context is the primary place to track
state that affects tokenization.

### Example: Brace Matching & Nesting

You can use a context to track nesting levels of braces, parentheses, or brackets.

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class BraceCtx(
  var text: CharSequence = "",
  stack: mutable.Stack[String] = mutable.Stack()
) extends LexerCtx

val MyLexer = lexer[BraceCtx]:
  case "\\(" =>
    ctx.stack.push("paren")
    Token["("]
  case "\\)" =>
    if ctx.stack.isEmpty || ctx.stack.pop() != "paren" then
      throw RuntimeException("Mismatched parenthesis")
    Token[")"]
```

## 2. Accessing Lexer Context in the Parser

Every `Lexeme` matched by the lexer carries a "snapshot" of the `LexerCtx` as it was at the moment that specific token
was matched.

This is useful for error reporting or for logic that depends on when a token appeared.

```scala sc:nocompile
import alpaca.*

val MyLexer = lexer:
  case id @ "[A-Z]+" => Token["ID"](id)
  case "\\s+" => Token.Ignored

object MyParser extends Parser:
  val root = rule:
    case MyLexer.ID(id) =>
      // id is a Lexeme with context fields from LexerCtx
      // Access position with id.position, line with id.line
      id.value
```

## 3. Parser-Level Context (`ParserCtx`)

`ParserCtx` is for maintaining state during the reduction process.
This is where you build symbol tables, track variable declarations, or perform type checking.

```scala sc:nocompile
import alpaca.*

case class SymbolTableCtx(
  var symbols: Map[String, String] = Map()
) extends ParserCtx derives Copyable

object MyParser extends Parser[SymbolTableCtx]:
  val root = rule:
    case Decl(d) => d

  val Decl: Rule[Unit] = rule:
    case (MyLexer.ID(id), MyLexer.COLON(_), MyLexer.ID(tpe)) =>
      ctx.symbols += (id.value -> tpe.value)
```

[//]: # (todo: ten przyklad nie dziala)
[//]: # (## 4. Mode Switching &#40;Lexical Feedback&#41;)

[//]: # ()
[//]: # (Sometimes you need to change how the lexer behaves based on what it just matched. For example, tracking whether)

[//]: # (you are inside a string literal to lex its content differently.)

[//]: # ()
[//]: # (While Alpaca doesn't support real-time feedback from the parser to the lexer &#40;since the lexer finishes first&#41;, you can)

[//]: # (implement modes within the lexer using context state. The context state is checked in rule bodies — not as pattern guards)

[//]: # (— since Alpaca patterns are matched by the generated scanner at compile time, not at runtime.)

[//]: # ()
[//]: # (```scala sc:nocompile)

[//]: # (import alpaca.*)

[//]: # ()
[//]: # (case class ModeCtx&#40;)

[//]: # (  var text: CharSequence = "",)

[//]: # (  var inString: Boolean = false,)

[//]: # (&#41; extends LexerCtx)

[//]: # ()
[//]: # (val ModeLexer = lexer[ModeCtx]:)

[//]: # (  case "\"" =>)

[//]: # (    ctx.inString = !ctx.inString)

[//]: # (    Token["QUOTE"])

[//]: # (  case content @ "[^\"]+" =>)

[//]: # (    if ctx.inString then Token["STRING_CONTENT"]&#40;content&#41;)

[//]: # (    else Token["TEXT"]&#40;content&#41;)

[//]: # (  case "\\s+" => Token.Ignored)

[//]: # (```)

## 5. The `BetweenStages` Hook

The `BetweenStages` hook is the internal engine that powers context updates.
It is a function called by Alpaca after **every** token match (including `Token.Ignored`) but **before** the next match
starts.

### Automatic Updates

By default, Alpaca uses `BetweenStages` to automatically update the `text` field in your context (to advance past the
matched string).
If your context extends `LineTracking` or `PositionTracking`, the derived hooks also increment `line` and `position`
counters.

### Customizing `BetweenStages`

If you need complex logic to run after every match regardless of which token was matched, you can provide a custom
`given` instance of `BetweenStages` for a trait your context extends.

```scala 3 sc:nocompile
import alpaca.*
import alpaca.internal.lexer.BetweenStages

trait IndentTracking extends LexerCtx:
  var indentLevel: Int

given BetweenStages[IndentTracking] = 
    (_, "\t", ctx) => ctx.indentLevel += 1
    (_, "\n", ctx) => ctx.indentLevel = 0
    (_, _, _) => 

case class IndentCtx(
  var indentLevel: Int = 0,
) extends IndentTracking
```

Alpaca automatically composes `BetweenStages` instances from all parent traits of your context type. The `IndentTracking`
hook above will be combined with the base `LexerCtx` hook.

## Summary of Data Flow

1. **Input String** flows into the `lexer`.
2. **`BetweenStages`** updates the `LexerCtx` after every match.
3. **`Lexeme`s** are produced, each capturing the current `LexerCtx` state.
4. **List[Lexeme]** flows into the `parser`.
5. **`ParserCtx`** is initialized and updated as rules are reduced.
6. **Result** is produced, along with the final `ParserCtx`.
