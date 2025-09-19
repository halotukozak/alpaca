package alpaca.lexer

import alpaca.core.{BetweenStages, Copyable, Empty}
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
      globalCtx._text.length match
        case 0 =>
          acc.reverse
        case _ =>
          compiled.findPrefixMatchOf(globalCtx._text) match
            case None =>
              throw new RuntimeException(s"Unexpected character: '${globalCtx._text.charAt(0)}'") // todo: custom error handling
            case Some(m) =>
              tokens.find(token => m.group(token.name) ne null) match
                case Some(token @ IgnoredToken(_, _, modifyCtx)) =>
                  betweenStages(token, m, globalCtx)

                  loop(globalCtx)(acc)
                case Some(token @ DefinedToken(_, _, modifyCtx, remapping)) =>
                  betweenStages(token, m, globalCtx)

                  val value = remapping(globalCtx)
                  val lexem = globalCtx.lastLexem.nn // todo: for now

                  loop(globalCtx)(lexem :: acc)
                case None =>
                  throw new AlgorithmError(s"$m matched but no token defined for it")

    val initialContext = empty()
    initialContext._text = input
    loop(initialContext)(Nil)
  }
}
