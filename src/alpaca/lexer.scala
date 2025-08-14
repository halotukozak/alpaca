package alpaca

import scala.util.matching.Regex
import scala.annotation.tailrec

final case class Token(name: String, regex: Regex, ignore: Boolean = false)

final case class Lexem(name: String, value: String, position: Int)

final class Lexer(tokenDefs: List[Token]) {
  private val compiled: Regex = tokenDefs.view.map { tokenDef => s"(?<${tokenDef.name}>${tokenDef.regex})" }.mkString("|").r

  @tailrec
  def tokenize(input: String, position: Int = 0, acc: List[Lexem] = Nil): List[Lexem] = input match
    case "" =>
      acc.reverse
    case _ =>
      compiled.findPrefixMatchOf(input) match
        case None =>
          throw new RuntimeException(s"Unexpected character at position $position: '${input(0)}'")

        case Some(m) =>
          tokenDefs.find(token => m.group(token.name) ne null) match
            case Some(tokenDef) if tokenDef.ignore =>
              tokenize(input.substring(m.end), position + m.end, acc)
            case Some(tokenDef) =>
              val lexem = Lexem(tokenDef.name, m.group(tokenDef.name), position)
              tokenize(input.substring(m.end), position + m.end, lexem :: acc)
            case None =>
              throw new AlgorithmError(s"$m matched but no token defined for it")
}
