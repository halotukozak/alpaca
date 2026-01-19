package alpaca
package internal
package lexer

import ox.tap

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
private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q)(using Log):
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
  def apply[T: Type](pattern: Tree): Flow[(Type[? <: ValidName], TokenInfo)] =
    Log.trace("compiling name and pattern")
    @tailrec def loop(tpe: TypeRepr, pattern: Tree): Flow[(Type[? <: ValidName], TokenInfo)] =
      Log.trace(show"looping with tpe=$tpe and pattern=$pattern")
      (tpe, pattern) match
        // case x @ "regex" => Token[x.type]
        case (TermRef(_, name), Bind(bind, Literal(StringConstant(regex)))) if name == bind =>
          Log.trace(show"matched simple regex with bind $bind and regex $regex")
          Flow.fromValues(TokenInfo(regex, regex))
        // case x @ ("regex" | "regex2") => Token[x.type]
        case (TermRef(_, name), Bind(bind, Alternatives(alternatives))) if name == bind =>
          Log.trace(
            show"matched alternative regex with bind $bind and alternatives ${alternatives.mkShow("[", ", ", "]")}",
          )
          alternatives.asFlow.unsafeMap:
            case Literal(StringConstant(str)) => TokenInfo(str, str)
        // case x @ <?> => Token[<?>]
        case (tpe, Bind(_, tree)) =>
          Log.trace(show"matched bind with tpe=$tpe and tree=$tree")
          loop(tpe, tree)
        // case x : "regex" => Token.Ignored
        case (tpe, Literal(StringConstant(str))) if tpe =:= TypeRepr.of[Nothing] =>
          Log.trace(show"matched ignored token with tpe=$tpe and str=$str")
          Flow.fromValues(TokenInfo(str, str))
        // case x : ("regex" | "regex2") => Token.Ignored
        case (tpe, Alternatives(alternatives)) if tpe =:= TypeRepr.of[Nothing] =>
          Log.trace(show"matched ignored token with tpe=$tpe and alternatives ${alternatives.mkShow}")
          alternatives.asFlow.unsafeMap:
            case Literal(StringConstant(str)) => TokenInfo(str, str)
        // case x : "regex" => Token["name"]
        case (ConstantType(StringConstant(name)), Literal(StringConstant(regex))) =>
          Log.trace(show"matched named token with name=$name and regex=$regex")
          Flow.fromValues(TokenInfo(name, regex))
        // case x : ("regex" | "regex2") => Token["name"]
        case (ConstantType(StringConstant(str)), Alternatives(alternatives)) =>
          Log.trace(show"matched named token with name=$str and alternatives ${alternatives.mkShow}")
          Flow.fromValues(
            TokenInfo(
              str,
              alternatives.asFlow
                .unsafeMap:
                  case Literal(StringConstant(str)) => str
                .mkShow("|"),
            ),
          )
        case x => raiseShouldNeverBeCalled[Flow[(Type[? <: ValidName], TokenInfo)]](x.toString)

    loop(TypeRepr.of[T], pattern).tapFlow: res =>
      Log.trace(show"compiled name and pattern to ${res.mkShow}")
