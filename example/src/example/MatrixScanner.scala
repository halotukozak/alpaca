package example

import alpaca.*
import alpaca.ctx

val MatrixLexer = lexer:
  case "\\+=" => Token["ADDASSIGN"]
  case "-=" => Token["SUBASSIGN"]
  case "\\*=" => Token["MULASSIGN"]
  case "/=" => Token["DIVASSIGN"]

  case "!=" => Token["NOT_EQUAL"]
  case "<=" => Token["LESS_EQUAL"]
  case ">=" => Token["GREATER_EQUAL"]
  case "==" => Token["EQUAL"]

  case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "\\{" | "\\}" | ":" |
      "'" | "," | ";") =>
    Token[literal.type](literal)

  case "\\.\\+" => Token["DOTADD"]
  case "\\.\\-" => Token["DOTSUB"]
  case "\\.\\*" => Token["DOTMUL"]
  case "\\./" => Token["DOTDIV"]

  case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "eye" | "zeros" | "ones" |
      "print") =>
    Token[keyword.type](keyword)
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case float @ "(\\d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" => Token["FLOAT"](float.toDouble)
  case int @ "[0-9]+" => Token["INTNUM"](int.toInt)
  case string @ "\"[^\"]*\"" => Token["STRING"](string.substring(1, string.length - 1))
  case " \t" => Token.Ignored
  case "\\#.*" => Token.Ignored
  case newLines @ "\n+" =>
    ctx.line += newLines.count(_ == '\n')
    Token.Ignored

//
//    def error(self, t: Token) -> None:
//        report_error(self, f"Illegal character '{t.value[0]}'", self.lineno)
//        self.index += 1
