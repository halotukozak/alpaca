 package alpaca
 package integration

 import org.scalatest.funsuite.AnyFunSuite

 @main def main(): Unit = {
   val CalcLexer = lexer {
     case num: "[0-9]+" => Token["NUM"](num.toDouble)
     case '+' => Token["PLUS"]
     case '-' => Token["MINUS"]
     case '*' => Token["STAR"]
     case '/' => Token["SLASH"]
     case '(' => Token["LP"]
     case ')' => Token["RP"]
     case """[ \t\r\n]+""" => Token.Ignored
   }

   object CalcParser extends Parser {
     val root: Rule[Double] = rule { case Expr(e) => e }

     val Expr: Rule[Double] = rule(
       { case (Expr(a), CalcLexer.PLUS(_), Term(b)) => a + b },
       { case (Expr(a), CalcLexer.MINUS(_), Term(b)) => a - b },
       { case Term(t) => t },
     )

     val Term: Rule[Double] = rule(
       { case (Term(a), CalcLexer.STAR(_), Factor(b)) => a * b },
       { case (Term(a), CalcLexer.SLASH(_), Factor(b)) => a / b },
       { case Factor(f) => f },
     )

     val Factor: Rule[Double] = rule(
       { case CalcLexer.NUM(n) => n.value },
       { case (CalcLexer.LP(_), Expr(e), CalcLexer.RP(_)) => e },
     )
   }

   val input = "1 + 2"
   val (_, lexemes) = CalcLexer.tokenize(input)
   val (_, result) = CalcParser.parse(lexemes)
   assert(result == 3.0)

   withLazyReader("""
         ((12 + 7) * (3 - 8 / (4 + 2)) + (15 - (9 - 3 * (2 + 1))) / 5)
         * ((6 * (2 + 3) - (4 - 7) * (8 / 2)) + (9 + (10 - 4) * (3 + 2) / (6 - 1)))
         - (24 / (3 + 1) * (7 - 5) + ((9 - 2 * (3 + 1)) * (8 / 4 - (6 - 2))))
         + (11 * (2 + (5 - 3) * (9 - (8 / (4 - 2)))) - ((13 - 7) / (5 + 1) * (2 * 3 - 4)))
       """) { input2 =>
     val (_, lexemes) = CalcLexer.tokenize(input2)
     val (_, result) = CalcParser.parse(lexemes)
     assert(result == 2096.0)
   }

 }

 class MainTest extends AnyFunSuite:
   test("e2e main test") {
     main()
   }
