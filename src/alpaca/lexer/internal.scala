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
        // case x @ "regex" => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Literal(StringConstant(regex)))) if name == bind =>
          List(decodeName(regex) -> regex)
        // case x @ ("regex" | "regex2") => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Alternatives(alternatives))) if name == bind =>
          alternatives
            .map {
              case Literal(StringConstant(str)) => decodeName(str) -> str
              case x => raiseShouldNeverBeCalled(x.show)
            }
        // case x @ <?> => Token[<?>]
        case (tpe, Bind(_, tree)) =>
          loop(tpe, tree)
        // case x : "regex" => Token.Ignored
        case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          List(decodeName(str) -> str)
        // case x : ("regex" | "regex2") => Token.Ignored
        case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
          alternatives.map {
            case Literal(StringConstant(str)) => (decodeName(str), str)
            case x => raiseShouldNeverBeCalled(x.show)
          }
        // case x : "regex" => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          List((decodeName(name), regex))
        // case x : ("regex" | "regex2") => Token["name"]
        case (ConstantType(StringConstant(str)), Alternatives(alternatives)) =>
          List(
            decodeName(str) ->
              alternatives
                .map {
                  case Literal(StringConstant(str)) => str
                  case x => raiseShouldNeverBeCalled(x.show)
                }
                .mkString("|"),
          )
        case x =>
          raiseShouldNeverBeCalled(x.toString)

    loop(TypeRepr.of[T], pattern)
}
