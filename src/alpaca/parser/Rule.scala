package alpaca
package parser

import scala.annotation.compileTimeOnly

type RuleDefinition[T] = PartialFunction[Tuple, T]

trait Rule[+T] {
  @compileTimeOnly("Should never be called outside the parser definition")
  def unapply(x: Any): Option[T] = ???

  object List {
    @compileTimeOnly("Should never be called outside the parser definition")
    def unapply(x: Any): Option[List[T]] = ???
  }

  object Option {
    @compileTimeOnly("Should never be called outside the parser definition")
    def unapply(x: Any): Option[Option[T]] = ???
  }
}

object Rule {
  extension (rule: Rule[?]) {
    inline def alwaysAfter(rules: Rule[?]*): rule.type = ???
    inline def alwaysBefore(rules: Rule[?]*): rule.type = ???
  }
}
