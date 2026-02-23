import java.nio.file.{Files, Paths}
import java.nio.file.StandardOpenOption.*

import BenchmarkUtils.*

object Benchmark {
  def benchmarkFullParse[T](
      parser: Parser[T],
      content: String,
  ): Double = timed() {
    parser.parse(content)
  }

  def main(args: Array[String]): Unit = {
    List(
      ("iterative_math", MathParser),
      ("recursive_math", MathParser),
      ("iterative_json", JsonParser),
      ("recursive_json", JsonParser)
    ).foreach { case (groupName, parser) =>
      val resultFilePath = Paths.get(s"outputs/fastparse_${groupName}.csv")
      Files.write(
        resultFilePath,
        "Size,Full Parse Time,Full Parse Iterations\n".getBytes
      )

      TestSizes.foreach { size =>
        val inputFilePath = Paths.get(s"inputs/${groupName}_$size.txt")
        val input = new String(Files.readAllBytes(inputFilePath))

        Files.write(resultFilePath, s"$size".getBytes, APPEND)
        safely(resultFilePath) { benchmarkFullParse(parser, input) }
        Files.write(resultFilePath, "\n".getBytes, APPEND)

        println(s"${groupName}_$size benchmark completed.")
      }
    }
  }
}
