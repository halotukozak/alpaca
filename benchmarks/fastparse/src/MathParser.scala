import fastparse._
import MultiLineWhitespace._

object MathParser extends Parser[Int] {
  def eval(tree: (Int, Seq[(String, Int)])) = {
    val (base, ops) = tree
    ops.foldLeft(base) { case (left, (op, right)) =>
      op match {
        case "+" => left + right
        case "-" => left - right
        case "*" => left * right
        case "/" => left / right
      }
    }
  }

  def number[$: P]: P[Int] = P(CharIn("0-9").rep(1).!.map(_.toInt))
  def parens[$: P]: P[Int] = P("(" ~/ addSub ~ ")")
  def factor[$: P]: P[Int] = P(number | parens)

  def divMul[$: P]: P[Int] =
    P(factor ~ (CharIn("*/").! ~/ factor).rep).map(eval)

  def addSub[$: P]: P[Int] =
    P(divMul ~ (CharIn("+\\-").! ~/ divMul).rep).map(eval)

  def expr[$: P]: P[Int] = P(addSub ~ End)

  def parse(input: String): Either[String, Int] = {
    fastparse.parse(input, expr(using _)) match {
      case Parsed.Success(value, _) => Right(value)
      case f: Parsed.Failure        => Left(f.msg)
    }
  }
}

object MathParserMain extends App {
  import java.nio.file.{Files, Paths}

  val filePathIterative = s"../inputs/iterative_math_3.txt"
  val fileContentIterative = new String(
    Files.readAllBytes(Paths.get(filePathIterative))
  )

  MathParser.parse(fileContentIterative) match {
    case Right(result) => println(s"\nResult Iterative: $result")
    case Left(error)   => println(s"\nError Iterative: $error")
  }

  val filePathRecursive = s"../inputs/recursive_math_3.txt"
  val fileContentRecursive = new String(
    Files.readAllBytes(Paths.get(filePathRecursive))
  )

  MathParser.parse(fileContentRecursive) match {
    case Right(result) => println(s"\nResult Recursive: $result")
    case Left(error)   => println(s"\nError Recursive: $error")
  }
}
