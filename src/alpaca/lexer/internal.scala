package alpaca
package lexer

import alpaca.core.raiseShouldNeverBeCalled

import scala.annotation.tailrec
import scala.quoted.*
import scala.reflect.NameTransformer

def decodeName(name: String)(using quotes: Quotes): Expr[ValidName] =
  import quotes.reflect.*
  NameTransformer.decode(name) match
    case invalid @ "_" => report.errorAndAbort(s"Invalid token name: $invalid")
    case other => Expr(other).asExprOf[ValidName]

private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[T: Type](pattern: Tree): List[(name: Expr[ValidName], pattern: String)] =
    @tailrec def loop(tpe: TypeRepr, pattern: Tree): List[(name: Expr[ValidName], pattern: String)] =
      (tpe, pattern) match
        case (TermRef(qual, name), Bind(bind, tree)) if name == bind =>
          loop(ConstantType(StringConstant(name)), tree)
        case (tpe, Bind(_, tree)) if tpe =:= TypeRepr.of[Nothing] =>
          loop(tpe, tree)
        case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          List(decodeName(str) -> str)
        case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
          alternatives.map {
            case Literal(StringConstant(str)) => (decodeName(str), str)
            case x => raiseShouldNeverBeCalled(x.show)
          }
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          List((decodeName(name), regex))
        case (ConstantType(StringConstant(str)), Alternatives(alternatives)) =>
          List(decodeName(str) -> alternatives.foldLeft("") {
            case (acc, Literal(StringConstant(str))) => s"$acc|$str"
            case (_, x) => raiseShouldNeverBeCalled(x.show)
          })
        case x =>
          raiseShouldNeverBeCalled(x.toString)

    loop(TypeRepr.of[T], pattern)
}
