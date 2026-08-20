package alpaca
package internal
package lexer
package regex

import alpaca.internal.lexer.regex.Regex.*

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Conformance cases adapted from other regex engines' own test suites, to cross-check this
 * parser's syntax/escape/algebra semantics against real-world references. Only the interesting
 * pattern/expectation data is adapted here (translated to this codebase's API) — not the
 * original test code itself.
 *
 * Cases exercising features this engine intentionally doesn't support (POSIX/Unicode property
 * classes, lookaround, backreferences, named groups, in-class `&&` intersection, or a
 * Matcher-style find/replace/split API this engine never had) are `ignore`d with a `TODO`
 * rather than deleted, so the gap stays visible instead of silently disappearing.
 */
final class RegexConformanceTest extends AnyFunSuite with Matchers:

  private def parse(pattern: String): Regex = RegexParser.parse(pattern) match
    case Right(r) => r
    case Left(err) => fail(s"expected successful parse of /$pattern/, got $err")

  private def subsetOf(pattern: String): Subset = Subset.parse(pattern) match
    case Right(s) => s
    case Left(err) => fail(s"expected successful parse of /$pattern/, got $err")

  private def codePointsOf(s: String): List[Int] = s.codePoints().toArray.toList

  /** Whole-string acceptance, i.e. `java.util.regex.Pattern.matches` semantics (not `find`). */
  private def matches(pattern: String, s: String): Boolean =
    codePointsOf(s).foldLeft(subsetOf(pattern))((acc, cp) => acc.derive(cp)).nullable

  private def isSubset(a: String, b: String): Boolean = subsetOf(a).subset(subsetOf(b))

  private def isProperSubset(a: String, b: String): Boolean = subsetOf(a).properSubset(subsetOf(b))

  private def doIntersect(a: String, b: String): Boolean = !Subset.of(parse(a) & parse(b)).isEmpty

  private def equiv(a: String, b: String): Boolean =
    val (sa, sb) = (subsetOf(a), subsetOf(b))
    sa.subset(sb) && sb.subset(sa)

  private def equivRegex(a: Regex, b: Regex): Boolean =
    val (sa, sb) = (Subset.of(a), Subset.of(b))
    sa.subset(sb) && sb.subset(sa)

  // ---------------------------------------------------------------------------------------
  // Adapted from OpenJDK's java.util.regex.RegExTest
  // https://github.com/openjdk/jdk/blob/master/test/jdk/java/util/regex/RegExTest.java
  // ---------------------------------------------------------------------------------------

  test("jdk: octal escapes (octalTest)") {
    matches("\\u0007", "") shouldBe true
    matches("\\07", "") shouldBe true
    matches("\\007", "") shouldBe true
    matches("\\0007", "") shouldBe true
    matches("\\040", " ") shouldBe true
    matches("\\0403", " 3") shouldBe true
    matches("\\0103", "C") shouldBe true
  }

  test("jdk: basic hex/octal/unicode escapes agree (escapes)") {
    matches("\\043", "#") shouldBe true
    matches("\\x23", "#") shouldBe true
    matches("\\u0023", "#") shouldBe true
  }

  test("jdk: \\Q...\\E terminates on the first literal \\E; backslashes inside are not special (escapedSegmentTest)") {
    parse("\\Qdir1\\dir2\\E") shouldBe Regex.literal("dir1\\dir2")
    parse("\\Qdir1\\dir2\\\\E") shouldBe Regex.literal("dir1\\dir2\\")
  }

  test("jdk: \\x{h...h} addresses the full code point range, incl. supplementary plane (unicodeHexNotationTest)") {
    // no ^/$ anchors: this engine's `matches` is always whole-string, like the JDK original's
    // intent, but ^/$ themselves are unsupported anchor syntax here
    matches("\\x{1033c}", "𐌼") shouldBe true
  }

  test("jdk: \\xhh is a raw BMP code unit, not a UTF-8 byte decoder (unicodeHexNotationTest)") {
    // 4 separate \xHH escapes must NOT combine into the one supplementary code point they'd
    // decode to as UTF-8 bytes (0xF0 0x90 0x8C 0xBC -> U+1033C)
    matches("\\xF0\\x90\\x8C\\xBC", "𐌼") shouldBe false
  }

  test("jdk: rejects malformed \\x{...} escapes (unicodeHexNotationTest)") {
    RegexParser.parse("\\x{-23}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
    RegexParser.parse("\\x{}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
    RegexParser.parse("\\x{AB[ef]") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("jdk: rejects a bare trailing backslash (unescapedBackslash)") {
    RegexParser.parse("\\") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  test("jdk: rejects a quantifier bound too large to fit in an Int (illegalRepetitionRange)") {
    RegexParser.parse(".{4294967296}") shouldBe a[Left[RegexParseError.InvalidSyntax, ?]]
  }

  ignore(
    "jdk: in-class intersection `[a&&b]` (droppedClassesWithIntersection)" +
      " - TODO: not supported; `&&` is currently silently misparsed as literal chars instead of" +
      " being rejected or computing the intersection",
  ) {
    parse("[A-Z&&[A-Z]0-9]") shouldBe Regex.range('A', 'Z')
  }

  ignore(
    "jdk: POSIX/Unicode property classes \\p{Lower} etc. (unicodeClassesTest)" +
      " - TODO: unsupported, parser rejects \\p/\\P with UnsupportedFeature",
  ) {
    parse("\\p{Lower}") shouldBe Regex.range('a', 'z')
  }

  // ---------------------------------------------------------------------------------------
  // Adapted from Kotlin's kotlin.text.Regex test suite
  // https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/test/text/RegexTest.kt
  //
  // Most of Kotlin's regex tests exercise a Matcher-style API this engine intentionally never
  // had (find/findAll/replace/split/capture groups/backreferences/lookaround/MULTILINE) - those
  // aren't ported since there's no equivalent surface to test here.
  // ---------------------------------------------------------------------------------------

  test("kotlin: escaping an arbitrary non-alphanumeric char (matchEscapeRandomChar)") {
    matches("\\-", "-") shouldBe true
  }

  test("kotlin: octal char value (matchCharWithOctalValue)") {
    matches("a\\0141", "aa") shouldBe true
  }

  ignore(
    "kotlin: named group + \\k<name> backreference (matchNamedGroupsWithBackReference)" +
      " - TODO: unsupported, no capture groups or backreferences at all",
  ) {
    matches("(?<title>\\w+), yes \\k<title>", "Sir, yes Sir") shouldBe true
  }

  // ---------------------------------------------------------------------------------------
  // Adapted from dregex's own test suite (marianobarrios/dregex), the DFA-based algebra library
  // this PR's Regex/RegexParser/Subset replace for cross-platform use.
  // https://github.com/marianobarrios/dregex/blob/master/src/test/java/dregex/OperationsTest.java
  // https://github.com/marianobarrios/dregex/blob/master/src/test/java/dregex/EquivalenceTest.java
  // ---------------------------------------------------------------------------------------

  test("dregex: subset relation (testSubsetBoolean)") {
    isSubset("a", ".") shouldBe true
    isSubset("", ".*") shouldBe true
    isSubset("a", "a") shouldBe true
    isSubset("(a|b){2}", "[ab][ab]") shouldBe true
    isSubset("[^a]", "[a]") shouldBe false
    isSubset("[abc]", "[ab]") shouldBe false
  }

  test("dregex: proper-subset relation (testProperSubsetBoolean)") {
    isProperSubset("a", ".") shouldBe true
    isProperSubset("", ".*") shouldBe true
    isProperSubset("[ab]+", "[ab]*") shouldBe true
    isProperSubset("[ab]", "[abcd]") shouldBe true
    isProperSubset("a", "a") shouldBe false
    isProperSubset("(a|b){2}", "[ab][ab]") shouldBe false
  }

  test("dregex: intersection non-emptiness, no lookaround needed (testIntersectionsBoolean)") {
    doIntersect("a", ".") shouldBe true
    doIntersect("a", "b") shouldBe false
    doIntersect("[^a]", "a") shouldBe false
    doIntersect("[^a]", "[a]") shouldBe false
    doIntersect("[^ab]", "[ab]") shouldBe false
    doIntersect("[^ab]", "a|b") shouldBe false
  }

  test("dregex: union results (testUnion, lookaround-free subset)") {
    equivRegex(parse("a") | parse("a"), parse("a")) shouldBe true
    equivRegex(parse("a") | parse("b"), parse("a|b")) shouldBe true
    // dregex's `.` is compiled with Pattern.DOTALL (matches everything); ours matches Java's
    // DOTALL-off default (excludes line terminators), so compare against an explicit full range
    // instead of relying on `.`
    equivRegex(parse("a") | parse("[^a]"), parse("[\\x{0}-\\x{10FFFF}]")) shouldBe true
  }

  test("dregex: intersection results (testIntersections, lookaround-free subset)") {
    equivRegex(parse("a") & parse("."), parse("a")) shouldBe true
  }

  test("dregex: quantifier equivalences (testQuantifiers)") {
    equiv("(a|b)+", "(a+|b+)+") shouldBe true
    equiv("a+", "aa*") shouldBe true
    equiv("a*a*", "a*") shouldBe true
    equiv("a?a*", "a*") shouldBe true
    equiv("(ab)+", "ab(ab)*") shouldBe true
    equiv("a", "a{1}") shouldBe true
    equiv("aa", "a{2}") shouldBe true
    equiv("aaa", "a{3}") shouldBe true
    equiv("a{0}", "") shouldBe true
    equiv("(a{2}){3}", "a{6}") shouldBe true
    equiv("(a{2}){3}", "a{5}a") shouldBe true
    equiv("(a{2}){3}", "a{5}") shouldBe false
    equiv("a{2,3}", "aaa?") shouldBe true
    equiv("a{2,3}", "a{2}a?") shouldBe true
    equiv("a{0,3}", "a{0,2}a?") shouldBe true
    equiv("a{3,}", "a{3}a*") shouldBe true
    equiv("a{3,}", "a{2}a+") shouldBe true
    equiv("a{3,}", "aaa+") shouldBe true
  }

  test("dregex: character class equivalences (testCharactedClasses)") {
    equiv("[a]", "a") shouldBe true
    equiv("a|b|c", "[abc]") shouldBe true
    equiv("[abcdef]", "[a-f]") shouldBe true
    equiv("[a-cdef]", "[a-f]") shouldBe true
    equiv("[a-cd-f]", "[a-f]") shouldBe true
  }

  test("dregex: shorthand character classes (testShortcutCharacterClasses)") {
    equiv("\\d", "[0-9]") shouldBe true
    equiv("\\w", "[a-zA-Z0-9_]") shouldBe true
    equiv("\\s", "[ \\t\\n\\r\\f\\x{B}]") shouldBe true
  }

  ignore(
    "dregex: shorthand classes nested inside a character class, e.g. `[\\d]` (testShortcutCharacterClasses)" +
      " - TODO: not supported; readClassChar rejects \\d/\\s/\\w/... inside `[...]` outright," +
      " unlike Java which allows and unions them",
  ) {
    equiv("\\d", "[\\d]") shouldBe true
  }

  test("dregex: \\Q...\\E block quotes (testBlockQuotesAndLiteralFlag)") {
    equiv("\\Q\\E", "") shouldBe true
    equiv("\\Qabc\\E", "abc") shouldBe true
    equiv("\\Qa*\\E", "a\\*") shouldBe true
    equiv("(\\Qa*\\E)*", "(a\\*)*") shouldBe true
    equiv("\\Q(\\E", "\\(") shouldBe true
    equiv("\\Q)\\E", "\\)") shouldBe true
    equiv("(\\Q)a\\E)", "\\)a") shouldBe true
    equiv("\\Q|\\E", "\\|") shouldBe true
  }

  test("dregex: named group rejected as unsupported (testGrouping)") {
    RegexParser.parse("(?<name>abc)") shouldBe a[Left[RegexParseError.UnsupportedFeature, ?]]
  }

  ignore(
    "dregex: lookaround equivalences (testLookaround)" +
      " - TODO: unsupported, lookaround is rejected at parse time",
  ) {
    equiv("(?!a|b)(?!c).*", "(?!a|b|c).*") shouldBe true
  }

  ignore(
    "dregex: POSIX character classes (testPosixCharacterClasses)" +
      " - TODO: unsupported, \\p{...} is rejected at parse time",
  ) {
    equiv("\\p{Lower}", "[a-z]") shouldBe true
  }
