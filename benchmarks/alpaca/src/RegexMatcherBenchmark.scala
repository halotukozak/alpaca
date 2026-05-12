package alpaca.internal.lexer.regex

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import scala.compiletime.uninitialized

/**
 * Compares the JDK `java.util.regex.Pattern` matcher (used by the lexer before
 * the Native milestone) against the internal Brzozowski-derivative [[TokenMatcher]].
 *
 * Models the lexer's tokenization hot loop: at each position, find the longest
 * prefix that matches one of the alternation branches; on no match, skip one char.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
class RegexMatcherBenchmark:

  private val patterns = List(
    " ",
    "\\t",
    "[a-zA-Z_][a-zA-Z0-9_]*",
    "\\+",
    "-",
    "\\*",
    "/",
    "=",
    ",",
    "\\(",
    "\\)",
    "\\d+",
    "#.*",
    "\n+",
  )

  @Param(Array("100", "1000", "10000"))
  var inputSize: Int = uninitialized

  private var input: String = uninitialized
  private var javaPattern: Pattern = uninitialized
  private var matcher: TokenMatcher = uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    val combined = patterns.zipWithIndex.map((p, i) => s"(?<g$i>$p)").mkString("|")
    javaPattern = Pattern.compile(combined)
    val regexes = patterns.map: p =>
      RegexParser.parse(p) match
        case Right(r) => r
        case Left(err) => sys.error(s"parse failed for /$p/: $err")
    matcher = TokenMatcher.fromRegexes(regexes*)

    val sample = "foo = bar + 42 * (x - y) / z\n"
    val builder = new StringBuilder(inputSize + sample.length)
    while builder.length < inputSize do builder.append(sample)
    input = builder.substring(0, inputSize)

  @Benchmark
  def javaRegex(bh: Blackhole): Unit =
    val m = javaPattern.matcher(input)
    var pos = 0
    while pos < input.length do
      m.region(pos, input.length)
      if m.lookingAt() then
        val end = m.end()
        bh.consume(end)
        pos = if end > pos then end else pos + 1
      else pos += 1

  @Benchmark
  def tokenMatcher(bh: Blackhole): Unit =
    var pos = 0
    while pos < input.length do
      matcher.matchAt(input, pos) match
        case Some((_, end)) if end > pos =>
          bh.consume(end)
          pos = end
        case _ => pos += 1
