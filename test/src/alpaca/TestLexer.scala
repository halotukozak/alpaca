package alpaca

val Lexer = lexer {
  case literal @ ("<" | ">" | "=" | "+" | "-" | "*" | "/" | "(" | ")" | "[" | "]" | "{" | "}" | ":" | "'" | "," |
      ";") =>
    Token[literal.type]
  case "\\.\\+" => Token["dotAdd"]
  case "\\.\\-" => Token["dotSub"]
  case "\\.\\*" => Token["dotMul"]
  case "\\.\\/" => Token["dotDiv"]
  case "<=" => Token["lessEqual"]
  case ">=" => Token["greaterEqual"]
  case "!=" => Token["notEqual"]
  case "==" => Token["equal"]
  case x @ "(d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" => Token["float"](x.toDouble)
  case x @ "[0-9]+" => Token["int"](x.toInt)
  case x @ "[^\"]*" => Token["string"](x)
  case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "eye" | "zeros" | "ones" |
      "print") =>
    Token[keyword.type]
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
}
