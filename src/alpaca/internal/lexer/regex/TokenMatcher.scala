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
final class TokenMatcher private[regex] (private val initial: Array[Subset]):

  /** Per-Subset cache: ASCII fast path is an Array index 0..127; non-ASCII fall back to a HashMap. */
  private val asciiCache = mutable.HashMap.empty[Subset, Array[AnyRef]]
  private val unicodeCache = mutable.HashMap.empty[Subset, mutable.HashMap[Int, Subset]]

  private def cachedDerive(s: Subset, c: Int): Subset =
    if c < 128 then
      val arr = asciiCache.getOrElseUpdate(s, new Array[AnyRef](128))
      val cached = arr(c)
      if cached != null then cached.asInstanceOf[Subset]
      else
        val v = s.derive(c)
        arr(c) = v.asInstanceOf[AnyRef]
        v
    else unicodeCache.getOrElseUpdate(s, mutable.HashMap.empty).getOrElseUpdate(c, s.derive(c))

  /**
   * Match a token starting at `start` in `input`.
   *
   * @return `Some((priority, end))` where `priority` is the index of the winning
   *         pattern and `end` is the exclusive end of the longest match. `None` if
   *         no pattern matches any (possibly empty) prefix at `start`.
   */
  def matchAt(input: CharSequence, start: Int): Option[(priority: Int, end: Int)] =
    val state = initial.clone()
    var lastAccept = firstNullable(state).map(idx => (priority = idx, end = start))

    var i = start
    while i < input.length && !allEmpty(state) do
      val c = Character.codePointAt(input, i)
      var k = 0
      while k < state.length do
        state(k) = cachedDerive(state(k), c)
        k += 1
      i += Character.charCount(c)
      firstNullable(state).foreach: idx =>
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

  private def firstNullable(state: Array[Subset]): Option[Int] =
    var k = 0
    while k < state.length do
      if state(k).nullable then return Some(k)
      k += 1
    None

  private def allEmpty(state: Array[Subset]): Boolean =
    var k = 0
    while k < state.length do
      if state(k) != Subset.empty then return false
      k += 1
    true

object TokenMatcher:

  /** Build a matcher from pre-parsed regexes (use this from macros after compile-time parsing). */
  def fromRegexes(initial: Regex*): TokenMatcher =
    TokenMatcher(initial.iterator.map(Subset.of).toArray)
