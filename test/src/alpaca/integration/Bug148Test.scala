package alpaca
package integration

import alpaca.*
import org.scalatest.funsuite.AnyFunSuite

final class Bug148Test extends AnyFunSuite:
  test("multiline action in parser rule - single production") {
    val BugLexer = lexer:
      case "T" => Token["T"]

    object Bug extends Parser:
      override val root: Rule[Any] = rule:
        case BugLexer.T(_) =>
          val x = 1
          x

    val (_, lexemes) = BugLexer.tokenize("T")
    val (_, result) = Bug.parse(lexemes)
    assert(result == 1)
  }

  test("multiline action in parser rule - multiple productions") {
    val BugLexer = lexer:
      case "\\s+" => Token.Ignored
      case x @ "[0-9]+" => Token["num"](x.toInt)
      case "\\+" => Token["+"]

    object Bug extends Parser:
      val Expr: Rule[Int] = rule(
        "add" {
          case (Expr(a), BugLexer.`+`(_), Expr(b)) =>
            val sum = a + b
            sum
        },
        {
          case BugLexer.num(n) =>
            val v = n.value
            v
        },
      )
      override val root: Rule[Int] = rule:
        case Expr(v) =>
          val result = v * 2
          result

      override val resolutions = Set(
        production.add.before(BugLexer.`+`),
      )

    val (_, lexemes) = BugLexer.tokenize("1 + 2")
    val (_, result) = Bug.parse(lexemes)
    assert(result == 6) // (1 + 2) * 2
  }
