package alpaca
import alpaca.core.{BetweenStages, Showable, show}
import alpaca.lexer.*
import alpaca.lexer.context.*
import alpaca.lexer.context.default.{DefaultGlobalCtx, DefaultLexem, LineTracking}
import alpaca.parser.*
import alpaca.parser.Symbol.{NonTerminal, Terminal}

@main def main(): Unit = {
  val Lexer = lexer {
    case "[0-9]+" => Token["NUMBER"]
    case "\\+" => Token["PLUS"]
    case "\\s+" => Token.Ignored["WHITESPACE"]
  }
  val result = Lexer.tokenize("42 + 13")

  val productions: List[Production] = List(
    Production(NonTerminal("S'"), Vector(NonTerminal("S"))),
    Production(NonTerminal("S"), Vector(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
    Production(NonTerminal("S"), Vector(NonTerminal("R"))),
    Production(NonTerminal("L"), Vector(Terminal("*"), NonTerminal("R"))),
    Production(NonTerminal("L"), Vector(Terminal("id"))),
    Production(NonTerminal("R"), Vector(NonTerminal("L"))),
  )

  val symbols = List(
    Terminal("id"),
    Terminal("*"),
    Terminal("="),
    Terminal("$"),
    NonTerminal("S"),
    NonTerminal("L"),
    NonTerminal("R"),
  )

  val table = parseTable(productions)

  print(centerText("S"))
  print("|")
  for (s <- symbols) {
    print(centerText(s.show))
    print("|")
  }

  for (i <- 0 to 13) {
    println("")
    print(centerText(i.show))
    print("|")
    for (s <- symbols) {
      print(centerText(table.get((i, s)).fold("")(_.show)))
      print("|")
    }
  }
}

def centerText(text: String, width: Int = 10): String = {
  if text.length >= width then return text
  val padding = width - text.length
  val leftPad = padding / 2
  val rightPad = padding - leftPad
  (" " * leftPad) + text + (" " * rightPad)
}

given Showable[Int | Production] =
  case i: Int => i.show
  case p: Production => p.show
