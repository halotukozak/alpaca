package alpaca.lexer

import scala.quoted.*
import scala.util.TupledFunction
import alpaca.core.raiseShouldNeverBeCalled

private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[T: Type](pattern: Tree): List[(name: Expr[ValidName], pattern: Expr[String])] = {
    // todo: @tailrec
    def loop(pattern: Tree): List[(String, String)] = pattern match
      case Bind(_, Literal(StringConstant(str))) => (str, str) :: Nil
      case Bind(_, Alternatives(alternatives)) => alternatives.flatMap(loop)
      case Literal(StringConstant(str)) => (str, str) :: Nil
      case Alternatives(alternatives) =>
        alternatives.flatMap(loop).foldLeft(("", "")) { case ((accName, accPattern), (name, pattern)) =>
          (accName + "_or_" + name, accPattern + "|" + pattern)
        } :: Nil
      case x => raiseShouldNeverBeCalled(x.show)

    def toResult(name: String, pattern: String) = (Expr(name).asExprOf[ValidName], Expr(pattern))

    TypeRepr.of[T] match
      case ConstantType(StringConstant(str)) => toResult(str, str) :: Nil
      case _ => loop(pattern).map(toResult)
  }
}
