package alpaca.lexer

import alpaca.core.{Copyable, Empty}
import alpaca.lexer.context.{AnyGlobalCtx, Lexem}

import scala.annotation.tailrec
import scala.util.matching.Regex

abstract class Tokenization[Ctx <: AnyGlobalCtx: {Copyable as copy, BetweenStages as betweenStages}]
  extends Selectable {

  private lazy val compiled: Regex =
    // todo: consider some quoting somewhere someday
    tokens.view.map(tokenDef => s"(?<${tokenDef.name}>${tokenDef.pattern})").mkString("|").r

  def tokens: List[Token[?, Ctx, ?]]
  def byName: Map[String, DefinedToken[?, Ctx, ?]] // todo: reconsider if selectDynamic should be implemented with PM

  def selectDynamic(fieldName: String): DefinedToken[?, Ctx, ?] =
    byName(scala.reflect.NameTransformer.decode(fieldName))

  final def tokenize(input: CharSequence)(using empty: Empty[Ctx]): List[Lexem[?, ?]] = {
    @tailrec def loop(globalCtx: Ctx)(acc: List[Lexem[?, ?]]): List[Lexem[?, ?]] =
      globalCtx.text.length match
        case 0 =>
          acc.reverse
        case _ =>
          val m = compiled.findPrefixMatchOf(globalCtx.text) getOrElse {
            // todo: custom error handling https://github.com/halotukozak/alpaca/issues/21
            throw new RuntimeException(s"Unexpected character: '${globalCtx.text.charAt(0)}'")
          }
          val token = tokens.find(token => m.group(token.name) ne null) getOrElse {
            throw new AlgorithmError(s"$m matched but no token defined for it")
          }
          val lexem = List(token).collect { case _: DefinedToken[?, Ctx, ?] => globalCtx.lastLexem }
          betweenStages(token, m, globalCtx)
          loop(globalCtx)(lexem ::: acc)

    val initialContext = empty()
    initialContext.text = input
    loop(initialContext)(Nil)
  }
}

extension (input: CharSequence)
  private def from(pos: Int): CharSequence = input match
    case lfr: LazyReader => lfr.from(pos)
    case _ => input.subSequence(pos, input.length)
