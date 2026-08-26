# Contextual Lexing

This guide covers stateful tokenization: tracking nesting depth, maintaining counters, passing information from the lexer to the parser, and handling errors gracefully.

**What you'll learn:** custom `LexerCtx`, `ParserCtx`, the `OnTokenMatch` hook, `ErrorHandling` strategies, and how lexer context flows into parser rules.

## Tracking State During Lexing

The BrainFuck> lexer tracks bracket depth to catch mismatched brackets at lex time:

```scala sc-name:BrainLexer
import halotukozak.alpaca.*

case class BrainLexContext(
  var brackets: Int = 0,
  var squareBrackets: Int = 0,
) extends LexerCtx

val BrainLexer = lexer[BrainLexContext]:
  case "\\[" =>
    ctx.squareBrackets += 1
    Token["jumpForward"]
  case "\\]" =>
    require(ctx.squareBrackets > 0, "Mismatched brackets")
    ctx.squareBrackets -= 1
    Token["jumpBack"]
  case "\\(" =>
    ctx.brackets += 1
    Token["functionOpen"]
  case "\\)" =>
    require(ctx.brackets > 0, "Mismatched brackets")
    ctx.brackets -= 1
    Token["functionClose"]
  case name @ "[A-Za-z]+" => Token["functionName"](name)
  case "!" => Token["functionCall"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored
```

After tokenization, check the final context:

```scala sc-compile-with:BrainLexer
val (finalCtx, lexemes) = BrainLexer.tokenize("foo(+++)foo!")
require(finalCtx.squareBrackets == 0 && finalCtx.brackets == 0, "Mismatched brackets")
```

## Accessing Lexer Context in the Parser

Every `Lexeme` carries a snapshot of the lexer context at match time. Inside parser rules, use the binding to access positional info:

```scala sc-hidden sc-name:ctx-brainast sc-compile-with:BrainLexer
enum BrainAST:
  case Root(ops: List[BrainAST])
  case FunctionDef(name: String, ops: List[BrainAST])
  case FunctionCall(name: String)
```

```scala sc-compile-with:ctx-brainast
import halotukozak.alpaca.*

object BrainParser extends Parser:
  val root: Rule[BrainAST] = rule:
    case FunctionCall(fc) => fc

  val FunctionCall: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      // name.value: String -- the function name
      // name.position: Int -- 1-based column within the current line (if lexer uses PositionTracking)
      // name.line: Int -- line number (if lexer uses LineTracking)
      BrainAST.FunctionCall(name.value)
```

To get position and line numbers, extend your context with the tracking traits:

```scala
import halotukozak.alpaca.*
import halotukozak.alpaca.internal.lexer.{PositionTracking, LineTracking}

case class BrainLexContext(
  var brackets: Int = 0,
  var squareBrackets: Int = 0,
  var position: Int = 1,
  var line: Int = 1,
) extends LexerCtx with PositionTracking with LineTracking
```

## Parser-Level Context

`ParserCtx` is for state that evolves during parsing -- symbol tables, function registries, type environments. The BrainFuck> parser uses it to track defined functions:

```scala sc-compile-with:ctx-brainast
import halotukozak.alpaca.*
import scala.collection.mutable

case class BrainParserCtx(
  functions: mutable.Set[String] = mutable.Set.empty,
) extends ParserCtx
object BrainParser extends Parser[BrainParserCtx]:
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val FunctionDef: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionOpen(_),
          Operation.List(ops), BrainLexer.functionClose(_)) =>
      require(ctx.functions.add(name.value), s"Function ${name.value} is already defined")
      BrainAST.FunctionDef(name.value, ops)

  val FunctionCall: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
      BrainAST.FunctionCall(name.value)

  val Operation: Rule[BrainAST] = rule(
    { case FunctionDef(fdef) => fdef },
    { case FunctionCall(call) => call },
    // ... other alternatives
  )
```

`ctx` is shared across all reductions in a single `parse()` call. A function defined in `FunctionDef` is immediately visible in `FunctionCall`.

## Error Handling Strategies

By default, the lexer throws on unmatched input. You can customize this with an `ErrorHandling` instance:

```scala sc-compile-with:BrainLexer
import halotukozak.alpaca.internal.lexer.ErrorHandling

// Option A: skip unrecognized characters silently
given ErrorHandling[BrainLexContext] = _ => ErrorHandling.Strategy.IgnoreChar
```

```scala sc-compile-with:BrainLexer
import halotukozak.alpaca.internal.lexer.ErrorHandling

// Option B: stop gracefully, returning what was tokenized so far
given ErrorHandling[BrainLexContext] = _ => ErrorHandling.Strategy.Stop
```

Four strategies are available:

| Strategy | Behavior |
|----------|----------|
| `Throw(ex)` | Abort with the given exception |
| `IgnoreChar` | Skip one character and continue |
| `IgnoreToken` | Skip to the next match and continue |
| `Stop` | Return lexemes collected so far |

An alternative to custom `ErrorHandling` is a catch-all pattern at the end of your lexer:

```scala
import halotukozak.alpaca.*

val LenientLexer = lexer:
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case x @ "." =>
    println(s"Unexpected character: $x")
    Token.Ignored   // skip and continue
```

This is simpler and often sufficient. The BrainFuck lexer uses this approach -- `"." => Token.Ignored` catches all non-command characters.

## Data Flow Summary

1. **Input** flows into the lexer
2. **`OnTokenMatch`** updates the `LexerCtx` after every match
3. **`Lexeme`s** are produced, each carrying a context snapshot
4. **`List[Lexeme]`** flows into the parser
5. **`ParserCtx`** is initialized and updated as rules are reduced
6. **Result** is produced, along with the final `ParserCtx`
