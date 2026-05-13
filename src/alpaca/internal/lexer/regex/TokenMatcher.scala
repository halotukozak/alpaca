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
  private var idToState: Array[Vector[Subset]] = new Array(16)
  private var asciiTrans: Array[Array[Int]] = new Array(16)
  private var unicodeTrans: Array[mutable.HashMap[Int, Int]] = new Array(16)
  private var stateInfo: Array[StateInfo] = new Array(16)
  private var stateCount: Int = 0

  private val initialId = registerState(initial.toVector)
  warmupAsciiDfa()

  /** Eagerly enumerate all ASCII-reachable states so the hot loop never falls into the miss branch. */
  private def warmupAsciiDfa(): Unit =
    val queue = mutable.Queue.empty[Int]
    queue.enqueue(initialId)
    val visited = mutable.HashSet.empty[Int]
    visited += initialId
    while queue.nonEmpty do
      val sid = queue.dequeue()
      if !stateInfo(sid).isDead then
        var c = 0
        while c < 128 do
          val next = asciiTransition(sid, c)
          if !visited.contains(next) then
            visited += next
            queue.enqueue(next)
          c += 1

  private def grow(): Unit =
    val n = asciiTrans.length * 2
    idToState = java.util.Arrays.copyOf(idToState, n)
    asciiTrans = java.util.Arrays.copyOf(asciiTrans, n)
    unicodeTrans = java.util.Arrays.copyOf(unicodeTrans, n)
    val newInfo = new Array[StateInfo](n)
    System.arraycopy(stateInfo, 0, newInfo, 0, stateCount)
    stateInfo = newInfo

  private def registerState(s: Vector[Subset]): Int =
    stateToId.get(s) match
      case Some(id) => id
      case None =>
        if stateCount >= asciiTrans.length then grow()
        val id = stateCount
        stateToId(s) = id
        idToState(id) = s
        val arr = new Array[Int](128)
        java.util.Arrays.fill(arr, -1)
        asciiTrans(id) = arr
        unicodeTrans(id) = mutable.HashMap.empty
        stateInfo(id) = infoFor(s)
        stateCount += 1
        id

  private def infoFor(s: Vector[Subset]): StateInfo =
    if s.forall(_ == Subset.empty) then StateInfo.Dead
    else
      val p = firstNullablePriority(s)
      if p >= 0 then StateInfo.Accept(p) else StateInfo.Live

  private def transition(fromId: Int, c: Int): Int =
    if c < 128 then asciiTransition(fromId, c)
    else unicodeTrans(fromId).getOrElseUpdate(c, registerState(idToState(fromId).map(_.derive(c))))

  private def asciiTransition(fromId: Int, c: Int): Int =
    val arr = asciiTrans(fromId)
    val cached = arr(c)
    if cached >= 0 then cached
    else
      val nextId = registerState(idToState(fromId).map(_.derive(c)))
      asciiTrans(fromId)(c) = nextId
      nextId

  /**
   * Match a token starting at `start` in `input`.
   *
   * @return `Some((priority, end))` where `priority` is the index of the winning
   *         pattern and `end` is the exclusive end of the longest match. `None` if
   *         no pattern matches any (possibly empty) prefix at `start`.
   */
  def matchAt(input: CharSequence, start: Int): Option[(priority: Int, end: Int)] =
    var sid = initialId
    var info: StateInfo = stateInfo(sid)
    var lastPriority = if info.isAccepting then info.priority else -1
    var lastEnd = start
    var i = start
    val len = input.length
    while i < len && !info.isDead do
      val ch = input.charAt(i)
      if ch < 128 then
        sid = asciiTransition(sid, ch.toInt)
        i += 1
      else
        val c = Character.codePointAt(input, i)
        sid = transition(sid, c)
        i += Character.charCount(c)
      info = stateInfo(sid)
      if info.isAccepting then
        lastPriority = info.priority
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

/**
 * Compact per-state status carried alongside the transition table.
 *
 * Encoded as a single [[Int]]:
 *   - `Int.MinValue` — dead state (no future transition can reach an accept)
 *   - `-1`           — live, not currently accepting
 *   - `>= 0`         — accepting; the value is the winning pattern's priority index
 */
opaque private[regex] type StateInfo = Int

private[regex] object StateInfo:
  val Dead: StateInfo = Int.MinValue
  val Live: StateInfo = -1
  def Accept(priority: Int): StateInfo =
    require(priority >= 0, s"accept priority must be non-negative, got $priority")
    priority

  extension (s: StateInfo)
    inline def isDead: Boolean = s == Int.MinValue
    inline def isAccepting: Boolean = s >= 0
    inline def priority: Int = s

object TokenMatcher:

  /** Build a matcher from pre-parsed regexes (use this from macros after compile-time parsing). */
  def fromRegexes(initial: Regex*): TokenMatcher =
    TokenMatcher(initial.iterator.map(Subset.of).toArray)
