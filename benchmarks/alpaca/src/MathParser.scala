import alpaca.*

val MathLexer = lexer {
  case "\\s+"                                          => Token.Ignored
  case x @ ("\\+" | "-" | "\\*" | "/" | "\\(" | "\\)") => Token[x.type]
  case num @ "\\d+" => Token["num"](num.toInt)
}

object MathParser extends Parser {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), MathLexer.`\\*`(_), Expr(b)) => a * b }: @name("mul"),
    { case (Expr(a), MathLexer.`/`(_), Expr(b)) => a / b }: @name("div"),
    { case (Expr(a), MathLexer.`\\+`(_), Expr(b)) => a + b }: @name("plus"),
    { case (Expr(a), MathLexer.`-`(_), Expr(b)) => a - b }: @name("minus"),
    { case (MathLexer.`\\(`(_), Expr(a), MathLexer.`\\)`(_)) => a },
    { case MathLexer.num(n) => n.value }
  )

  val root: Rule[Int] = rule { case Expr(v) => v }

  override val resolutions = Set(
    // multiplication and division are left associative (reduction before shift)
    Production.ofName("mul").before(MathLexer.`\\*`, MathLexer.`/`),
    Production.ofName("div").before(MathLexer.`\\*`, MathLexer.`/`),

    // addition and subtraction have lower precedence than multiplication and division
    Production.ofName("plus").after(MathLexer.`\\*`, MathLexer.`/`),
    Production.ofName("minus").after(MathLexer.`\\*`, MathLexer.`/`),

    // addition and subtraction are left associative (reduction before shift)
    Production.ofName("plus").before(MathLexer.`\\+`, MathLexer.`-`),
    Production.ofName("minus").before(MathLexer.`\\+`, MathLexer.`-`)
  )
}

object MathParserMain extends App {
  import java.nio.file.{Files, Paths}

  val filePathIterative = s"../inputs/iterative_math_3.txt"
  val fileContentIterative = new String(
    Files.readAllBytes(Paths.get(filePathIterative))
  )

  try {
    val (_, tokens) = MathLexer.tokenize(fileContentIterative)
    val (_, result) = MathParser.parse(tokens)
    println(s"\nResult Iterative: $result")
  } catch {
    case e: Exception =>
      println(s"\nError Iterative: ${e.getMessage}")
      e.printStackTrace()
  }

  val filePathRecursive = s"../inputs/recursive_math_3.txt"
  val fileContentRecursive = new String(
    Files.readAllBytes(Paths.get(filePathRecursive))
  )

  try {
    val (_, tokens) = MathLexer.tokenize(fileContentRecursive)
    val (_, result) = MathParser.parse(tokens)
    println(s"\nResult Recursive: $result")
  } catch {
    case e: Exception =>
      println(s"\nError Recursive: ${e.getMessage}")
      e.printStackTrace()
  }
}
