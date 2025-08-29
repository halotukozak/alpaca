package alpaca.lexer

import scala.annotation.tailrec
import scala.util.matching.Regex

trait Tokenization {
  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.name}>${tokenDef.pattern})").mkString("|").r

  def tokens: List[Token[?]]

  final def tokenize(input: CharSequence): List[Lexem[?]] = {
    @tailrec def loop(ctx: Ctx, acc: List[Lexem[?]]): List[Lexem[?]] = ctx.text match
      case "" =>
        acc.reverse
      case _ =>
        compiled.findPrefixMatchOf(ctx.text) match
          case None =>
            throw new RuntimeException(s"Unexpected character at position ${ctx.position}: '${ctx.text.charAt(0)}'")
          case Some(m) =>
            val newCtx = new Ctx(_text = ctx.text.from(m.end), position = ctx.position + m.end)

            tokens.find(token => m.group(token.name) ne null) match
              case Some(IgnoredToken(name, _, modifyCtx)) =>
                loop(modifyCtx(newCtx), acc)
              case Some(DefinedToken(name, _, modifyCtx, remapping)) =>
                val value = remapping(new Ctx(_text = m.group(name), position = ctx.position))
                val lexem = Lexem(name, value, ctx.position)
                loop(modifyCtx(newCtx), lexem :: acc)
              case None =>
                throw new AlgorithmError(s"$m matched but no token defined for it")

    loop(new Ctx(input, 0), Nil)
  }
}
