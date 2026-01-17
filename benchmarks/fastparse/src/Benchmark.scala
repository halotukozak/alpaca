import java.nio.file.{Files, Paths}
import scala.util.{Try, Success, Failure}
import java.nio.file.StandardOpenOption.*

object Benchmark {
  val iterations = 10
  
  def formatTime(seconds: Double): String = {
    if (seconds < 0.001) f"${seconds * 1_000_000}%.2f µs"
    else if (seconds < 1) f"${seconds * 1_000}%.2f ms"
    else f"${seconds}%.2f s"
  }

  def benchmarkFullParse[T](
      parser: Parser[T],
      content: String,
      iterations: Int = Benchmark.iterations
  ): Double = {
    // Warmup
    for (_ <- 0 until 3) {
      parser.parse(content)
    }

    // Benchmark
    val start = System.nanoTime()
    for (_ <- 0 until iterations) {
      parser.parse(content)
    }
    val end = System.nanoTime()

    (end - start) / 1e9
  }

  def main(args: Array[String]): Unit = {
    List(
      ("iterative_math", MathParser),
      ("recursive_math", MathParser),
      ("iterative_json", JsonParser),
      ("recursive_json", JsonParser)
    ).foreach { case (groupName, parser) =>
      val resultFilePath = Paths.get(s"../outputs/fastparse_${groupName}.csv")
      Files.write(
        resultFilePath,
        "Size,Full Parse Time,Full Parse Iterations\n".getBytes
      )

      List(100, 500, 1_000, 2_000, 5_000, 10_000).foreach { size =>
        val inputFilePath = Paths.get(s"../inputs/${groupName}_$size.txt")
        val input = new String(Files.readAllBytes(inputFilePath))

        try {
          val fullParseTime = benchmarkFullParse(parser, input)
          val line = s"$size,${formatTime(fullParseTime)},$iterations\n".getBytes
          Files.write(resultFilePath, line, APPEND)
        } catch {
          case _: StackOverflowError =>
            val line = s"$size,StackOverflowError,0\n".getBytes
            Files.write(resultFilePath, line, APPEND)
        }

        println(s"${groupName}_$size benchmark completed.")
      }
    }
  }
}
