import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption.*

object BenchmarkUtils:
  val Iterations = 10
  val WarmupIterations = 3
  val TestSizes = Seq(100, 500, 1_000, 2_000, 5_000, 10_000)

  def formatTime(seconds: Double): String =
    if seconds < 0.001 then f"${seconds * 1_000_000}%.2f µs"
    else if seconds < 1 then f"${seconds * 1_000}%.2f ms"
    else f"${seconds}%.2f s"

  def timed(iterations: Int = Iterations, warmup: Int = WarmupIterations)(block: => Unit): Double =
    for _ <- 0 until warmup do block

    val start = System.nanoTime()
    for _ <- 0 until iterations do block
    val end = System.nanoTime()

    (end - start) / 1e9

  def safely(resultFile: Path)(run: => Double): Unit =
    try
      val time = run
      val line = s",${formatTime(time)},$Iterations".getBytes
      Files.write(resultFile, line, APPEND)
    catch
      case _: StackOverflowError =>
        val line = s",StackOverflowError,0".getBytes
        Files.write(resultFile, line, APPEND)
