object MathParser extends Parser[Int] {
  def eval(tree: (Int, Seq[(String, Int)])) = {
    val (base, ops) = tree
    ops.foldLeft(base):
      case (left, ("+", right)) => left + right
      case (left, ("-", right)) => left - right
      case (left, ("*", right)) => left * right
      case (left, ("/", right)) => left / right
  }

  def number[$: P]: P[Int] = P(CharIn("0-9").rep(1).!.map(_.toInt))
  def parens[$: P]: P[Int] = P("(" ~/ addSub ~ ")")
  def factor[$: P]: P[Int] = P(number | parens)

  def divMul[$: P]: P[Int] =
    P(factor ~ (CharIn("*/").! ~/ factor).rep).map(eval)

  def addSub[$: P]: P[Int] =
    P(divMul ~ (CharIn("+\\-").! ~/ divMul).rep).map(eval)

  def expr[$: P]: P[Int] = P(addSub ~ End)

  def parse(input: String): Either[String, Int] =
    fastparse.parse(input, expr(using _)) match {
      case Parsed.Success(value, _) => Right(value)
      case f: Parsed.Failure => Left(f.msg)
    }
}
