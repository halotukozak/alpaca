package alpaca.parser

import scala.quoted.Quotes
import scala.quoted.Expr

type RuleDefinition = PartialFunction[Tuple, Any]

class Rule {
  def unapply(x: Any): Option[Int] = ???
}

inline def rule(inline rules: RuleDefinition): Rule = ${ ruleImpl }
def ruleImpl(using quotes: Quotes): Expr[Rule] = '{
  ???
}
