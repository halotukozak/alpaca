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
      // todo: @tailrec
      def multipleLoop(pattern: Tree): List[(String, String)] = pattern match
        case Literal(StringConstant(str)) => (str, str) :: Nil
        case Alternatives(alternatives) =>
          alternatives
            .map(multipleLoop(_).reduce { case ((name, pattern), (altName, altPattern)) =>
              s"${name}_or_$altName" -> s"$pattern|$altPattern"
            })
        case x => raiseShouldNeverBeCalled(x.show)

      multipleLoop(pattern).map { case (name, pattern) => (decodeName(name), pattern) }
    case x =>
      raiseShouldNeverBeCalled(x.toString)
}
