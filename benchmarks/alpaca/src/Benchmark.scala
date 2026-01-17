import java.nio.file.{Files, Paths, Path}
import scala.util.{Try, Success, Failure}
import java.nio.file.StandardOpenOption.*

import alpaca.*
import alpaca.internal.lexer.Tokenization

object Benchmark {
  val iterations = 10
  
  def safely(resultFile: Path)(run: => Double): Unit =
    try {
      val time = run
      val line = s",${formatTime(time)},$iterations".getBytes
      Files.write(resultFile, line, APPEND)
    } catch {
      case _: StackOverflowError =>
        val line = s",StackOverflowError,0".getBytes
        Files.write(resultFile, line, APPEND)
    }

  def formatTime(seconds: Double): String = {
    if (seconds < 0.001) f"${seconds * 1_000_000}%.2f µs"
    else if (seconds < 1) f"${seconds * 1_000}%.2f ms"
    else f"${seconds}%.2f s"
  }

  def benchmarkLexer(
      lexer: Tokenization[LexerCtx.Default],
      content: String,
      iterations: Int = Benchmark.iterations
  ): Double = {
    // Warmup
    for (_ <- 0 until 3) {
      lexer.tokenize(content)
    }

    // Benchmark
    val start = System.nanoTime()
    for (_ <- 0 until iterations) {
      lexer.tokenize(content)
    }
    val end = System.nanoTime()

    (end - start) / 1e9
  }

  def benchmarkParser(
      lexer: Tokenization[LexerCtx.Default],
      parser: Parser[ParserCtx.Empty],
      content: String,
      iterations: Int = Benchmark.iterations
  ): Double = {
    val (_, tokens) = lexer.tokenize(content)

    // Warmup
    for (_ <- 0 until 3) {
      parser.parse(tokens)
    }

    // Benchmark
    val start = System.nanoTime()
    for (_ <- 0 until iterations) {
      parser.parse(tokens)
    }
    val end = System.nanoTime()

    (end - start) / 1e9
  }

  def benchmarkFullParse(
      lexer: Tokenization[LexerCtx.Default],
      parser: Parser[ParserCtx.Empty],
      content: String,
      iterations: Int = Benchmark.iterations
  ): Double = {
    // Warmup
    for (_ <- 0 until 3) {
      val (_, tokens) = lexer.tokenize(content)
      parser.parse(tokens)
    }

    // Benchmark
    val start = System.nanoTime()
    for (_ <- 0 until iterations) {
      val (_, tokens) = lexer.tokenize(content)
      parser.parse(tokens)
    }
    val end = System.nanoTime()

    (end - start) / 1e9
  }

  def main(args: Array[String]): Unit = {
    List(
      ("iterative_math", MathLexer, MathParser),
      ("recursive_math", MathLexer, MathParser),
      ("iterative_json", JsonLexer, JsonParser),
      ("recursive_json", JsonLexer, JsonParser)
    ).foreach { case (groupName, lexer, parser) =>
      val resultFilePath = Paths.get(s"../outputs/alpaca_${groupName}.csv")
      Files.write(
        resultFilePath,
        "Size,Lex Time,Lex Iterations,Parse Time,Parse Iterations,Full Parse Time,Full Parse Iterations\n".getBytes
      )

      List(100, 500, 1_000, 2_000).foreach { size =>
        val inputFilePath = Paths.get(s"../inputs/${groupName}_$size.txt")
        val input = new String(Files.readAllBytes(inputFilePath))
        Files.write(resultFilePath, s"$size".getBytes, APPEND)

        safely(resultFilePath) { benchmarkLexer(lexer, input) }
        safely(resultFilePath) { benchmarkParser(lexer, parser, input) }
        safely(resultFilePath) { benchmarkFullParse(lexer, parser, input) }

        Files.write(resultFilePath, "\n".getBytes, APPEND)
        println(s"${groupName}_$size benchmark completed.")
      }
    }
  }
}
