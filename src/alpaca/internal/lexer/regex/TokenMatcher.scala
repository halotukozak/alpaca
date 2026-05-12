package alpaca
package internal
package lexer
package regex

import scala.collection.mutable

/**
 * Lexer-style longest-match tokenizer over a priority-ordered list of patterns.
 *
 * Simulates a tagged Brzozowski-derivative DFA in parallel for all patterns. Returns
 * the longest prefix match across all patterns; ties broken by lowest priority index
 * (i.e., the first pattern in the input list wins).
 *
 * Derivatives are memoized per `(Subset, codepoint)` pair across calls. The cache grows
 * lazily as inputs are consumed and is bounded by the size of the reachable derivative
 * state space (finite up to similarity, thanks to smart-constructor normalization).
 */
final class TokenMatcher private[regex] (initial: Vector[Subset]):

  private val cache = mutable.HashMap.empty[Subset, mutable.HashMap[Int, Subset]]

  private def cachedDerive(s: Subset, c: Int): Subset =
    cache.getOrElseUpdate(s, mutable.HashMap.empty).getOrElseUpdate(c, s.derive(c))

  /**
   * Match a token starting at `start` in `input`.
   *
   * @return `Some((priority, end))` where `priority` is the index of the winning
   *         pattern and `end` is the exclusive end of the longest match. `None` if
   *         no pattern matches any (possibly empty) prefix at `start`.
   */
  def matchAt(input: CharSequence, start: Int): Option[(priority: Int, end: Int)] =
    var state = initial
    var lastAccept = state.firstNullable.map(idx => (priority = idx, end = start))

    var i = start
    while i < input.length && !state.allEmpty do
      val c = Character.codePointAt(input, i)
      state = state.map(cachedDerive(_, c))
      i += Character.charCount(c)
      state.firstNullable.foreach: idx =>
        lastAccept = Some((priority = idx, end = i))

    lastAccept

  /** First position `>= from` at which some pattern matches a non-empty prefix. */
  def findFirst(input: CharSequence, from: Int): Option[(start: Int, priority: Int, end: Int)] =
    var p = from
    var result: (start: Int, priority: Int, end: Int) | Null = null
    while result == null && p < input.length do
      matchAt(input, p) match
        case Some((priority, end)) if end > p =>
          result = (start = p, priority = priority, end = end)
        case _ =>
          p += Character.charCount(Character.codePointAt(input, p))
    Option(result)

object TokenMatcher:

  /** Build a matcher from pre-parsed regexes (use this from macros after compile-time parsing). */
  def fromRegexes(initial: Regex*): TokenMatcher = TokenMatcher(initial.iterator.map(Subset.of).toVector)

extension (s: Vector[Subset])
  private[regex] def firstNullable: Option[Int] = s.iterator.zipWithIndex.collectFirst:
    case (sub, idx) if sub.nullable => idx

  private[regex] def allEmpty: Boolean = s.forall(_ == Subset.empty)
