# Getting Started

This guide walks you through building a BrainFuck interpreter with Alpaca. By the end, you will have a working lexer, parser, and evaluator — roughly 60 lines of code.

BrainFuck is a minimal language with eight single-character commands. That makes it an ideal first project: the grammar is small enough to fit on screen, but rich enough to exercise lexing, parsing, loops, and AST construction.

## Prerequisites

- JDK 21 or later
- Mill 1.1.3+ (or SBT — see [Installation](index.md#installation) for SBT/Scala CLI setup)
- Scala 3.8.3-RC1 or later

## Project Setup

Create a Mill project with Alpaca as a dependency:

```mill
//| mill-version: 1.1.3
//| mill-jvm-version: 21

import mill._
import mill.scalalib._

object brainfuck extends ScalaModule {
  def scalaVersion = "3.8.3-RC1"
  def scalacOptions = Seq("-Yretain-trees")
  def mvnDeps = Seq(
    mvn"io.github.halotukozak::alpaca:0.1.0"
  )
}
```

The `-Yretain-trees` flag is required. Alpaca's macros inspect the AST of your lexer and parser definitions at compile time, and this flag tells the compiler to preserve that information.

## Step 1: The Lexer

BrainFuck has eight commands: `>`, `<`, `+`, `-`, `.`, `,`, `[`, `]`. Everything else is a comment (ignored).

```scala sc:nocompile sc-name:BrainLexer.scala
import alpaca.*

val BrainLexer = lexer:
  case ">" => Token["next"]
  case "<" => Token["prev"]
  case "\\+" => Token["inc"]
  case "-" => Token["dec"]
  case "\\." => Token["print"]
  case "," => Token["read"]
  case "\\[" => Token["jumpForward"]
  case "\\]" => Token["jumpBack"]
  case "." => Token.Ignored
  case "\n" => Token.Ignored
```

Each `case` maps a regex pattern to a token. `Token["next"]` creates a named token with a `String` value equal to the matched text. `Token.Ignored` matches but produces no output — the lexer skips it.

Pattern order matters: `"\\."` (literal dot — the print command) must appear before `"."` (any character — the catch-all). Otherwise the catch-all would shadow the print command.

Try it:

```scala sc:nocompile sc-compile-with:BrainLexer.scala
val (_, lexemes) = BrainLexer.tokenize("++[>+<-].")
println(lexemes.map(_.name))
// List(inc, inc, jumpForward, next, inc, prev, dec, jumpBack, print)
```

## Step 2: The AST

Before writing the parser, define the tree structure it will produce. BrainFuck programs are sequences of instructions, some of which contain nested instruction lists (loops):

```scala sc:nocompile sc-name:BrainAST.scala
enum BrainAST:
  case Root(ops: List[BrainAST])
  case While(ops: List[BrainAST])
  case Next, Prev, Inc, Dec, Print, Read
```

`Root` wraps the top-level program. `While` represents a `[...]` loop — it repeats its body while the current cell is nonzero.

## Step 3: The Parser

The parser turns a flat list of lexemes into a nested `BrainAST`:

```scala sc:nocompile sc-name:BrainParser.scala sc-compile-with:BrainLexer.scala,BrainAST.scala
import alpaca.*

object BrainParser extends Parser:
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
  )
```

Three things to note:

- **`root`** is required. It defines the grammar's start symbol. Here it matches zero or more `Operation`s via `.List`.
- **`Operation.List(stmts)`** is an EBNF operator. It matches zero or more occurrences of the `Operation` rule and returns a `List[BrainAST]`.
- **`BrainLexer.next(_)`** matches a lexeme whose token name is `"next"`. The `_` discards the lexeme value — we only care that the token appeared.

## Step 4: The Evaluator

BrainFuck operates on an array of 256 bytes with a movable pointer:

```scala sc:nocompile sc-name:Eval.scala sc-compile-with:BrainAST.scala
class Memory(
  val cells: Array[Int] = new Array(256),
  var pointer: Int = 0,
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
```

## Step 5: Run It

Wire the three stages together:

```scala sc:nocompile sc-compile-with:BrainLexer.scala,BrainParser.scala,BrainAST.scala,Eval.scala
import alpaca.*

@main def run(): Unit =
  val program = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++."
  val (_, lexemes) = BrainLexer.tokenize(program)
  val (_, ast) = BrainParser.parse(lexemes)
  ast.nn.eval(Memory())
  // prints: Hello World!
```

The pipeline is always the same: `tokenize` produces lexemes, `parse` produces an AST (or `null` on failure — `.nn` asserts non-null), and you do whatever you want with the result.

## What's Next

This BrainFuck interpreter uses the simplest form of every Alpaca feature. The rest of the documentation extends it:

- [Lexer](lexer.md) — regex patterns, value extraction, token naming rules
- [Lexer Context](lexer-context.md) — tracking state during tokenization (we add bracket matching to BrainLexer)
- [Parser](parser.md) — rules, named productions, EBNF operators
- [Parser Context](parser-context.md) — shared state during parsing (we add a function registry)
- [Extractors](extractors.md) — pattern matching on terminals and non-terminals
- [Conflict Resolution](conflict-resolution.md) — resolving shift/reduce and reduce/reduce conflicts
- [Theory](theory/pipeline.md) — formal foundations behind everything above
