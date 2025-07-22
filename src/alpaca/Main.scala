package alpaca

@main def main(): Unit =
  val productions: List[Production] = List(
    Production(NonTerminal("S'"), List(NonTerminal("S"))),
    Production(NonTerminal("S"), List(NonTerminal("L"), Terminal("="), NonTerminal("R"))),
    Production(NonTerminal("S"), List(NonTerminal("R"))),
    Production(NonTerminal("L"), List(Terminal("*"), NonTerminal("R"))),
    Production(NonTerminal("L"), List(Terminal("id"))),
    Production(NonTerminal("R"), List(NonTerminal("L"))),
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
    print(centerText(s.toString))
    print("|")
  }

  for (i <- 0 to 13) {
    println("")
    print(centerText(i.toString))
    print("|")
    for (s <- symbols) {
      table.get((i, s)) match {
        case Some(value) => print(centerText(value.toString))
        case None => print(centerText(""))
      }
      print("|")
    }
  }

def centerText(text: String, width: Int = 10): String = {
  if text.length >= width then return text
  val padding = width - text.length
  val leftPad = padding / 2
  val rightPad = padding - leftPad
  (" " * leftPad) + text + (" " * rightPad)
}

//  for ((k, v) <- parseTable(productions)) {
//    println(s"$k -> $v")
//  }
