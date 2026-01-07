val MathLexer = lexer:
  case "\\s+" => Token.Ignored
  case operator @ ("\\+" | "-" | "\\*" | "/" | "\\(" | "\\)") => Token[operator.type]
  case num @ "\\d+" => Token["num"](num.toInt)

object MathParser extends Parser:
  val Expr: Rule[Int] = rule(
    { case (Expr(a), MathLexer.`\\*`(_), Expr(b)) => a * b }: @name("mul"),
    { case (Expr(a), MathLexer.`/`(_), Expr(b)) => a / b }: @name("div"),
    { case (Expr(a), MathLexer.`\\+`(_), Expr(b)) => a + b }: @name("plus"),
    { case (Expr(a), MathLexer.`-`(_), Expr(b)) => a - b }: @name("minus"),
    { case (MathLexer.`\\(`(_), Expr(a), MathLexer.`\\)`(_)) => a },
    { case MathLexer.num(n) => n.value },
  )
  // Parser entry point
  val root: Rule[Int] = rule { case Expr(v) => v }

  override val resolutions = Set(
    // addition and subtraction only after multiplication and division
    Production.ofName("plus").after(MathLexer.`\\*`, MathLexer.`/`),
    Production.ofName("minus").after(MathLexer.`\\*`, MathLexer.`/`),

    // all operators are left associative (reduce before shifting)
    Production.ofName("mul").before(MathLexer.`\\*`, MathLexer.`/`),
    Production.ofName("div").before(MathLexer.`\\*`, MathLexer.`/`),
    Production.ofName("plus").before(MathLexer.`\\+`, MathLexer.`-`),
    Production.ofName("minus").before(MathLexer.`\\+`, MathLexer.`-`),
  )
