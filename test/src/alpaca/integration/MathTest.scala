package alpaca
package integration

import org.scalatest.funsuite.AnyFunSuite
import alpaca.lexer.lexer
import alpaca.lexer.Token
import alpaca.parser.Parser
import alpaca.parser.context.default.EmptyGlobalCtx
import alpaca.parser.Rule
import alpaca.parser.rule
import alpaca.parser.{after, before, name}
import alpaca.lexer.LazyReader
import java.nio.file.Files
import scala.util.Using
import alpaca.parser.Production as P

class MathTest extends AnyFunSuite:
  test("e2e math test") {
    val CalcLexer = lexer {
      // ignore whitespace/comments
      case _ @ "[ \t\r\n]+" => Token.Ignored
      case _ @ "#.*" => Token.Ignored

      // multi-char ops FIRST
      case "\\*\\*" => Token["exp"]
      case "//" => Token["fdiv"]

      // single-char ops
      case literal @ ("\\+" | "-" | "\\*" | "/" | "%" | "\\(" | "\\)" | "!" | ",") =>
        Token[literal.type]

      // math constants (lowercase)
      case keyword @ ("pi" | "e" | "tau" | "inf" | "nan") =>
        Token[keyword.type]

      // trig functions (lowercase)
      case keyword @ ("sin" | "cos" | "tan" | "asin" | "acos" | "atan" | "atan2" | "sinh" | "cosh" | "tanh") =>
        Token[keyword.type]

      // numbers
      case x @ "(\\d+\\.\\d*|\\.\\d+)([eE][+-]?\\d+)?" => Token["float"](x.toDouble)
      case x @ "\\d+" => Token["int"](x.toInt)
    }

    object CalcParser extends Parser[EmptyGlobalCtx] {
      val Expr: Rule[Double] = rule(
        // binary arithmetic
        { case (Expr(a), CalcLexer.`\\+`(_), Expr(b)) => a + b }: @name("plus"),
        { case (Expr(a), CalcLexer.`-`(_), Expr(b)) => a - b }: @name("minus"),
        { case (Expr(a), CalcLexer.`\\*`(_), Expr(b)) => a * b }: @name("times"),
        { case (Expr(a), CalcLexer.`/`(_), Expr(b)) => a / b }: @name("divide"),
        { case (Expr(a), CalcLexer.`%`(_), Expr(b)) => a % b }: @name("mod"),
        { case (Expr(a), CalcLexer.`fdiv`(_), Expr(b)) => math.floor(a / b) }: @name("fdiv"),
        { case (Expr(a), CalcLexer.`exp`(_), Expr(b)) => math.pow(a, b) }: @name("exp"),

        // unary operators
        { case (CalcLexer.`-`(_), Expr(a)) => -a }: @name("uminus"),
        { case (CalcLexer.`\\+`(_), Expr(a)) => +a }: @name("uplus"),

        // math constants
        { case CalcLexer.pi(_) => math.Pi }: @name("const_pi"),
        { case CalcLexer.e(_) => math.E }: @name("const_e"),
        { case CalcLexer.tau(_) => 2.0 * math.Pi }: @name("const_tau"),
        { case CalcLexer.inf(_) => Double.PositiveInfinity }: @name("const_inf"),
        { case CalcLexer.nan(_) => Double.NaN }: @name("const_nan"),

        // trig functions (radians)
        { case (CalcLexer.sin(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.sin(a) }: @name("sin"),
        { case (CalcLexer.cos(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.cos(a) }: @name("cos"),
        { case (CalcLexer.tan(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.tan(a) }: @name("tan"),
        { case (CalcLexer.asin(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.asin(a) }: @name("asin"),
        { case (CalcLexer.acos(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.acos(a) }: @name("acos"),
        { case (CalcLexer.atan(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.atan(a) }: @name("atan"),
        { case (CalcLexer.atan2(_), CalcLexer.`\\(`(_), Expr(y), CalcLexer.`,`(_), Expr(x), CalcLexer.`\\)`(_)) =>
          math.atan2(y, x)
        }: @name("atan2"),
        { case (CalcLexer.sinh(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.sinh(a) }: @name("sinh"),
        { case (CalcLexer.cosh(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.cosh(a) }: @name("cosh"),
        { case (CalcLexer.tanh(_), CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => math.tanh(a) }: @name("tanh"),

        // parentheses
        { case (CalcLexer.`\\(`(_), Expr(a), CalcLexer.`\\)`(_)) => a },

        // literals
        { case CalcLexer.float(x) => x.value },
        { case CalcLexer.int(n) => n.value.toDouble },
      )
      val root: Rule[Double] = rule { case Expr(v) => v }

      override val resolutions = Set(
        CalcLexer.exp.before(
          P.ofName("uplus"),
          P.ofName("uminus"),
          P.ofName("mod"),
          P.ofName("exp"),
          P.ofName("fdiv"),
          P.ofName("times"),
          P.ofName("divide"),
        ),
        P.ofName("exp").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("uplus").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("uminus").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("times").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("divide").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("fdiv").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("mod").before(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("plus").after(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("plus").before(CalcLexer.`\\+`, CalcLexer.`-`),
        P.ofName("minus").after(CalcLexer.`\\*`, CalcLexer.`/`, CalcLexer.fdiv, CalcLexer.`%`),
        P.ofName("minus").before(CalcLexer.`\\+`, CalcLexer.`-`),
      )
    }

    val input = "1 + 2"
    val tokens = CalcLexer.tokenize(input)
    val result = CalcParser.parse[Double](tokens)
    assert(result.result == 3.0)

    val tempFile = Files.createTempFile("test", ".txt")
    try {
      Files.write(
        tempFile,
        """
      (12 + 7) * (3 - 8 / (4 + 2)) + (15 - (9 - 3 * (2 + 1))) / 5) 
      * ((6 * (2 + 3) - (4 - 7) * (8 / 2)) + (9 + (10 - 4) * (3 + 2) / (6 - 1))) 
      - (24 / (3 + 1) * (7 - 5) + ((9 - 2 * (3 + 1)) * (8 / 4 - (6 - 2)))) 
      + (11 * (2 + (5 - 3) * (9 - (8 / (4 - 2)))) - ((13 - 7) / (5 + 1) * (2 * 3 - 4)))
    """.getBytes(),
      )

      Using(LazyReader.from(tempFile)) { input2 =>
        val tokens2 = CalcLexer.tokenize(input2)
        val result2 = CalcParser.parse[Double](tokens2)
        assert(result2.result == 2096.0)
      }
    } finally {
      Files.deleteIfExists(tempFile)
    }

    val tempFile2 = Files.createTempFile("test", ".txt")
    try {
      Files.write(
        tempFile2,
        """
      +sin(pi/6) + cos(pi/3) + tan(pi/4)
      + (2 ** 3 ** 2) / (3 + 1)
      + ((7 // 3) * 5 + (10 % 4))
      - (-cos(0) + +sin(pi/2))
      + (((12 / 5) + (20 // 3) - (17 % 5)) * ((3 + 2) ** 3 / (2 ** 3)))
      + atan2(1, 0)
    """.getBytes(),
      )

      Using(LazyReader.from(tempFile2)) { input2 =>
        val tokens2 = CalcLexer.tokenize(input2)
        val result2 = CalcParser.parse[Double](tokens2)
        val expected = 2.0 + 128.0 + 12.0 + 0.0 + 100.0 + (math.Pi / 2.0)

        assert(result2.result == expected, s"Multiple expression mismatch: $result2 vs $expected")
      }
    } finally {
      Files.deleteIfExists(tempFile2)
    }
  }
