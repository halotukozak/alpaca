package halotukozak
package alpaca
package internal
package lexer

import halotukozak.regex.{RegexParser, Subset}

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
   * Patterns using syntax this parser doesn't support (see [[halotukozak.regex.RegexParser]])
   * are skipped rather than failing the build - shadow detection is a best-effort diagnostic,
   * not a guarantee.
   *
   * @param patterns the regex patterns to check.
   */
  def checkPatterns(patterns: List[String]): Unit = patterns match
    case Nil => ()
    case patterns =>
      val subsets = patterns.map(parseOrSkip)

      for
        i <- patterns.indices
        j <- (i + 1) until subsets.size
        si <- subsets(i)
        sj <- subsets(j)
      do if sj.withAnySuffix.subset(si.withAnySuffix) then throw ShadowException(patterns(j), patterns(i))

  private def parseOrSkip(pattern: String): Option[Subset] =
    RegexParser.parse(pattern) match
      case Right(r) => Some(Subset.of(r))
      case Left(_) => None
