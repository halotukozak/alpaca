package alpaca.parser

import scala.annotation.compileTimeOnly
import scala.quoted.{Expr, Quotes}

type RuleDefinition = PartialFunction[Tuple, Any]

class Rule {
  def unapply(x: Any): Option[Int] = ???
}

@compileTimeOnly("Should never be called outside the parser definition")
def rule(rules: RuleDefinition): Rule = ???
