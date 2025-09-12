package alpaca.lexer

import dk.brics.automaton._

object RegexChecker {
  def checkPatterns(patterns: List[String]): Option[(Int, Int)] = {
    val pairs = for {
      i <- patterns.indices
      j <- (i + 1) until patterns.indices.end
    } yield (i, j)

    pairs.find { (i, j) => {
      val reg1 = new RegExp(patterns(i)).toAutomaton()
      val reg2 = new RegExp(patterns(j)).toAutomaton()
      reg2.minus(reg1).isEmpty
    }}
  }
}