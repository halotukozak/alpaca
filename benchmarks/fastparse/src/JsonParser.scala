// Based on: https://com-lihaoyi.github.io/fastparse/#Json
import fastparse.*
import MultiLineWhitespace.*

object JsonParser extends Parser[JsonParser.Val]:
  sealed trait Val extends Any:
    def value: Any
    def apply(i: Int): Val = this.asInstanceOf[Arr].value(i)
    def apply(s: java.lang.String): Val =
      this.asInstanceOf[Obj].value.find(_._1 == s).get._2
  case class Str(value: java.lang.String) extends AnyVal with Val
  case class Obj(value: (java.lang.String, Val)*) extends AnyVal with Val
  case class Arr(value: Val*) extends AnyVal with Val
  case class Num(value: Double) extends AnyVal with Val
  case object False extends Val:
    def value = false
  case object True extends Val:
    def value = true
  case object Null extends Val:
    def value = null

  def stringChars(c: Char) = c != '\"' && c != '\\'

  def space[$: P] = P(CharsWhileIn(" \r\n", 0))
  def digits[$: P] = P(CharsWhileIn("0-9"))
  def exponent[$: P] = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
  def fractional[$: P] = P("." ~ digits)
  def integral[$: P] = P("0" | CharIn("1-9") ~ digits.?)

  def number[$: P] = P(
    CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?,
  ).!.map(x => JsonParser.Num(x.toDouble))

  def `null`[$: P] = P("null").map(_ => JsonParser.Null)
  def `false`[$: P] = P("false").map(_ => JsonParser.False)
  def `true`[$: P] = P("true").map(_ => JsonParser.True)

  def hexDigit[$: P] = P(CharIn("0-9a-fA-F"))
  def unicodeEscape[$: P] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  def escape[$: P] = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  def strChars[$: P] = P(CharsWhile(stringChars))
  def string[$: P] =
    P(space ~ "\"" ~/ (strChars | escape).rep.! ~ "\"")
      .map(JsonParser.Str.apply)

  def array[$: P] =
    P("[" ~/ jsonExpr.rep(sep = ","./) ~ space ~ "]").map(JsonParser.Arr(_*))

  def pair[$: P] = P(string.map(_.value) ~/ ":" ~/ jsonExpr)

  def obj[$: P] =
    P("{" ~/ pair.rep(sep = ","./) ~ space ~ "}").map(JsonParser.Obj(_*))

  def jsonExpr[$: P]: P[JsonParser.Val] = P(
    space ~ (obj | array | string | `true` | `false` | `null` | number) ~ space,
  )

  /**
   * Parse input string and return result
   */
  def parse(input: String): Either[String, JsonParser.Val] =
    fastparse.parse(input, jsonExpr(using _)) match
      case Parsed.Success(value, _) => Right(value)
      case f: Parsed.Failure => Left(f.msg)

object JsonParserMain extends App:
  import java.nio.file.{Files, Paths}

  val filePathIterative = s"inputs/iterative_json_3.txt"
  val fileContentIterative = new String(
    Files.readAllBytes(Paths.get(filePathIterative)),
  )

  JsonParser.parse(fileContentIterative) match
    case Right(result) => println(s"\nResult Iterative: $result")
    case Left(error) => println(s"\nError Iterative: $error")

  val filePathRecursive = s"inputs/recursive_json_3.txt"
  val fileContentRecursive = new String(
    Files.readAllBytes(Paths.get(filePathRecursive)),
  )

  JsonParser.parse(fileContentRecursive) match
    case Right(result) => println(s"\nResult Recursive: $result")
    case Left(error) => println(s"\nError Recursive: $error")
