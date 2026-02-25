package bench.alpaca

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import java.nio.file.{Files, Paths}
import scala.compiletime.uninitialized

import alpaca.*

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
class AlpacaBenchmark:

  @Param(Array(
    "iterative_math", "recursive_math",
    "iterative_json", "recursive_json",
    "big_grammar",
  ))
  var scenario: String = uninitialized

  @Param(Array("100", "500", "1000", "2000", "5000", "10000"))
  var size: String = uninitialized

  // Input text loaded from file
  private var input: String = uninitialized

  // Pre-built closures that call the correct lexer/parser for this scenario.
  // This avoids path-dependent type issues with Tokenization#Lexeme.
  private var lexFn: String => Any = uninitialized
  private var parseFn: String => Any = uninitialized
  private var fullParseFn: String => Any = uninitialized

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

    scenario match
      case s if s.contains("math") =>
        lexFn = (in: String) => MathLexer.tokenize(in)
        parseFn = (in: String) =>
          val (_, tokens) = MathLexer.tokenize(in)
          MathParser.parse(tokens)
        fullParseFn = (in: String) =>
          val (_, tokens) = MathLexer.tokenize(in)
          MathParser.parse(tokens)

      case s if s.contains("json") =>
        lexFn = (in: String) => JsonLexer.tokenize(in)
        parseFn = (in: String) =>
          val (_, tokens) = JsonLexer.tokenize(in)
          JsonParser.parse(tokens)
        fullParseFn = (in: String) =>
          val (_, tokens) = JsonLexer.tokenize(in)
          JsonParser.parse(tokens)

      case "big_grammar" =>
        lexFn = (in: String) => BigGrammarLexer.tokenize(in)
        parseFn = (in: String) =>
          val (_, tokens) = BigGrammarLexer.tokenize(in)
          BigGrammarParser.parse(tokens)
        fullParseFn = (in: String) =>
          val (_, tokens) = BigGrammarLexer.tokenize(in)
          BigGrammarParser.parse(tokens)

  @Benchmark
  def lex(bh: Blackhole): Unit =
    try
      bh.consume(lexFn(input))
    catch
      case _: StackOverflowError => bh.consume("StackOverflowError")

  @Benchmark
  def parseOnly(bh: Blackhole): Unit =
    try
      bh.consume(parseFn(input))
    catch
      case _: StackOverflowError => bh.consume("StackOverflowError")

  @Benchmark
  def fullParse(bh: Blackhole): Unit =
    try
      bh.consume(fullParseFn(input))
    catch
      case _: StackOverflowError => bh.consume("StackOverflowError")
