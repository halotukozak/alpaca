package bench.alpaca

import alpaca.*

val FlatListLexer = lexer {
  case "\\s+" => Token.Ignored
  case "a" => Token["a"]
}

object FlatListParser extends Parser:
  val root: Rule[List[Any]] = rule:
    case FlatListLexer.a.List(items) => items
