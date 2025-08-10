package alpaca

import Symbol.*

@main def main(): Unit = lexerTest()

def lexerTest(): Unit = {
  val tokenDefs = List(
    Token("WHITESPACE", "[ \t\n]+".r, ignore = true),
    Token("IF", "if".r),
    Token("ELSE", "else".r),
    Token("WHILE", "while".r),
    Token("NUMBER", "\\d+".r),
    Token("ID", "[a-zA-Z_][a-zA-Z0-9_]*".r),
    Token("GET", ">=".r),
    Token("SET", "<=".r),
    Token("LT", "<".r),
    Token("GT", ">".r),
    Token("NEQ", "!=".r),
    Token("EQEQ", "==".r),
    Token("EQ", "=".r),
    Token("PLUS", "\\+".r),
    Token("MINUS", "-".r),
    Token("STAR", "\\*".r),
    Token("SLASH", "/".r),
  )

  print(Lexer(tokenDefs).tokenize("a = 3"))
}

def parserTest(): Unit = {
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
