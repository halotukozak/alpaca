package alpaca.lexer

import dregex.Regex
import scala.jdk.CollectionConverters.*

object RegexChecker {
  def checkPatterns(patterns: List[String]) = {
    if patterns.isEmpty then 
      Nil
    else
      val regexes = Regex.compile(patterns.asJava)

      for {
        i <- patterns.indices
        j <- (i + 1) until regexes.size
        if regexes.get(j).isSubsetOf(regexes.get(i))
      } yield s"Pattern ${patterns(j)} is shadowed by ${patterns(i)}"
  }
}
