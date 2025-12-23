package alpaca
package internal
package lexer

import scala.annotation.tailrec

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

  private val extractOrTypes: PartialFunction[Tree, List[Tree]] =
    case Applied(tpt, List(left, right)) if tpt.tpe =:= TypeRepr.of[|] => extractOrTypes(left) ++ extractOrTypes(right)
    case Typed(_, pattern) => extractOrTypes(pattern)
    case pattern => List(pattern)
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
  def apply[T <: ValidNameLike: Type](pattern: Tree): List[Expr[TokenInfo[?]]] =
    @tailrec def loop(tpe: TypeRepr, pattern: Tree): List[Expr[TokenInfo[?]]] =
      (tpe, pattern) match
        // case x @ "regex" => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Literal(StringConstant(regex)))) if name == bind =>
          TokenInfo.unsafe(regex, Vector(regex)) :: Nil
        // case x @ 'l' => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Literal(CharConstant(regex)))) if name == bind =>
          TokenInfo.unsafe(regex.toString, Vector(regex)) :: Nil
        // case x @ ("regex" | 'l') => Token[x.type]
        case (TermRef(qual, name), Bind(bind, Alternatives(alternatives))) if name == bind =>
          alternatives.unsafeMap:
            case Literal(StringConstant(str)) => TokenInfo.unsafe(str, Vector(str))
            case Literal(CharConstant(str)) => TokenInfo.unsafe(str.toString, Vector(str))
        // case x @ <?> => Token[<?>]
        case (tpe, Bind(_, tree)) =>
          loop(tpe, tree)
        // case x : "regex" => Token.Ignored
        case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          TokenInfo.unsafe(str, Vector(str)) :: Nil
        // case x : 'l' => Token.Ignored
        case (tpe, Literal(CharConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          TokenInfo.unsafe(str.toString, Vector(str)) :: Nil
        // case x : ("regex" | 'l') => Token.Ignored
        case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
          alternatives.unsafeMap:
            case Literal(StringConstant(str)) => TokenInfo.unsafe(str, Vector(str))
            case Singleton(Literal(StringConstant(str))) => TokenInfo.unsafe(str, Vector(str))
            case Literal(CharConstant(str)) => TokenInfo.unsafe(str.toString, Vector(str))
            case Singleton(Literal(CharConstant(str))) => TokenInfo.unsafe(str.toString, Vector(str))
        // case x : "regex" => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          TokenInfo.unsafe(name, Vector(regex)) :: Nil
        // case x : 'l' => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(CharConstant(regex))) =>
          TokenInfo.unsafe(name, Vector(regex)) :: Nil
        // case x : ("regex" | 'l') => Token["name"]
        case (ConstantType(StringConstant(str)), extractOrTypes(alternatives)) =>
          TokenInfo.unsafe(
            str,
            alternatives
              .unsafeMap:
                case Literal(StringConstant(str)) => str
                case Singleton(Literal(StringConstant(str))) => str
                case Literal(CharConstant(char)) => char
                case Singleton(Literal(CharConstant(char))) => char
              .toVector,
          ) :: Nil
        // case x: ("regex" | 'l') => Token[x.type]
        case (TermRef(qual, name), extractOrTypes(alternatives)) =>
          alternatives.unsafeMap:
            case Literal(StringConstant(str)) => TokenInfo.unsafe(str, Vector(str))
            case Singleton(Literal(StringConstant(str))) => TokenInfo.unsafe(str, Vector(str))
            case Literal(CharConstant(str)) => TokenInfo.unsafe(str.toString, Vector(str))
            case Singleton(Literal(CharConstant(str))) => TokenInfo.unsafe(str.toString, Vector(str))
        case _ =>
          raiseShouldNeverBeCalled(
            s"""
               |tpe: ${tpe.show}
               |pattern: ${pattern.show}
               |tpe: ${tpe.toString}
               |pattern: ${pattern.toString}
               |""".stripMargin,
          )

    loop(TypeRepr.of[T], pattern)
}
