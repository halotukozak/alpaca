package alpaca

import scala.util.matching.Regex
import scala.annotation.tailrec
import java.nio.file.Path

final case class Token(name: String, regex: Regex, ignore: Boolean = false)

final case class Lexem(name: String, value: String, ctx: Int = 0)

final class Lexer(tokenDefs: List[Token]) {
  private val compiled: Regex = tokenDefs.view.map { tokenDef => s"(?<${tokenDef.name}>${tokenDef.regex})" }.mkString("|").r

  def tokenizeFile(path: Path): List[Lexem] = tokenize(LazyFileReader.fromFile(path))
  def tokenizeString(data: String): List[Lexem] = tokenize(LazyFileReader.fromString(data))

  @tailrec
  def tokenize(input: LazyFileReader, acc: List[Lexem] = Nil): List[Lexem] = input.length match
    case 0 =>
      acc.reverse
    case _ =>
      compiled.findPrefixMatchOf(input) match
        case None =>
          throw new RuntimeException(s"Unexpected character: '${input.charAt(0)}'")

        case Some(m) =>
          tokenDefs.find(token => m.group(token.name) ne null) match
            case Some(tokenDef) if tokenDef.ignore =>
              tokenize(input.from(m.end), acc)
            case Some(tokenDef) =>
              val lexem = Lexem(tokenDef.name, m.group(tokenDef.name))
              tokenize(input.from(m.end), lexem :: acc)
            case None =>
              throw new AlgorithmError(s"$m matched but no token defined for it")
}
