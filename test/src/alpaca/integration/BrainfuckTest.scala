package alpaca
package integration

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.io.StdIn.readChar

final class BrainfuckTest extends AnyFunSuite:

  case class BrainLexContext(var brackets: Int = 0, var squareBrackets: Int = 0) extends LexerCtx

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
    case name @ "[A-Za-z]+" =>
      Token["functionName"](name)
    case "\\(" =>
      ctx.brackets += 1
      Token["functionOpen"]
    case "\\)" =>
      ctx.brackets -= 1
      Token["functionClose"]
    case "!" =>
      Token["functionCall"]
    case "." => Token.Ignored
    case "\n" => Token.Ignored

  class Memory(
    val underlying: Array[Int] = new Array(256),
    var pointer: Int = 0,
    val functions: mutable.Map[String, List[AST]] = mutable.Map.empty,
  )

  enum AST:
    case Root(ops: List[AST])
    case While(ops: List[AST])
    case FunctionDef(name: String, ops: List[AST])
    case FunctionCall(name: String)
    case Next, Prev, Inc, Dec, Print, Read

  extension (ast: AST)
    def eval(mem: Memory): Unit = ast match
      case AST.Root(ops) => ops.foreach(_.eval(mem))
      case AST.Next => mem.pointer += 1
      case AST.Prev => mem.pointer -= 1
      case AST.Inc => mem.underlying(mem.pointer) += 1
      case AST.Dec => mem.underlying(mem.pointer) -= 1
      case AST.Print => print(mem.underlying(mem.pointer).toChar)
      case AST.Read => mem.underlying(mem.pointer) = readChar().toByte
      case AST.While(ops) =>
        while mem.underlying(mem.pointer) != 0 do ops.foreach(_.eval(mem))
      case AST.FunctionDef(name, ops) => mem.functions += (name -> ops)
      case AST.FunctionCall(name) =>
        mem.functions.get(name) match
          case Some(ops) => ops.foreach(_.eval(mem))
          case _ => throw RuntimeException(s"Undefined function: $name")

  case class BrainParserCtx(functions: mutable.Set[String] = mutable.Set.empty) extends ParserCtx

  object BrainParser extends Parser[BrainParserCtx]:
    val root: Rule[AST] = rule:
      case Operation.List(stmts) => AST.Root(stmts)

    val While: Rule[AST] = rule:
      case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) => AST.While(stmts)

    val Operation: Rule[AST] = rule(
      { case BrainLexer.next(_) => AST.Next },
      { case BrainLexer.prev(_) => AST.Prev },
      { case BrainLexer.inc(_) => AST.Inc },
      { case BrainLexer.dec(_) => AST.Dec },
      { case BrainLexer.print(_) => AST.Print },
      { case BrainLexer.read(_) => AST.Read },
      { case While(whl) => whl },
      { case FunctionDef(fdef) => fdef },
      { case FunctionCall(call) => call },
    )

    val FunctionDef: Rule[AST] = rule {
      case (
            BrainLexer.functionName(name),
            BrainLexer.functionOpen(_),
            Operation.List(ops),
            BrainLexer.functionClose(_),
          ) =>
        require(ctx.functions.add(name.value), s"Function ${name.value} is already defined")
        AST.FunctionDef(name.value, ops)
    }

    val FunctionCall: Rule[AST] = rule { case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
      require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
      AST.FunctionCall(name.value)
    }

  // --- Lexer Tests ---

  private def tokenize(input: String) = BrainLexer.tokenize(input)

  test("tokenize basic operators") {
    val (_, tokens) = tokenize("><+-.,")
    assert(tokens.map(_.name) == List("next", "prev", "inc", "dec", "print", "read"))
  }

  test("tokenize brackets") {
    val (ctx, tokens) = tokenize("[>+<-]")
    assert(ctx.squareBrackets == 0)
    assert(tokens.map(_.name) == List("jumpForward", "next", "inc", "prev", "dec", "jumpBack"))
  }

  test("mismatched closing bracket") {
    intercept[IllegalArgumentException] {
      tokenize("]")
    }
  }

  test("tokenize function syntax") {
    val (ctx, tokens) = tokenize("foo(++)")
    assert(ctx.brackets == 0)
    assert(tokens.map(_.name) == List("functionName", "functionOpen", "inc", "inc", "functionClose"))
  }

  test("tokenize function call") {
    val (_, tokens) = tokenize("foo!")
    assert(tokens.map(_.name) == List("functionName", "functionCall"))
  }

  test("ignore whitespace and unknown chars") {
    val (_, tokens) = tokenize("+ +\n+")
    assert(tokens.map(_.name) == List("inc", "inc", "inc"))
  }

  // --- Parser Tests ---

  private def parse(input: String): AST =
    val (_, tokens) = BrainLexer.tokenize(input)
    val (_, ast) = BrainParser.parse(tokens)
    ast.nn

  test("parse basic operations") {
    val ast = parse("><+-.,")
    assert(ast == AST.Root(List(AST.Next, AST.Prev, AST.Inc, AST.Dec, AST.Print, AST.Read)))
  }

  test("parse while loop") {
    val ast = parse("[>+<-]")
    assert(ast == AST.Root(List(AST.While(List(AST.Next, AST.Inc, AST.Prev, AST.Dec)))))
  }

  test("parse nested loops") {
    val ast = parse("[>[+]-]")
    assert(ast == AST.Root(List(AST.While(List(AST.Next, AST.While(List(AST.Inc)), AST.Dec)))))
  }

  test("parse empty program") {
    val ast = parse("")
    assert(ast == AST.Root(List.empty))
  }

  test("parse function definition") {
    val ast = parse("foo(++)")
    assert(ast == AST.Root(List(AST.FunctionDef("foo", List(AST.Inc, AST.Inc)))))
  }

  test("parse function definition and call") {
    val ast = parse("foo(++)foo!")
    assert(ast == AST.Root(List(AST.FunctionDef("foo", List(AST.Inc, AST.Inc)), AST.FunctionCall("foo"))))
  }

  // --- Eval Tests ---

  private def run(input: String): Memory =
    val (_, tokens) = BrainLexer.tokenize(input)
    val (_, ast) = BrainParser.parse(tokens)
    val mem = new Memory()
    ast.nn.eval(mem)
    mem

  private def runAndCapture(input: String): (Memory, String) =
    val (_, tokens) = BrainLexer.tokenize(input)
    val (_, ast) = BrainParser.parse(tokens)
    val mem = new Memory()
    val out = new java.io.ByteArrayOutputStream()
    Console.withOut(out) {
      ast.nn.eval(mem)
    }
    (mem, out.toString)

  test("increment") {
    val mem = run("+++")
    assert(mem.underlying(0) == 3)
  }

  test("decrement") {
    val mem = run("+++-")
    assert(mem.underlying(0) == 2)
  }

  test("move pointer") {
    val mem = run("+++>++>+")
    assert(mem.underlying(0) == 3)
    assert(mem.underlying(1) == 2)
    assert(mem.underlying(2) == 1)
    assert(mem.pointer == 2)
  }

  test("move pointer back") {
    val mem = run(">+++<++")
    assert(mem.underlying(0) == 2)
    assert(mem.underlying(1) == 3)
    assert(mem.pointer == 0)
  }

  test("simple loop - clear cell") {
    val mem = run("+++++[-]")
    assert(mem.underlying(0) == 0)
  }

  test("loop - multiply") {
    val mem = run("+++++[>+++<-]")
    assert(mem.underlying(0) == 0)
    assert(mem.underlying(1) == 15)
  }

  test("nested loops") {
    val mem = run("++[>++[>+++<-]<-]")
    assert(mem.underlying(2) == 12)
  }

  test("print character") {
    val (_, output) = runAndCapture("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++.")
    assert(output == "H")
  }

  test("hello world") {
    val (_, output) = runAndCapture(
      "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.",
    )
    assert(output == "Hello World!\n")
  }

  test("function definition registers in memory") {
    val mem = run("foo(+++)")
    assert(mem.functions.contains("foo"))
  }

  test("function call executes body") {
    val mem = run("foo(+++)foo!")
    assert(mem.underlying(0) == 3)
  }

  test("function call multiple times") {
    val mem = run("foo(+++)foo!foo!")
    assert(mem.underlying(0) == 6)
  }

  test("function with pointer movement") {
    val mem = run("foo(>+++)foo!foo!")
    assert(mem.underlying(1) == 3)
    assert(mem.underlying(2) == 3)
    assert(mem.pointer == 2)
  }

  test("undefined function call fails") {
    intercept[RuntimeException] {
      run("bar!")
    }
  }
