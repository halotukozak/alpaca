import java.nio.file.{Files, Path, Paths}
import java.nio.file.StandardOpenOption.*

import alpaca.*
import alpaca.internal.lexer.Tokenization

import BenchmarkUtils.*

object Benchmark:
  def benchmarkLexer(
    lexer: Tokenization[LexerCtx.Default],
    content: String,
  ): Double = timed() {
    lexer.tokenize(content)
  }

  def benchmarkParser(
    lexer: Tokenization[LexerCtx.Default],
    parser: Parser[ParserCtx.Empty],
    content: String,
  ): Double =
    val (_, tokens) = lexer.tokenize(content)
    timed() {
      parser.parse(tokens)
    }

  def benchmarkFullParse(
    lexer: Tokenization[LexerCtx.Default],
    parser: Parser[ParserCtx.Empty],
    content: String,
  ): Double = timed() {
    val (_, tokens) = lexer.tokenize(content)
    parser.parse(tokens)
  }

  def main(args: Array[String]): Unit =
    List(
      ("iterative_math", MathLexer, MathParser),
      ("recursive_math", MathLexer, MathParser),
      ("iterative_json", JsonLexer, JsonParser),
      ("recursive_json", JsonLexer, JsonParser),
    ).foreach { case (groupName, lexer, parser) =>
      val resultFilePath = Paths.get(s"outputs/alpaca_$groupName.csv")
      Files.write(
        resultFilePath,
        "Size,Lex Time,Lex Iterations,Parse Time,Parse Iterations,Full Parse Time,Full Parse Iterations\n".getBytes,
      )

      TestSizes.foreach { size =>
        val inputFilePath = Paths.get(s"inputs/${groupName}_$size.txt")
        val input = new String(Files.readAllBytes(inputFilePath))
        Files.write(resultFilePath, s"$size".getBytes, APPEND)

        safely(resultFilePath)(benchmarkLexer(lexer, input))
        safely(resultFilePath)(benchmarkParser(lexer, parser, input))
        safely(resultFilePath)(benchmarkFullParse(lexer, parser, input))

        Files.write(resultFilePath, "\n".getBytes, APPEND)
        println(s"${groupName}_$size benchmark completed.")
      }
    }
