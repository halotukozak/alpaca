package alpaca
package internal
package lexer

import dregex.Regex
import ox.par

import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * Utility for checking regex patterns for shadowing issues.
 *
 * This object provides methods to check if any token patterns are
 * shadowed by others, which would mean they could never be matched.
 */
private[lexer] object RegexChecker:

  /**
   * Checks a sequence of regex patterns for shadowing.
   *
   * A pattern is shadowed if it is a subset of an earlier pattern,
   * meaning the earlier pattern would always match first and the
   * shadowed pattern would never be used.
   *
   * @param patterns the regex patterns to check.
   */
  def checkPatterns(patterns: List[String])(using Log): Unit = patterns match
    case Nil => ()
    case _ =>
      Log.trace("checking regex patterns for shadowing...")
      val regexes = Regex.compile(patterns.map(_ + ".*").asJava)

      par:
        for
          i <- patterns.indices
          j <- (i + 1) until regexes.size
        yield () => if regexes.get(j).isSubsetOf(regexes.get(i)) then throw ShadowException(patterns(j), patterns(i))
