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
private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q)(using DebugSettings):
  import quotes.reflect.*

  private def extractOrTypes(tree: Tree, level: Int = 0): List[Tree] = tree match
    case tree @ Applied(tpt, List(left, right)) if tpt.tpe =:= TypeRepr.of[|] =>
      s"Level $level: ${tree.toString}\n".soft
      extractOrTypes(left, level + 1) ++ extractOrTypes(right, level + 1)
    case tree @ Typed(_, pattern) =>
      s"Level $level: ${tree.toString}\n".soft
      extractOrTypes(pattern, level + 1)
    case pattern =>
      s"Final Level $level: ${pattern.toString}\n".soft
      List(pattern)

  def treeToStr(tree: Tree): String | Char = tree match
    case Literal(StringConstant(str)) => str
    case Singleton(Literal(StringConstant(str))) => str
    case Literal(CharConstant(char)) => char
    case Singleton(Literal(CharConstant(char))) => char
    case x => raiseShouldNeverBeCalled[String](x.toString)

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
          alternatives.map(treeToStr).map(str => TokenInfo.unsafe(str.toString, Vector(str)))
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
          alternatives.map(treeToStr).map(str => TokenInfo.unsafe(str.toString, Vector(str)))
        // case x : "regex" => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          TokenInfo.unsafe(name, Vector(regex)) :: Nil
        // case x : 'l' => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(CharConstant(regex))) =>
          TokenInfo.unsafe(name, Vector(regex)) :: Nil
        // case x : ("regex" | 'l') => Token["name"]
        case (ConstantType(StringConstant(str)), alternatives) =>
          TokenInfo.unsafe(
            str,
            extractOrTypes(alternatives).map(treeToStr).toVector,
          ) :: Nil
        // case x: ("regex" | 'l') => Token[x.type]
        case (TermRef(qual, name), alternatives) =>
          extractOrTypes(alternatives).map(treeToStr).map(str => TokenInfo.unsafe(str.toString, Vector(str)))

        case x => raiseShouldNeverBeCalled[List[Expr[TokenInfo[?]]]](x.toString)

    loop(TypeRepr.of[T], pattern)
