package alpaca
package integration

import org.scalatest.funsuite.AnyFunSuite

/** Validates the extractors tutorial patterns compile and run correctly. */
final class ExtractorsTutorialTest extends AnyFunSuite:
  // Lexer used across tests
  val MyLexer = lexer:
    case "\\s+" => Token.Ignored
    case "\\+" => Token["+"]
    case "-" => Token["-"]
    case "\\*" => Token["*"]
    case ":" => Token[":"]
    case "," => Token[","]
    case "\\(" => Token["("]
    case "\\)" => Token[")"]
    case keyword @ ("if" | "then" | "val") => Token[keyword.type]
    case x @ "\\d+" => Token["NUM"](x.toInt)
    case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](x)

  // Section 1: Matching terminals — basic token matching and value access
  test("basic token matching with _ and value access") {
    object P extends Parser:
      val root: Rule[Int] = rule:
        case Expr(v) => v

      val Expr: Rule[Int] = rule(
        // "To match a token without caring about its value, use _"
        "plus" { case (Expr(a), MyLexer.`+`(_), Expr(b)) => a + b },
        // "To access the value captured by the token, bind it to a variable"
        { case MyLexer.NUM(n) => n.value },
      )

      override val resolutions = Set(
        production.plus.before(MyLexer.`+`),
      )

    val (_, lexemes) = MyLexer.tokenize("1 + 2 + 3")
    val (_, result) = P.parse(lexemes)
    assert(result == 6)
  }

  // Section 2: Matching non-terminals (rules as extractors)
  test("rules act as extractors") {
    object P extends Parser:
      val root: Rule[String] = rule:
        case Stmt(s) => s

      val Expr: Rule[Int] = rule(
        "plus" { case (Expr(a), MyLexer.`+`(_), Expr(b)) => a + b },
        { case MyLexer.NUM(n) => n.value },
      )

      val Stmt: Rule[String] = rule:
        case Expr(e) => s"Expression result: $e"

      override val resolutions = Set(
        production.plus.before(MyLexer.`+`),
      )

    val (_, lexemes) = MyLexer.tokenize("1 + 2")
    val (_, result) = P.parse(lexemes)
    assert(result == "Expression result: 3")
  }

  // Section 3: EBNF extractors — .List (works on Rules, not tokens)
  test(".List extractor matches zero or more") {
    object P extends Parser:
      val root: Rule[List[Int]] = rule:
        case Num.List(ns) => ns

      val Num: Rule[Int] = rule:
        case MyLexer.NUM(n) => n.value

    val (_, lexemes) = MyLexer.tokenize("1 2 3")
    val (_, result) = P.parse(lexemes)
    assert(result == List(1, 2, 3))
  }

  // Section 3: EBNF extractors — .Option (works on Rules, not tokens)
  test(".Option extractor matches zero or one") {
    object P extends Parser:
      val root: Rule[(Int, Option[Int])] = rule:
        case (Num(a), MyLexer.`,`(_), Num.Option(b)) =>
          (a, b)

      val Num: Rule[Int] = rule:
        case MyLexer.NUM(n) => n.value

    val (_, lexemes1) = MyLexer.tokenize("1 , 2")
    val (_, result1) = P.parse(lexemes1)
    assert(result1 == (1, Some(2)))

    val (_, lexemes2) = MyLexer.tokenize("1 ,")
    val (_, result2) = P.parse(lexemes2)
    assert(result2 == (1, None))
  }

  // Section 3: EBNF extractors — .SeparatedBy (works on Rules, not tokens)
  test(".SeparatedBy extractor matches zero or more items with separators") {
    object P extends Parser:
      val root: Rule[List[Any]] = rule:
        case Num.SeparatedBy[MyLexer.`,`](items) => items

      val Num: Rule[Int] = rule:
        case MyLexer.NUM(n) => n.value

    val (_, emptyLexemes) = MyLexer.tokenize("")
    val (_, emptyResult) = P.parse(emptyLexemes)
    assert(emptyResult == Nil)

    val (_, singletonLexemes) = MyLexer.tokenize("1")
    val (_, singletonResult) = P.parse(singletonLexemes)
    assert(singletonResult == List(1))

    val (_, repeatedLexemes) = MyLexer.tokenize("1,2,3")
    val (_, repeatedResult) = P.parse(repeatedLexemes)
    assert(repeatedResult match
      case List(1, _, 2, _, 3) => true
      case _ => false)
  }

  test(".SeparatedBy inside a tuple pattern") {
    object P extends Parser:
      val root: Rule[(Int, List[Any])] = rule:
        case (MyLexer.`(`(_), Num.SeparatedBy[MyLexer.`,`](items), MyLexer.`)`(_)) =>
          (items.count(_.isInstanceOf[Int]), items)

      val Num: Rule[Int] = rule:
        case MyLexer.NUM(n) => n.value

    val (_, lexemes) = MyLexer.tokenize("(10,20,30)")
    val (_, result) = P.parse(lexemes)
    assert(result.nn._1 == 3)
    assert(result.nn._2 match
      case List(10, _, 20, _, 30) => true
      case _ => false)

    val (_, emptyLexemes) = MyLexer.tokenize("()")
    val (_, emptyResult) = P.parse(emptyLexemes)
    assert(emptyResult == (0, Nil))
  }

  test(".SeparatedBy with a Rule separator") {
    object P extends Parser:
      val root: Rule[List[Any]] = rule:
        case Num.SeparatedBy[Sep.type](items) => items

      val Num: Rule[Int] = rule:
        case MyLexer.NUM(n) => n.value

      val Sep: Rule[String] = rule:
        case MyLexer.`,`(_) => ","

    val (_, lexemes) = MyLexer.tokenize("1,2,3")
    val (_, result) = P.parse(lexemes)
    assert(result == List(1, ",", 2, ",", 3))
  }

  // Section 4: Tuple matching
  test("tuple matching for sequences") {
    object P extends Parser:
      val root: Rule[Int] = rule:
        case Expr(v) => v

      val Expr: Rule[Int] = rule(
        { case (MyLexer.`(`(_), Expr(a), MyLexer.`)`(_)) => a },
        "times" { case (Expr(a), MyLexer.`*`(_), Expr(b)) => a * b },
        { case MyLexer.NUM(n) => n.value },
      )

      override val resolutions = Set(
        production.times.before(MyLexer.`*`),
      )

    val (_, lexemes) = MyLexer.tokenize("(2 * 3)")
    val (_, result) = P.parse(lexemes)
    assert(result == 6)
  }
