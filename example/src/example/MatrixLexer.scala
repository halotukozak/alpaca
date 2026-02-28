package example

import alpaca.{*, given}

@annotation.nowarn("msg=flexible type")
val MatrixLexer = lexer:
  case assignOp @ ("\\+=" | "-=" | "\\*=" | "/=") => Token["AssignOp"](assignOp.take(1) /*remove =*/ )
  case comp @ ("!=" | "<=" | ">=" | "==") => Token["LongComparator"](comp)
  case comp @ ("<" | ">") => Token["ShortComparator"](comp)
  case literal @ ("=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "\\{" | "\\}" | ":" | "'" | "," |
      ";") =>
    Token[literal.type]
  case dotOp @ ("\\.\\+" | "\\.\\-" | "\\.\\*" | "\\./") => Token["DotOp"](dotOp.drop(1) /*remove dot*/ )
  case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "print") =>
    Token[keyword.type]
  case function @ ("eye" | "zeros" | "ones") => Token["Function"](function)
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["Id"](id)
  case float @ "(\\d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" =>
    Token["Float"](float.toDouble)
  case int @ "[0-9]+" => Token["Int"](int.toInt)
  case string @ "\"[^\"]*\"" =>
    Token["String"](string.substring(1, string.length - 1))
  case (" " | "\t" | "\\#.*") => Token.Ignored
  case newLines @ "\n+" =>
    ctx.line += newLines.length
    Token.Ignored
