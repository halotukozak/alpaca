package alpaca.lexer

import scala.annotation.tailrec
import scala.util.matching.Regex
import scala.util.chaining.scalaUtilChainingOps
import alpaca.core.Copyable

abstract class Tokenization[Ctx <: EmptyCtx: Copyable as copy] {

  // todo: simplify types in refinement
  // type Token[Name <: ValidName] = alpaca.Token[Name, Ctx]
  // type Lexem[Name <: ValidName] = alpaca.Lexem[Name, Ctx]

  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.name}>${tokenDef.pattern})").mkString("|").r

  def tokens: List[Token[?, Ctx]]

  final def tokenize(input: String, initialContext: String => Ctx): List[Lexem[?, Ctx]] = {
    @tailrec def loop(ctx: Ctx, acc: List[Lexem[?, Ctx]]): List[Lexem[?, Ctx]] = ctx.text match
      case "" =>
        acc.reverse
      case _ =>
        compiled.findPrefixMatchOf(ctx.text) match
          case None =>
            throw new RuntimeException(s"Unexpected character ${ctx.text(0)}'") // custom error handling
          case Some(m) =>
            val newCtx = copy(ctx).tap(_.betweenLexems(m))

            tokens.find(token => m.group(token.name) ne null) match
              case Some(IgnoredToken(name, _, modifyCtx)) =>
                loop(modifyCtx(newCtx), acc)
              case Some(DefinedToken(name, _, modifyCtx, remapping)) =>
                val value = remapping(newCtx)
                val lexem = Lexem(name, value, newCtx)
                loop(modifyCtx(newCtx), lexem :: acc)
              case None =>
                throw new AlgorithmError(s"$m matched but no token defined for it")

    loop(initialContext(input), Nil)
  }
}
