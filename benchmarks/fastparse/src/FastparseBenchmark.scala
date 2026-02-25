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
    val fileName = s"${scenario}_${size}.txt"
    // Walk up from CWD to find benchmarks/inputs/ — JMH fork CWD varies by Mill version
    val cwd = Paths.get("").toAbsolutePath
    val candidates = Iterator.iterate(cwd)(_.getParent).takeWhile(_ != null).take(5).flatMap { dir =>
      Seq(dir.resolve(s"inputs/$fileName"), dir.resolve(s"benchmarks/inputs/$fileName"))
    }.toSeq
    val inputPath = candidates.find(Files.exists(_))
      .getOrElse(sys.error(s"Input file not found for $scenario/$size. CWD=$cwd, tried: ${candidates.mkString(", ")}"))
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
