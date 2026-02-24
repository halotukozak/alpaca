package alpaca.benchmark

import alpaca.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import annotation.nowarn

private val JsonLexer = lexer:
  case "\\s+" => Token.Ignored
  case "\\{" => Token["{"]
  case "\\}" => Token["}"]
  case "\\[" => Token["["]
  case "\\]" => Token["]"]
  case ":" => Token[":"]
  case "," => Token[","]
  case x @ ("false" | "true") => Token["Bool"](x.toBoolean)
  case "null" =>  Token["Null"](null: @nowarn("msg=unused explicit parameter")) // todo: why needs @nowarn?
  case x @ """[-+]?\d+(\.\d+)?""" => Token["Number"](x.toDouble)
  case x @ """"(\\.|[^"])*"""" => Token["String"](x.slice(1, x.length - 1))

private object JsonParser extends Parser:
  val root: Rule[Any] = rule:
    case Value(value) => value

  val Value: Rule[Any] = rule(
    { case JsonLexer.Null(n) => n.value },
    { case JsonLexer.Bool(b) => b.value },
    { case JsonLexer.Number(n) => n.value },
    { case JsonLexer.String(s) => s.value },
    { case Object(obj) => obj },
    { case Array(arr) => arr },
  )

  val Object: Rule[Map[String, Any]] = rule(
    { case (JsonLexer.`{`(_), JsonLexer.`}`(_)) => Map.empty[String, Any] },
    { case (JsonLexer.`{`(_), ObjectMembers(members), JsonLexer.`}`(_)) => members.toMap },
  )

  val ObjectMembers: Rule[List[(String, Any)]] = rule(
    { case ObjectMember(member) => scala.List(member) },
    { case (ObjectMembers(members), JsonLexer.`,`(_), ObjectMember(member)) => members :+ member },
  )

  val ObjectMember: Rule[(String, Any)] = rule:
    case (JsonLexer.String(s), JsonLexer.`:`(_), Value(v)) => (s.value, v)

  val Array: Rule[List[Any]] = rule(
    { case (JsonLexer.`[`(_), JsonLexer.`]`(_)) => Nil },
    { case (JsonLexer.`[`(_), ArrayElements(elems), JsonLexer.`]`(_)) => elems },
  )

  val ArrayElements: Rule[List[Any]] = rule(
    { case Value(v) => scala.List(v) },
    { case (ArrayElements(elems), JsonLexer.`,`(_), Value(v)) => elems :+ v },
  )

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class RuntimeBenchmark:

  val simpleJson = """{"name": "Alice", "age": 30, "active": true}"""

  val complexJson =
    """{
      "menu": {
        "id": "file",
        "value": "File",
        "popup": {
          "menuitem": [
            {"value": "New", "onclick": "CreateNewDoc()"},
            {"value": "Open", "onclick": "OpenDoc()"},
            {"value": "Close", "onclick": "CloseDoc()"}
          ]
        }
      }
    }"""

  @Benchmark
  def tokenizeSimpleJson(): Int =
    val (_, lexemes) = JsonLexer.tokenize(simpleJson)
    lexemes.size

  @Benchmark
  def tokenizeComplexJson(): Int =
    val (_, lexemes) = JsonLexer.tokenize(complexJson)
    lexemes.size

  @Benchmark
  def parseSimpleJson(): Any =
    val (_, lexemes) = JsonLexer.tokenize(simpleJson)
    val (_, result) = JsonParser.parse(lexemes)
    result

  @Benchmark
  def parseComplexJson(): Any =
    val (_, lexemes) = JsonLexer.tokenize(complexJson)
    val (_, result) = JsonParser.parse(lexemes)
    result
