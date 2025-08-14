package alpaca.lexer

import scala.annotation.tailrec
import scala.collection.SortedSet
import scala.util.matching.Regex

trait Tokenization {
  def tokens: SortedSet[Token[?]]
  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.tpe}>${tokenDef.pattern})").mkString("|").r

  @tailrec
  final def tokenize(
    input: String,
    position: Int = 0,
    acc: List[Lexem[?]] = Nil,
  ): List[Lexem[?]] =
    given Ctx = ??? /*todo: https://github.com/halotukozak/alpaca/issues/42*/
    input match
      case "" =>
        acc.reverse
      case _ =>
        compiled.findPrefixMatchOf(input) match
          case None =>
            throw new RuntimeException(s"Unexpected character at position $position: '${input(0)}'")

          case Some(m) =>
            tokens.find(token => m.group(token.tpe) ne null) match
              case Some(tokenDef: IgnoredToken[?]) =>
                tokenize(input.substring(m.end), position + m.end, acc)
              case Some(tokenDef) =>
                val lexem = Lexem(tokenDef.tpe, m.group(tokenDef.tpe), position)
                tokenize(input.substring(m.end), position + m.end, lexem :: acc)
              case None =>
                throw new AlgorithmError(s"$m matched but no token defined for it")
}
