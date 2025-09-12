package alpaca.parser

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.annotation.compileTimeOnly

type RuleDefinition = PartialFunction[Tuple, Any]

class Rule {
  def unapply(x: Any): Option[Int] = ???
}

@compileTimeOnly("Should never be called outside the parser definition")
def rule(rules: RuleDefinition): Rule = ???
