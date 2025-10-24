package alpaca
package parser

import alpaca.lexer.*
import alpaca.lexer.context.Lexem
import scala.annotation.compileTimeOnly
import Rule.RuleOnly

trait Rule {
  // todo: move to generic
  type Result

  @compileTimeOnly(RuleOnly)
  inline def alwaysAfter(rules: (Token[?, ?, ?] | Rule)*): this.type = ???
  @compileTimeOnly(RuleOnly)
  inline def alwaysBefore(rules: (Token[?, ?, ?] | Rule)*): this.type = ???

  @compileTimeOnly(RuleOnly)
  inline def unapply(x: Any): Option[Result] = ???

  @compileTimeOnly(RuleOnly)
  inline def List: PartialFunction[Any, List[Result]] = ???

  @compileTimeOnly(RuleOnly)
  inline def Option: PartialFunction[Any, Option[Result]] = ???
}

def rule[R](productions: PartialFunction[Tuple | Lexem[?, ?], R]*): Rule.AUX[R] = ???

extension [R](production: PartialFunction[Tuple | Lexem[?, ?], R]) {
  transparent inline def named(name: String): production.type = production
}

object Rule {
  type AUX[R] = Rule { type Result = R }

  final val RuleOnly = "Should never be called outside the parser definition"
}
