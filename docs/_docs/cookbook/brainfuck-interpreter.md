# BrainFuck Interpreter

This guide assembles the complete BrainFuck> interpreter that has been built incrementally across the documentation. It combines every Alpaca feature: custom lexer context, value-bearing tokens, EBNF operators, parser context, and semantic actions.

**What you'll build:** a working interpreter for BrainFuck extended with named function definitions and calls.

## The BrainFuck> Language

Standard BrainFuck has eight single-character commands operating on an array of 256 bytes with a movable pointer:

| Command | Meaning |
|---------|---------|
| `>` | Move pointer right |
| `<` | Move pointer left |
| `+` | Increment current cell |
| `-` | Decrement current cell |
| `.` | Print current cell as ASCII |
| `,` | Read one byte into current cell |
| `[` | Jump forward past matching `]` if current cell is zero |
| `]` | Jump back to matching `[` if current cell is nonzero |

BrainFuck> extends this with:
- `name(body)` -- define a function with the given name and body
- `name!` -- call a previously defined function

Everything else is a comment.

## The AST

```scala
enum BrainAST:
  case Root(ops: List[BrainAST])
  case While(ops: List[BrainAST])
  case FunctionDef(name: String, ops: List[BrainAST])
  case FunctionCall(name: String)
  case Next, Prev, Inc, Dec, Print, Read
```

## The Lexer

The lexer tracks bracket depth in a custom context to catch mismatches at lex time:

```scala
import alpaca.*

case class BrainLexContext(
  var brackets: Int = 0,
  var squareBrackets: Int = 0,
) extends LexerCtx

val BrainLexer = lexer[BrainLexContext]:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "\\[" =>
    ctx.squareBrackets += 1
    Token["jumpForward"]
  case "\\]" =>
    require(ctx.squareBrackets > 0, "Mismatched brackets")
    ctx.squareBrackets -= 1
    Token["jumpBack"]
  case name @ "[A-Za-z]+" => Token["functionName"](name)
  case "\\(" =>
    ctx.brackets += 1
    Token["functionOpen"]
  case "\\)" =>
    require(ctx.brackets > 0, "Mismatched brackets")
    ctx.brackets -= 1
    Token["functionClose"]
  case "!" => Token["functionCall"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored
```

## The Parser

The parser uses `ParserCtx` to track defined function names and reject calls to undefined functions:

```scala sc:nocompile
import alpaca.*
import scala.collection.mutable

case class BrainParserCtx(
  functions: mutable.Set[String] = mutable.Set.empty,
) extends ParserCtx
object BrainParser extends Parser[BrainParserCtx]:
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val While: Rule[BrainAST] = rule:
    case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) =>
      BrainAST.While(stmts)

  val Operation: Rule[BrainAST] = rule(
    { case BrainLexer.next(_) => BrainAST.Next },
    { case BrainLexer.prev(_) => BrainAST.Prev },
    { case BrainLexer.inc(_) => BrainAST.Inc },
    { case BrainLexer.dec(_) => BrainAST.Dec },
    { case BrainLexer.print(_) => BrainAST.Print },
    { case BrainLexer.read(_) => BrainAST.Read },
    { case While(whl) => whl },
    { case FunctionDef(fdef) => fdef },
    { case FunctionCall(call) => call },
  )

  val FunctionDef: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionOpen(_),
          Operation.List(ops), BrainLexer.functionClose(_)) =>
      require(ctx.functions.add(name.value), s"Function ${name.value} is already defined")
      BrainAST.FunctionDef(name.value, ops)

  val FunctionCall: Rule[BrainAST] = rule:
    case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
      BrainAST.FunctionCall(name.value)
```

## The Evaluator

```scala sc:nocompile
import scala.collection.mutable

class Memory(
  val cells: Array[Int] = new Array(256),
  var pointer: Int = 0,
  val functions: mutable.Map[String, List[BrainAST]] = mutable.Map.empty,
)

extension (ast: BrainAST)
  def eval(mem: Memory): Unit = ast match
    case BrainAST.Root(ops)  => ops.foreach(_.eval(mem))
    case BrainAST.Next       => mem.pointer = (mem.pointer + 1) & 0xff
    case BrainAST.Prev       => mem.pointer = (mem.pointer - 1) & 0xff
    case BrainAST.Inc        => mem.cells(mem.pointer) = (mem.cells(mem.pointer) + 1) & 0xff
    case BrainAST.Dec        => mem.cells(mem.pointer) = (mem.cells(mem.pointer) - 1) & 0xff
    case BrainAST.Print      => print(mem.cells(mem.pointer).toChar)
    case BrainAST.Read       => mem.cells(mem.pointer) = scala.io.StdIn.readChar() & 0xff
    case BrainAST.While(ops) => while mem.cells(mem.pointer) != 0 do ops.foreach(_.eval(mem))
    case BrainAST.FunctionDef(name, ops) => mem.functions += (name -> ops)
    case BrainAST.FunctionCall(name) =>
      mem.functions.get(name) match
        case Some(ops) => ops.foreach(_.eval(mem))
        case _ => throw RuntimeException(s"Undefined function: $name")
```

## Running Programs

```scala sc:nocompile
import alpaca.*

@main def run(): Unit =
  val program = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++."
  val (ctx, lexemes) = BrainLexer.tokenize(program)
  require(ctx.squareBrackets == 0 && ctx.brackets == 0, "Mismatched brackets")
  val (_, ast) = BrainParser.parse(lexemes)
  ast.nn.eval(Memory())
  // prints: Hello World!
```

With function extensions:

```scala sc:nocompile
val program = "foo(+++)foo!foo!"
val (ctx, lexemes) = BrainLexer.tokenize(program)
val (_, ast) = BrainParser.parse(lexemes)
val mem = Memory()
ast.nn.eval(mem)
// mem.cells(0) == 6  (two calls to foo, each incrementing 3 times)
```

## Testing

Key assertions from the test suite:

```scala sc:nocompile
// Lexer
val (_, tokens) = BrainLexer.tokenize("><+-.,")
assert(tokens.map(_.name) == List("next", "prev", "inc", "dec", "print", "read"))

// Parser
val (_, ast) = BrainParser.parse(BrainLexer.tokenize("[>+<-]")._2)
assert(ast == BrainAST.Root(List(
  BrainAST.While(List(BrainAST.Next, BrainAST.Inc, BrainAST.Prev, BrainAST.Dec))
)))

// Evaluator
val mem = Memory()
BrainParser.parse(BrainLexer.tokenize("+++++[-]")._2)._2.nn.eval(mem)
assert(mem.cells(0) == 0)  // cell cleared by loop
```

## Extensions

Ideas for extending the interpreter:

- **Error recovery** -- use `ErrorHandling.Strategy.IgnoreChar` instead of a catch-all pattern
- **Source positions** -- add `PositionTracking` and `LineTracking` to the lexer context for better error messages
- **String literals** -- add a `"..."` token for inline string output
- **Macros** -- add a `name{body}` syntax for compile-time expansion (no runtime function table)
