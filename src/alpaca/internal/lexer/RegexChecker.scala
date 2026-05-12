package alpaca
package internal
package lexer

import alpaca.internal.lexer.regex.{Regex, Subset}

/**
 * Cross-platform shadow detection for token regex patterns.
 *
 * A pattern is shadowed if every string it matches is also matched (as a prefix)
 * by an earlier pattern, meaning the earlier pattern would always be selected first.
 * Implemented via Brzozowski-derivative DFA emptiness check on `L(later · Σ*) ⊆ L(earlier · Σ*)`.
 */
private[lexer] object RegexChecker:

  /**
   * Checks a priority-ordered sequence of pre-parsed regexes for shadowing.
   *
   * @throws ShadowException if any pattern is shadowed by an earlier one.
   */
  def checkRegexes(items: List[(name: String, regex: Regex)])(using Log): Unit = items match
    case Nil => ()
    case _ =>
      logger.trace("checking regex patterns for shadowing...")
      val withSuffix = items.map((name, r) => name -> Subset.of(r).withAnySuffix)
      withSuffix.tails.foreach:
        case Nil => ()
        case (earlierName, earlierSub) :: laters =>
          laters.foreach: (laterName, laterSub) =>
            if laterSub.subset(earlierSub) then throw ShadowException(laterName, earlierName)
