package alpaca
package internal
package lexer
package regex

/**
 * Lexer-style longest-match tokenizer over a priority-ordered list of patterns.
 *
 * Simulates a tagged Brzozowski-derivative DFA in parallel for all patterns. Returns
 * the longest prefix match across all patterns; ties broken by lowest priority index
 * (i.e., the first pattern in the input list wins).
 */
opaque private[lexer] type TokenMatcher = Vector[Subset]

private[lexer] object TokenMatcher:

  /** Build a matcher from pre-parsed regexes (use this from macros after compile-time parsing). */
  def fromRegexes(initial: Regex*): TokenMatcher = initial.iterator.map(Subset.of).toVector

  extension (m: TokenMatcher)

    /**
     * Match a token starting at `start` in `input`.
     *
     * @return `Some((priority, end))` where `priority` is the index of the winning
     *         pattern and `end` is the exclusive end of the longest match. `None` if
     *         no pattern matches any (possibly empty) prefix at `start`.
     */
    def matchAt(input: CharSequence, start: Int): Option[(priority: Int, end: Int)] =
      codePointStarts(input, start)
        .map(p => (Character.codePointAt(input, p), p))
        .scanLeft((m, start)):
          case ((st, _), (c, p)) => (st.map(_.derive(c)), p + Character.charCount(c))
        .takeWhile((st, _) => !st.allEmpty)
        .flatMap((st, p) => st.firstNullable.map(idx => (priority = idx, end = p)))
        .foldLeft(Option.empty[(priority: Int, end: Int)])((_, hit) => Some(hit))

    /** First position `>= from` at which some pattern matches a non-empty prefix. */
    def findFirst(input: CharSequence, from: Int): Option[(start: Int, priority: Int, end: Int)] =
      codePointStarts(input, from)
        .map(start => (start, m.matchAt(input, start)))
        .collectFirst:
          case (start, Some((priority, end))) if end > start => (start, priority, end)

  /** Iterator of code-point start offsets in `input`, beginning at `from`. */
  private def codePointStarts(input: CharSequence, from: Int): Iterator[Int] =
    Iterator
      .iterate(from)(p => p + Character.charCount(Character.codePointAt(input, p)))
      .takeWhile(_ < input.length)

  extension (s: Vector[Subset])
    private def firstNullable: Option[Int] = s.iterator.zipWithIndex.collectFirst:
      case (sub, idx) if sub.nullable => idx

    private def allEmpty: Boolean = s.forall(_ == Subset.empty)
