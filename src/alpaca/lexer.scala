package alpaca

import scala.util.matching.Regex
import scala.annotation.tailrec
import java.nio.file.Path

final case class Token(name: String, regex: Regex, ignore: Boolean = false)

final case class Lexem(name: String, value: String)

final class Lexer(tokenDefs: List[Token]) {
  private val compiled: Regex = tokenDefs.view.map { tokenDef => s"(?<${tokenDef.name}>${tokenDef.regex})" }.mkString("|").r

  def tokenize(path: Path): List[Lexem] = tokenizeImpl(LazyReader.from(path))
  def tokenize(data: String): List[Lexem] = tokenizeImpl(data)

  @tailrec
  private def tokenizeImpl(input: CharSequence, acc: List[Lexem] = Nil): List[Lexem] = input.length match
    case 0 =>
      input.close()
      acc.reverse
    case _ =>
      compiled.findPrefixMatchOf(input) match
        case None =>
          input.close()
          throw new RuntimeException(s"Unexpected character: '${input.charAt(0)}'")

        case Some(m) =>
          tokenDefs.find(token => m.group(token.name) ne null) match
            case Some(tokenDef) if tokenDef.ignore =>
              tokenizeImpl(input.from(m.end), acc)
            case Some(tokenDef) =>
              val lexem = Lexem(tokenDef.name, m.group(tokenDef.name))
              tokenizeImpl(input.from(m.end), lexem :: acc)
            case None =>
              input.close()
              throw new AlgorithmError(s"$m matched but no token defined for it")
}

extension (input: CharSequence) def from(pos: Int): CharSequence = input match
  case lfr: LazyReader => lfr.from(pos)
  case _ => input.subSequence(pos, input.length)

extension (input: CharSequence) def close(): Unit = input match
  case lfr: LazyReader => lfr.close()
  case _ => ()
