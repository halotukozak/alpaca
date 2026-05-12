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
 * States are interned to integer IDs the first time they are reached; transitions
 * (per state, per code point) are then cached in flat arrays indexed by state ID.
 * After warm-up the hot loop is pure integer array indexing — no `Subset` equality,
 * no `HashMap` lookups, no per-character allocation.
 */
final class TokenMatcher private[regex] (initial: Array[Subset]):

  private val stateToId = mutable.HashMap.empty[Vector[Subset], Int]
  private val idToState = mutable.ArrayBuffer.empty[Vector[Subset]]
  private val asciiTrans = mutable.ArrayBuffer.empty[Array[Int]]
  private val unicodeTrans = mutable.ArrayBuffer.empty[mutable.HashMap[Int, Int]]
  private val acceptPriority = mutable.ArrayBuffer.empty[Int]
  private val isDead = mutable.ArrayBuffer.empty[Boolean]

  private val initialId = registerState(initial.toVector)

  private def registerState(s: Vector[Subset]): Int =
    stateToId.get(s) match
      case Some(id) => id
      case None =>
        val id = idToState.length
        stateToId(s) = id
        idToState.append(s)
        val arr = new Array[Int](128)
        java.util.Arrays.fill(arr, -1)
        asciiTrans.append(arr)
        unicodeTrans.append(mutable.HashMap.empty)
        acceptPriority.append(firstNullablePriority(s))
        isDead.append(s.forall(_ == Subset.empty))
        id

  private def transition(fromId: Int, c: Int): Int =
    if c < 128 then
      val arr = asciiTrans(fromId)
      val cached = arr(c)
      if cached >= 0 then cached
      else
        val nextId = registerState(idToState(fromId).map(_.derive(c)))
        arr(c) = nextId
        nextId
    else unicodeTrans(fromId).getOrElseUpdate(c, registerState(idToState(fromId).map(_.derive(c))))

  /**
   * Match a token starting at `start` in `input`.
   *
   * @return `Some((priority, end))` where `priority` is the index of the winning
   *         pattern and `end` is the exclusive end of the longest match. `None` if
   *         no pattern matches any (possibly empty) prefix at `start`.
   */
  def matchAt(input: CharSequence, start: Int): Option[(priority: Int, end: Int)] =
    var sid = initialId
    var lastPriority = acceptPriority(sid)
    var lastEnd = start
    var i = start
    while i < input.length && !isDead(sid) do
      val c = Character.codePointAt(input, i)
      sid = transition(sid, c)
      i += Character.charCount(c)
      val p = acceptPriority(sid)
      if p >= 0 then
        lastPriority = p
        lastEnd = i
    if lastPriority >= 0 then Some((priority = lastPriority, end = lastEnd)) else None

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

  private def firstNullablePriority(s: Vector[Subset]): Int =
    var k = 0
    while k < s.length do
      if s(k).nullable then return k
      k += 1
    -1

object TokenMatcher:

  /** Build a matcher from pre-parsed regexes (use this from macros after compile-time parsing). */
  def fromRegexes(initial: Regex*): TokenMatcher =
    TokenMatcher(initial.iterator.map(Subset.of).toArray)
