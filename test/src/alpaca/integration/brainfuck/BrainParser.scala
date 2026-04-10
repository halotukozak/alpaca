package alpaca
package integration.brainfuck

import scala.collection.mutable

case class BrainParserCtx(functions: mutable.Set[String] = mutable.Set.empty) extends ParserCtx

object BrainParser extends Parser[BrainParserCtx]:
  val root: Rule[BrainAST] = rule:
    case Operation.List(stmts) => BrainAST.Root(stmts)

  val While: Rule[BrainAST] = rule:
    case (BrainLexer.jumpForward(_), Operation.List(stmts), BrainLexer.jumpBack(_)) => BrainAST.While(stmts)

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

  val FunctionDef: Rule[BrainAST] = rule {
    case (
          BrainLexer.functionName(name),
          BrainLexer.functionOpen(_),
          Operation.List(ops),
          BrainLexer.functionClose(_),
        ) =>
      require(ctx.functions.add(name.value), s"Function ${name.value} is already defined")
      BrainAST.FunctionDef(name.value, ops)
  }

  val FunctionCall: Rule[BrainAST] = rule { case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
    require(ctx.functions.contains(name.value), s"Function ${name.value} is not defined")
    BrainAST.FunctionCall(name.value)
  }
