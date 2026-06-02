package bench.alpaca

import alpaca.*

val DeepNestedLexer = lexer {
  case "\\s+" => Token.Ignored
  case x @ ("\\(" | "\\)") => Token[x.type]
  case num @ "\\d+" => Token["num"](num.toInt)
}

object DeepNestedParser extends Parser:
  val Expr: Rule[Int] = rule(
    { case (DeepNestedLexer.`\\(`(_), Expr(a), DeepNestedLexer.`\\)`(_)) => a },
    { case DeepNestedLexer.num(n) => n.value },
  )

  val root: Rule[Int] = rule:
    case Expr(v) => v
