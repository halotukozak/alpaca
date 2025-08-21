package alpaca.lexer

import scala.annotation.tailrec
import scala.collection.SortedSet
import scala.util.matching.Regex

trait Tokenization {
  def tokens: SortedSet[Token[?]]
  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.tpe}>${tokenDef.pattern})").mkString("|").r

  final def tokenize(input: String): List[Lexem[?]] = {
    @tailrec def loop(using ctx: Ctx)(acc: List[Lexem[?]]): List[Lexem[?]] = ctx.text match
      case "" =>
        acc.reverse
      case _ =>
        compiled.findPrefixMatchOf(ctx.text) match
          case None =>
            throw new RuntimeException(s"Unexpected character at position ${ctx.position}: '${ctx.text(0)}'")
          case Some(m) =>
            val newCtx = new Ctx(text = ctx.text.substring(m.end), position = ctx.position + m.end)

            tokens.find(token => m.group(token.tpe) ne null) match
              case Some(IgnoredToken(tpe, _, modifyCtx)) =>
                loop(modifyCtx(newCtx))(acc)
              case Some(TokenImpl(tpe, _, modifyCtx, remapping)) =>
                val value = {
                  val matched = m.group(tpe)
                  remapping match
                    case Some(remap) => remap(new Ctx(text = matched, position = ctx.position))
                    case None => matched
                }

                val lexem = Lexem(tpe, value, ctx.position)
                loop(modifyCtx(newCtx))(lexem :: acc)
              case None =>
                throw new AlgorithmError(s"$m matched but no token defined for it")

    loop(new Ctx(input, 0))(Nil)
  }
}
