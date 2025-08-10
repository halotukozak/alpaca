package alpaca

import scala.util.matching.Regex

case class Token(name: String, regex: Regex, ignore: Boolean = false)

case class Lexem(name: String, value: String, position: Int)

final case class Lexer(tokenDefs: List[Token]) {
  private val compiled: Regex = tokenDefs.map { tokenDef => s"(?<${tokenDef.name}>${tokenDef.regex})" }.mkString("|").r

  def tokenize(input: String, position: Int = 0): List[Lexem] = {
    if input.isEmpty then return List.empty[Lexem]

    compiled.findPrefixMatchOf(input) match
      case Some(m) =>
        val tokenDef = tokenDefs.find(token => m.group(token.name) ne null).get

        if tokenDef.ignore then
          tokenize(input.substring(m.end), position + m.end)
        else
          val lexem = Lexem(tokenDef.name, m.group(tokenDef.name), position)
          lexem :: tokenize(input.substring(m.end), position + m.end)
        end if

      case None =>
        throw new RuntimeException(s"Unexpected character at position $position: '${input(0)}'")
  }
}
