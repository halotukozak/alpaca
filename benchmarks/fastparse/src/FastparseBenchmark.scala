package bench.fastparse

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import java.nio.file.{Files, Paths}
import scala.compiletime.uninitialized

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
class FastparseBenchmark:

  @Param(Array(
    "iterative_math", "recursive_math",
    "iterative_json", "recursive_json",
    "big_grammar",
  ))
  var scenario: String = uninitialized

  @Param(Array("100", "500", "1000", "2000", "5000", "10000"))
  var size: String = uninitialized

  // Resolved at setup time based on scenario
  private var input: String = uninitialized
  private var currentParser: Parser[?] = uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    // Try multiple base directories: forkWorkingDir may point to benchmarks/ or project root
    val candidates = Seq(
      Paths.get(s"inputs/${scenario}_${size}.txt"),                     // benchmarks/ as cwd
      Paths.get(s"benchmarks/inputs/${scenario}_${size}.txt"),          // project root as cwd
    )
    val inputPath = candidates.find(Files.exists(_))
      .getOrElse(sys.error(s"Input file not found for $scenario/$size. Tried: ${candidates.mkString(", ")}"))
    input = new String(Files.readAllBytes(inputPath))
    currentParser = scenario match
      case s if s.contains("math") => MathParser
      case s if s.contains("json") => JsonParser
      case "big_grammar"           => BigGrammarParser

  @Benchmark
  def fullParse(bh: Blackhole): Unit =
    try
      bh.consume(currentParser.parse(input))
    catch
      case _: StackOverflowError => bh.consume("StackOverflowError")
