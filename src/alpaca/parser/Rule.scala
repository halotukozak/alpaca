package alpaca
package parser

import alpaca.core.dummy
import alpaca.lexer.context.Lexem

import scala.annotation.compileTimeOnly

trait Rule[Result] {
  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[Result] = dummy

  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[Result]] = dummy

  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[Result]] = dummy
}

@compileTimeOnly(ParserOnly)
inline def rule[R](productions: PartialFunction[Tuple | Lexem[?, ?], R]*): Rule[R] = dummy
