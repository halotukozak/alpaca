package alpaca
package parser

import alpaca.core.shoudNotBeCalled
import alpaca.lexer.context.Lexem

import scala.annotation.compileTimeOnly

trait Rule[Result] {
  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[Result] = shoudNotBeCalled

  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[Result]] = shoudNotBeCalled

  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[Result]] = shoudNotBeCalled
}

@compileTimeOnly(ParserOnly)
inline def rule[R](productions: PartialFunction[Tuple | Lexem[?, ?], R]*): Rule[R] = shoudNotBeCalled
