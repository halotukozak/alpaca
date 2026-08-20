package bench.alpaca

import halotukozak.alpaca.internal.parser.Parser
import halotukozak.alpaca.{lexer, rule, Rule, Token}

val FlatListLexer = lexer {
  case "\\s+" => Token.Ignored
  case "a" => Token["a"]
}

object FlatListParser extends Parser:
  val root: Rule[List[Any]] = rule:
    case FlatListLexer.a.List(items) => items
