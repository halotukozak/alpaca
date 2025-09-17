package alpaca
package lexer

import alpaca.core.raiseShouldNeverBeCalled

import scala.quoted.*
import scala.reflect.NameTransformer

def decodeName(name: String)(using quotes: Quotes): Expr[ValidName] =
  import quotes.reflect.*
  NameTransformer.decode(name) match
    case invalid @ "_" => report.errorAndAbort(s"Invalid token name: $invalid")
    case other => Expr(other).asExprOf[ValidName]

// this util has bugs for sure https://github.com/halotukozak/alpaca/issues/51
private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[T: Type](pattern: Tree): List[(name: Expr[ValidName], pattern: String)] = (TypeRepr.of[T], pattern) match
    case (ConstantType(StringConstant(str)), pattern) =>
      // todo: @tailrec
      def patternLoop(pattern: Tree): String = pattern match
        case Bind(_, bind) => patternLoop(bind)
        case Literal(StringConstant(str)) => str
        case Alternatives(alternatives) => alternatives.map(patternLoop).reduce(_ + "|" + _)
        case x => raiseShouldNeverBeCalled(x.show)

      List(decodeName(str) -> patternLoop(pattern))
    case (TermRef(qual, name), Bind(bind, pattern)) if name == bind =>
      pattern match
        case Literal(StringConstant(str)) => (decodeName(str), str) :: Nil
        case Alternatives(alternatives) => alternatives.map {
          case Literal(StringConstant(str)) => (decodeName(str), str)
          case x => raiseShouldNeverBeCalled(x.show)
        }
        case x => raiseShouldNeverBeCalled(x.show)
    case x =>
      raiseShouldNeverBeCalled(x.toString)
}
