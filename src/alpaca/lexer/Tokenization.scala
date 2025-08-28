package alpaca.lexer

import alpaca.core.Copyable

import scala.annotation.tailrec
import scala.util.chaining.scalaUtilChainingOps
import scala.util.matching.Regex

abstract class Tokenization[Ctx <: AnyGlobalCtx: {Copyable as copy, BetweenStages as betweenStages}]
  extends Selectable {
  // todo: simplify types in refinement
  // type Token[Name <: ValidName] = alpaca.Token[Name, Ctx]
  // type Lexem[Name <: ValidName] = alpaca.Lexem[Name, Ctx]

  lazy val byName: Map[ValidName, Token[?, Ctx]] = tokens.view.map(token => token.name -> token).toMap
  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.name}>${tokenDef.pattern})").mkString("|").r

  def tokens: List[Token[?, Ctx]]

  def selectDynamic(fieldName: String): Token[?, Ctx] = byName(fieldName)

  final def tokenize(input: CharSequence, initialContext: Ctx): List[Lexem[?]] = {

    @tailrec def loop(globalCtx: Ctx)(acc: List[Lexem[?]]): List[Lexem[?]] =
      globalCtx.text match
        case "" =>
          acc.reverse
        case _ =>
          compiled.findPrefixMatchOf(globalCtx.text) match
            case None =>
              throw new RuntimeException(s"Unexpected character ${globalCtx.text(0)}'") // todo: custom error handling
            case Some(m) =>
              tokens.find(token => m.group(token.name) ne null) match
                case Some(IgnoredToken(name, _, modifyCtx)) =>
                  betweenStages(m, globalCtx)

                  loop(globalCtx)(acc)
                case Some(DefinedToken(name, _, modifyCtx, remapping)) =>
                  betweenStages(m, globalCtx)

                  val value = remapping(globalCtx)
                  val lexem = globalCtx.lastLexem.nn // todo: for now

                  loop(globalCtx)(lexem :: acc)
                case None =>
                  throw new AlgorithmError(s"$m matched but no token defined for it")

    initialContext.text = input
    loop(initialContext)(Nil)
  }
}
