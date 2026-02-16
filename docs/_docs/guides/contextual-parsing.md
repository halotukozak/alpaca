# Guide: Contextual Parsing

Contextual parsing refers to the ability of a lexer or parser to change its behavior or maintain state based on the input it has already seen. Alpaca provides powerful mechanisms for this through `LexerCtx`, `ParserCtx`, and the `BetweenStages` hook.

## 1. Lexer-Level Context

Most contextual logic in Alpaca happens at the lexer level. Since the lexer tokenizes the entire input before the parser starts, the lexer context is the primary place to track state that affects tokenization.

### Pattern: Brace Matching & Nesting

You can use a context to track nesting levels of braces, parentheses, or brackets.

```scala
import alpaca.*
import scala.collection.mutable.Stack

case class BraceCtx(
  var text: CharSequence = "",
  val stack: Stack[String] = Stack()
) extends LexerCtx

val myLexer = lexer[BraceCtx]:
  case "\(" => 
    ctx.stack.push("paren")
    Token["("]
  case "\)" => 
    if ctx.stack.isEmpty || ctx.stack.pop() != "paren" then
      throw RuntimeException("Mismatched parenthesis")
    Token[")"]
```

### Pattern: Indentation-Based Parsing

For languages like Python or Scala (with `braceless` syntax), you can track indentation levels in the context and emit virtual `INDENT` and `OUTDENT` tokens (or just adjust state for later use).

```scala
case class IndentCtx(
  var text: CharSequence = "",
  var currentIndent: Int = 0,
  val indents: Stack[Int] = Stack(0)
) extends LexerCtx

val pythonicLexer = lexer[IndentCtx]:
  case x @ "
 +" =>
    val newIndent = x.length - 1
    if newIndent > ctx.currentIndent then
      ctx.indents.push(newIndent)
      ctx.currentIndent = newIndent
      Token["INDENT"]
    else if newIndent < ctx.currentIndent then
      // ... logic to emit multiple OUTDENTs ...
      Token["OUTDENT"]
    else
      Token.Ignored
```

## 2. Accessing Lexer Context in the Parser

Every `Lexeme` matched by the lexer carries a "snapshot" of the `LexerCtx` as it was at the moment that specific token was matched.

This is extremely useful for error reporting or for logic that depends on when a token appeared.

```scala
object MyParser extends Parser:
  val root = rule:
    case MyLexer.ID(id) => 
      // id is a Lexeme, which has a .fields property
      // fields contains all members of your LexerCtx
      println(s"Matched ID at line ${id.fields.line}")
      id.value
```

## 3. Parser-Level Context (`ParserCtx`)

`ParserCtx` is for maintaining state during the reduction process. This is where you build symbol tables, track variable declarations, or perform type checking.

```scala
case class MyParserCtx(var symbols: Map[String, Type] = Map()) extends ParserCtx

object MyParser extends Parser[MyParserCtx]:
  val root = rule:
    case Decl(d) => d

  val Decl = rule:
    case (MyLexer.VAR(_), MyLexer.ID(id), MyLexer.TYPE(t)) =>
      ctx.symbols += (id.value -> t.value)
      // ...
```

## 4. Mode Switching (Lexical Feedback)

Sometimes you need to change how the lexer behaves based on what it just matched. For example, when parsing a string with interpolation:
`"Hello ${user.name}!"`

While Alpaca doesn't support "real-time" feedback from the parser to the lexer (as the lexer finishes first), you can implement modes within the lexer using context state.

```scala
case class ModeCtx(var inString: Boolean = false, var text: CharSequence = "") extends LexerCtx

val modeLexer = lexer[ModeCtx]:
  case """ => 
    ctx.inString = !ctx.inString
    Token["QUOTE"]
  
  case "[a-z]+" if !ctx.inString => Token["KEYWORD"]
  case "[^"]+" if ctx.inString  => Token["STRING_CONTENT"]
```

## 5. The `BetweenStages` Hook

The `BetweenStages` hook is the internal engine that powers context updates. It is a function called by Alpaca after **every** token match (including `Token.Ignored`) but **before** the next match starts.

### Automatic Updates
By default, Alpaca uses `BetweenStages` to automatically update the `text` field in your context. If your context extends `LineTracking` or `PositionTracking`, it also increments `line` and `position` counters.

### Customizing `BetweenStages`
If you need complex logic to run after every match, you can provide a custom `given` instance of `BetweenStages`.

```scala
given MyBetweenStages: BetweenStages[MyCtx] with
  def apply(token: Token[?, MyCtx, ?], matcher: Matcher, ctx: MyCtx): Unit =
    // Custom global logic
    println(s"Just matched ${token.info.name}")
```

## Summary of Data Flow

1. **Input String** flows into the `lexer`.
2. **`BetweenStages`** updates the `LexerCtx` after every match.
3. **`Lexeme`s** are produced, each capturing the current `LexerCtx` state.
4. **List[Lexeme]** flows into the `parser`.
5. **`ParserCtx`** is initialized and updated as rules are reduced.
6. **Result** is produced, along with the final `ParserCtx`.
