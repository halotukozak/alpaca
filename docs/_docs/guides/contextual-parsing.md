# Guide: Contextual Parsing

Contextual parsing refers to the ability of a lexer or parser to change its behavior or maintain state based on the input it has already seen.
Alpaca provides powerful mechanisms for this through `LexerCtx`, `ParserCtx`, and the `BetweenStages` hook.

## 1. Lexer-Level Context

Most contextual logic in Alpaca happens at the lexer level.
Since the lexer tokenizes the entire input before the parser starts, the lexer context is the primary place to track state that affects tokenization.

### Exaple: Brace Matching & Nesting

You can use a context to track nesting levels of braces, parentheses, or brackets.

```scala
import alpaca.*
import scala.collection.mutable.Stack

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

Every `Lexeme` matched by the lexer carries a "snapshot" of the `LexerCtx` as it was at the moment that specific token was matched.

This is extremely useful for error reporting or for logic that depends on when a token appeared.

```scala
val MyLexer = lexer:
  case id @ "[A-Z]+" => Token["ID"](id)

object MyParser extends Parser:
  val root = rule:
    case MyLexer.ID(id) =>
      // id is a Lexeme, which has a .line property (and others) from the LexerCtx
      println(s"Matched ID at line ${id.line}")
      // fields contains all members of your LexerCtx
      println(s"All context fields: ${id.fields}")
      id.value
```

## 3. Parser-Level Context (`ParserCtx`)

`ParserCtx` is for maintaining state during the reduction process.
This is where you build symbol tables, track variable declarations, or perform type checking.

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
  case "\"" => 
    ctx.inString = !ctx.inString
    Token["QUOTE"]
  
  case "[a-z]+" if !ctx.inString => Token["KEYWORD"]
  case "[^\"]+" if ctx.inString  => Token["STRING_CONTENT"]
```

## Summary of Data Flow

1. **Input String** flows into the `lexer`.
2. **`BetweenStages`** updates the `LexerCtx` after every match.
3. **`Lexeme`s** are produced, each capturing the current `LexerCtx` state.
4. **List[Lexeme]** flows into the `parser`.
5. **`ParserCtx`** is initialized and updated as rules are reduced.
6. **Result** is produced, along with the final `ParserCtx`.
