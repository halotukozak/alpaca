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

  @tailrec def apply[T: Type](pattern: Tree): List[(name: Expr[ValidName], pattern: String)] =
    (TypeRepr.of[T], pattern) match
      // case x: "str" => Token[x.type]
      case (TermRef(qual, name), Bind(bind, Literal(StringConstant(str)))) if name == bind =>
        List((decodeName(str), str))
      // case x: "str1" | "str2" => Token[x.type]
      case (TermRef(qual, name), Bind(bind, Alternatives(alternatives))) if name == bind =>
        alternatives.map {
          case Literal(StringConstant(str)) => (decodeName(str), str)
          case x => raiseShouldNeverBeCalled(x.show)
        }
      // case x @ "str" => Token.Ignored
      case (tpe, Bind(bind, Literal(StringConstant(str)))) if tpe =:= TypeRepr.of[Nothing] =>
        List((decodeName(str), str))
      // case x @ "str1" | "str2" => Token.Ignored
      case (tpe, Bind(bind, Alternatives(alternatives))) if tpe =:= TypeRepr.of[Nothing] =>
        alternatives.map {
          case Literal(StringConstant(str)) => (decodeName(str), str)
          case x => raiseShouldNeverBeCalled(x.show)
        }
      // case x : "str" => Token.Ignored
      case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
        List((decodeName(str), str))
      // case x : "str1" | "str2" => Token.Ignored
      case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
        alternatives.map {
          case Literal(StringConstant(str)) => (decodeName(str), str)
          case x => raiseShouldNeverBeCalled(x.show)
        }
      // for case x @ ... => Token["name"]
      case (_, Bind(_, bind)) =>
        apply[T](bind)
      // for case x: "str" => Token["name"]
      case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
        List((decodeName(name), regex))
      // for case x: "str1" | "str2" => Token["name"]
      case (ConstantType(StringConstant(str)), Alternatives(alternatives)) =>
        List(decodeName(str) -> alternatives.foldLeft("") {
          case (acc, Literal(StringConstant(str))) => s"$acc|$str"
          case (_, x) => raiseShouldNeverBeCalled(x.show)
        })
      case x =>
        raiseShouldNeverBeCalled(x.toString)
}
