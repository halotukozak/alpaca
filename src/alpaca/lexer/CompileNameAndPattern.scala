package alpaca
package lexer

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.lexer.CompileNameAndPattern.*

import scala.annotation.tailrec
import scala.quoted.*

private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[T: Type](pattern: Tree): List[Expr[TokenInfo[?]]] =
    @tailrec def loop(tpe: TypeRepr, pattern: Tree): List[Expr[TokenInfo[?]]] =
      (tpe, pattern) match
        // case x @ "regex" => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Literal(StringConstant(regex)))) if name == bind =>
          Result.unsafe(regex, regex) :: Nil
        // case x @ ("regex" | "regex2") => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Alternatives(alternatives))) if name == bind =>
          alternatives
            .map {
              case Literal(StringConstant(str)) => Result.unsafe(str, str)
              case x => raiseShouldNeverBeCalled(x.show)
            }
        // case x @ <?> => Token[<?>]
        case (tpe, Bind(_, tree)) =>
          loop(tpe, tree)
        // case x : "regex" => Token.Ignored
        case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          Result.unsafe(str, str) :: Nil
        // case x : ("regex" | "regex2") => Token.Ignored
        case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
          alternatives.map {
            case Literal(StringConstant(str)) => Result.unsafe(str, str)
            case x => raiseShouldNeverBeCalled(x.show)
          }
        // case x : "regex" => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          Result.unsafe(name, regex) :: Nil
        // case x : ("regex" | "regex2") => Token["name"]
        case (ConstantType(StringConstant(str)), Alternatives(alternatives)) =>
          Result.unsafe(
            str,
            alternatives
              .map {
                case Literal(StringConstant(str)) => str
                case x => raiseShouldNeverBeCalled(x.show)
              }
              .mkString("|"),
          ) :: Nil
        case x =>
          raiseShouldNeverBeCalled(x.toString)

    loop(TypeRepr.of[T], pattern)
}

private object CompileNameAndPattern {
  private def validateName(name: String)(using quotes: Quotes): ValidName =
    import quotes.reflect.*
    name match
      case invalid @ "_" => report.errorAndAbort(s"Invalid token name: $invalid")
      case other => other

  object Result {
    def unsafe(name: String, regex: String)(using quotes: Quotes): Expr[TokenInfo[?]] = {
      import quotes.reflect.*
      val validatedName = validateName(name)
      ConstantType(StringConstant(validatedName)).asType match
        case '[type nameTpe <: ValidName; nameTpe] =>
          '{ TokenInfo[nameTpe](${ Expr(validatedName).asExprOf[nameTpe] }, ${ Expr(regex) }) }
    }
  }
}
