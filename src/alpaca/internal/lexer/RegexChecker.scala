package alpaca.internal
package lexer

import dregex.Regex

import java.util.regex.Pattern
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * Utility for checking regex patterns for shadowing issues.
 *
 * This object provides methods to check if any token patterns are
 * shadowed by others, which would mean they could never be matched.
 */
private[lexer] object RegexChecker {

  def checkInfos(infos: Seq[TokenInfo[?]])(using quotes: Quotes): Unit = for
    patterns = infos.map(_.toEscapedRegex)
    (j, i) <- checkPatterns(patterns)
  do quotes.reflect.report.error(s"Pattern ${infos(j).toRegex} is shadowed by ${infos(i).toRegex}")

  /**
   * Checks a sequence of regex patterns for shadowing.
   *
   * A pattern is shadowed if it is a subset of an earlier pattern,
   * meaning the earlier pattern would always match first and the
   * shadowed pattern would never be used.
   *
   * @param patterns the regex patterns to check
   * @return a sequence of error messages describing any shadowing issues
   */
  def checkPatterns(patterns: Seq[String]): Seq[(Int, Int)] = patterns match
    case Nil => Nil
    case _ =>
      val regexes = Regex.compile(patterns.map(_ + ".*").asJava)

      for
        i <- patterns.indices
        j <- (i + 1) until regexes.size
        if regexes.get(j).isSubsetOf(regexes.get(i))
      yield (j, i)
}
