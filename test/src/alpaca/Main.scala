package alpaca

import alpaca.core.{show, Showable}
import alpaca.lexer.context.default.DefaultLexem
import alpaca.parser.*
import alpaca.parser.Symbol.{NonTerminal, Terminal}

@main def main(): Unit = {
  val productions: List[Production] = List(
    Production(NonTerminal("S'"), Vector(NonTerminal("S"))),
    Production(NonTerminal("S"), Vector(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
    Production(NonTerminal("S"), Vector(NonTerminal("R"))),
    Production(NonTerminal("L"), Vector(Terminal("*"), NonTerminal("R"))),
    Production(NonTerminal("L"), Vector(Terminal("ID"))),
    Production(NonTerminal("R"), Vector(NonTerminal("L"))),
  )

  val symbols = List(
    Terminal("ID"),
    Terminal("*"),
    Terminal("="),
    Terminal("$"),
    NonTerminal("S"),
    NonTerminal("L"),
    NonTerminal("R"),
  )

  val table = parseTable(productions)

  print(centerText("State"))
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
  println("")

  val code = List(
    DefaultLexem("*", "*"),
    DefaultLexem("ID", "a"),
    DefaultLexem("=", "="),
    DefaultLexem("ID", "b"),
    DefaultLexem("$", "$"),
  )

  val result = new Parser(table).parse(code)
  // result.nn.display()
}

case class AST(value: Any, children: List[AST] = Nil) {
  def display(indentSize: Int = 0): Unit = {
    println(" " * indentSize + value)
    children.foreach(_.display(indentSize + 2))
  }
}

object AST {
  given Dupa[AST] = AST.apply(_, _)
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
