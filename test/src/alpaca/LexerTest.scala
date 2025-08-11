package alpaca

import alpaca.ctx
import org.scalatest.funsuite.AnyFunSuite

class LexerTest extends AnyFunSuite {

//  showAst {
//    given Ctx = ???
//    identity[String => Token[?]] {
//      case literal @ ("(" | ")") => Token[literal.type]
//      case "+" => Token["PLUS"]
//    }
//  }

  val CalcLexer: Tokenize = lexer {
    case comment @ "#.*" => Token.Ignored
    case newLines @ "\\n+" =>
      println("dupa")
      ctx.lineno += newLines.count(_ == '\n')
      ctx.lineno += 8
      Token.Ignored
    case literal @ ("(" | ")") => Token[literal.type]
    case "+" => Token["PLUS"]
    case "-" => Token["MINUS"]
    case "*" => Token["TIMES"]
    case "/" => Token["DIVIDE"]
    case "=" => Token["ASSIGN"]
    case "<=" => Token["LE"]
    case "<" => Token["LT"]
    case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](x.toUpperCase)
    case x @ "\\d+" => Token["NUMBER"](x.toInt)
  }

  //    def error(self, t):
  //        self.errors.append(t.value)
  //        self.index += 1
  //        if hasattr(self, 'return_error'):
  //            return t
  //
  //    def __init__(self):
  //        self.errors = []

  test("basic recognition of tokens and literals") {
    val result @ TokenizationResult(toks, errors) = CalcLexer.tokenize("abc 123 + - * / = < <= ( )")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    assert(types == List("ID", "NUMBER", "PLUS", "MINUS", "TIMES", "DIVIDE", "ASSIGN", "LT", "LE", "(", ")"))
    assert(vals == List("ABC", 123, "+", "-", "*", "/", "=", "<", "<=", "(", ")"))
  }

  test("position tracking") {
    val text = "abc\n( )"
    val result @ TokenizationResult(toks, errors) = CalcLexer.tokenize(text)
    val lines = toks.map(_.lineno)
    val indices = toks.map(_.index)
    val ends = toks.map(_.end)
    val valuesFromSlice = toks.map(t => text.slice(t.index, t.end))
    assert(valuesFromSlice == List("abc", "(", ")"))
    assert(lines == List(1, 2, 2))
    assert(indices == List(0, 4, 6))
    assert(ends == List(3, 5, 7))
  }

  test("ignored comments and newlines") {
    val result @ TokenizationResult(toks, errors) = CalcLexer.tokenize("\n\n# A comment\n123\nabc\n")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    val linenos = toks.map(_.lineno)
    assert(types == List("NUMBER", "ID"))
    assert(vals == List(123, "ABC"))
    assert(linenos == List(4, 5))
    assert(result.lineno == 6)
  }

  test("error handling without returning error token") {
    val TokenizationResult(toks, errors) = CalcLexer.tokenize("123 :+-")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    assert(types == List("NUMBER", "PLUS", "MINUS"))
    assert(vals == List(123, "+", "-"))
    assert(errors.toList == List(":+-"))
  }

  test("error token return handling") {
//    CalcLexer.returnError = true
    val result @ TokenizationResult(toks, errors) = CalcLexer.tokenize("123 :+-")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    assert(types == List("NUMBER", "ERROR", "PLUS", "MINUS"))
    assert(vals == List(123, ":+-", "+", "-"))
    assert(errors == List(":+-"))
  }

  val ModernCalcLexer: Tokenize = lexer {
    case " \t" | "#.*" => Token.Ignored
    case newLines @ "\\n+" =>
      ctx.lineno += newLines.count(_ == '\n')
      Token.Ignored
    case literal @ ("(" | ")") => Token[literal.type]
    case "+" => Token["PLUS"]
    case "-" => Token["MINUS"]
    case "*" => Token["TIMES"]
    case "/" => Token["DIVIDE"]
    case "=" => Token["ASSIGN"]
    case "<=" => Token["LE"]
    case "<" => Token["LT"]
    case "if" => Token["IF"]
    case "else" => Token["ELSE"]
    case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](x.toUpperCase)
    case x @ "\\d+" => Token["NUMBER"](x.toInt)
  }

  //    def error(self, t):
  //        self.errors.append(t.value)
  //        self.index += 1
  //        if hasattr(self, 'return_error'):
  //            return t
  //
  //    def __init__(self):
  //        self.errors = []

  test("modern lexer basic tokens with keywords") {
    val result @ TokenizationResult(toks, errors) = ModernCalcLexer.tokenize("abc if else 123 + - * / = < <= ( )")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    assert(
      types == List("ID", "IF", "ELSE", "NUMBER", "PLUS", "MINUS", "TIMES", "DIVIDE", "ASSIGN", "LT", "LE", "(", ")")
    )
    assert(vals == List("ABC", "if", "else", 123, "+", "-", "*", "/", "=", "<", "<=", "(", ")"))
  }

  test("modern lexer: ignored comments and newlines") {
    val result @ TokenizationResult(toks, errors) = ModernCalcLexer.tokenize("\n\n# A comment\n123\nabc\n")
    val types = toks.map(_.tpe)
    val vals = toks.map(_.value)
    val linenos = toks.map(_.lineno)
    assert(types == List("NUMBER", "ID"))
    assert(vals == List(123, "ABC"))
    assert(linenos == List(4, 5))
    assert(result.lineno == 6)
  }

//  test("modern lexer: error handling without returning error token") {
//    val toks = ModernCalcLexer.tokenize("123 :+-")
//    val types = toks.map(_.tpe)
//    val vals = toks.map(_.value)
//    assert(types == List("NUMBER", "PLUS", "MINUS"))
//    assert(vals == List(123, "+", "-"))
//    assert(ModernCalcLexer.errors.toList == List(":+-"))
//  }

//  test("modern lexer: error token return handling") {
//    ModernCalcLexer.returnError = true
//    val TokenizationResult(toks, errors) = ModernCalcLexer.tokenize("123 :+-")
//    val types = toks.map(_.tpe)
//    val vals = toks.map(_.value)
//    assert(types == List("NUMBER", "ERROR", "PLUS", "MINUS"))
//    assert(vals == List(123, ":+-", "+", "-"))
//    assert(ModernCalcLexer.errors.toList == List(":+-"))
//  }
}
