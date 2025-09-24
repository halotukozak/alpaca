package alpaca
package parser

import scala.annotation.compileTimeOnly

type RuleDefinition = PartialFunction[Tuple, Any]

trait Rule {
  @compileTimeOnly("Should never be called outside the parser definition")
  def unapply(x: Any): Option[Int] = ???
}

@compileTimeOnly("Should never be called outside the parser definition")
inline def rule(rules: RuleDefinition): Rule = ???

object Rule {
  extension (rule: Rule) {
    inline def alwaysAfter(rules: Rule*): Rule = ???
    inline def alwaysBefore(rules: Rule*): Rule = ???
  }
}
