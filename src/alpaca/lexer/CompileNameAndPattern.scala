package alpaca
package lexer

import alpaca.core.raiseShouldNeverBeCalled
import alpaca.lexer.CompileNameAndPattern.*

import scala.annotation.tailrec
import scala.quoted.*
import alpaca.core.ValidName

/**
 * Compiler for lexer token patterns during macro expansion.
 *
 * This class extracts token names and patterns from pattern match trees
 * in lexer definitions. It handles various pattern forms including simple
 * patterns, alternatives, and bindings.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  /**
   * Compiles a pattern tree into token information.
   *
   * Extracts the token name and regex pattern from various forms of
   * pattern matching trees, handling bindings and alternatives.
   *
   * @tparam T the type of the pattern
   * @param pattern the pattern tree to compile
   * @return a list of TokenInfo expressions
   */
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

  /**
   * Validates a token name during macro expansion.
   *
   * Token names must not be underscore (_) as that would be invalid.
   *
   * @param name the token name to validate
   * @param quotes the Quotes instance
   * @return the validated name
   * @throws compilation error if the name is invalid
   */
  private def validateName(name: String)(using quotes: Quotes): ValidName =
    import quotes.reflect.*
    name match
      case invalid @ "_" => report.errorAndAbort(s"Invalid token name: $invalid")
      case other => other

  object Result {

    /**
     * Creates a TokenInfo expression from a name and regex pattern.
     *
     * This validates the name and constructs an expression that will
     * create a TokenInfo at runtime.
     *
     * @param name the token name
     * @param regex the regex pattern
     * @param quotes the Quotes instance
     * @return a TokenInfo expression
     */
    def unsafe(name: String, regex: String)(using quotes: Quotes): Expr[TokenInfo[?]] = {
      import quotes.reflect.*
      val validatedName = validateName(name)
      ConstantType(StringConstant(validatedName)).asType match
        case '[type nameTpe <: ValidName; nameTpe] =>
          '{
            TokenInfo[nameTpe](
              ${ Expr(validatedName).asExprOf[nameTpe] },
              ${ Expr(TokenInfo.nextName()) },
              ${ Expr(regex) },
            )
          }
    }
  }
}
