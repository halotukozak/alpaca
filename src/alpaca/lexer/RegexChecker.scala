package alpaca.lexer

import dregex.Regex

import scala.jdk.CollectionConverters.*

object RegexChecker {
  def checkPatterns(patterns: Seq[String]): Seq[String] = patterns match
    case Nil => Nil
    case _ =>
      val regexes = Regex.compile(patterns.asJava)

      for {
        i <- patterns.indices
        j <- (i + 1) until regexes.size
        if regexes.get(j).isSubsetOf(regexes.get(i))
      } yield s"Pattern ${patterns(j)} is shadowed by ${patterns(i)}"
}
