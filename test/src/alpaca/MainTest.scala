package alpaca

import org.scalatest.funsuite.AnyFunSuite
import alpaca.lexer.*
import alpaca.parser.*
import alpaca.parser.Symbol.*
import alpaca.interpreter.Interpreter

class MainTest extends AnyFunSuite {
  test("Main.main") {
    // user defined
    val Lexer = lexer {
      case "\\s+" => Token.Ignored
      case "=" => Token["="]
      case "\\*" => Token["*"]
      case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"]
    }

    val productions: List[Production] = List(
      Production(NonTerminal("S'"), Vector(NonTerminal("S"))),
      Production(NonTerminal("S"), Vector(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
      Production(NonTerminal("S"), Vector(NonTerminal("R"))),
      Production(NonTerminal("L"), Vector(Terminal("*"), NonTerminal("R"))),
      Production(NonTerminal("L"), Vector(Terminal("ID"))),
      Production(NonTerminal("R"), Vector(NonTerminal("L"))),
    )

    val code = "*A = **B"

    // compile time
    val table = ParseTable(productions)

    // printTable(table, List(
    //       Terminal("ID"),
    //       Terminal("*"),
    //       Terminal("="),
    //       Terminal("$"),
    //       NonTerminal("S"),
    //       NonTerminal("L"),
    //       NonTerminal("R"),
    //     ))

    val interpreter = Interpreter(table)

    // runtime
    val tokens = Lexer.tokenize(code)

    // print(tokens)

    val ast = interpreter.run(tokens)
    // println(ast)
  }

  def printTable(table: ParseTable, symbols: List[Symbol]): Unit = {
    print(centerText("State"))
    print("|")
    for (s <- symbols) {
      print(centerText(s.show))
      print("|")
    }

    for (i <- 0 to 13) {
      println("")
      print(centerText(i.toString))
      print("|")
      for (s <- symbols) {
        print(centerText(table.get((i, s)).fold("")(_.show)))
        print("|")
      }
    }
    println("")
  }

  def centerText(text: String, width: Int = 10): String = {
    if text.length >= width then return text
    val padding = width - text.length
    val leftPad = padding / 2
    val rightPad = padding - leftPad
    (" " * leftPad) + text + (" " * rightPad)
  }
}
