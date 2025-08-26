package alpaca.lexer

import alpaca.core.Copyable

import scala.annotation.tailrec
import scala.util.chaining.scalaUtilChainingOps
import scala.util.matching.Regex

abstract class Tokenization[Ctx <: EmptyCtx: Copyable as copy] extends Selectable {

  // todo: simplify types in refinement
  // type Token[Name <: ValidName] = alpaca.Token[Name, Ctx]
  // type Lexem[Name <: ValidName] = alpaca.Lexem[Name, Ctx]

  lazy val byName: Map[ValidName, Token[?, Ctx]] = tokens.view.map(token => token.name -> token).toMap
  private lazy val compiled: Regex =
    tokens.view.map(tokenDef => s"(?<${tokenDef.name}>${tokenDef.pattern})").mkString("|").r

  def tokens: List[Token[?, Ctx]]

  def selectDynamic(fieldName: String): Token[?, Ctx] = byName(fieldName)

  final def tokenize(input: String, initialContext: String => Ctx): List[Lexem[?, Ctx]] = {
    @tailrec def loop(globalCtx: Ctx, acc: List[Lexem[?, Ctx]]): List[Lexem[?, Ctx]] = globalCtx.text match
      case "" =>
        acc.reverse
      case _ =>
        compiled.findPrefixMatchOf(globalCtx.text) match
          case None =>
            throw new RuntimeException(s"Unexpected character ${globalCtx.text(0)}'") // custom error handling
          case Some(m) =>
            tokens.find(token => m.group(token.name) ne null) match
              case Some(IgnoredToken(name, _, modifyCtx)) =>
                val newGlobalCtx = copy(globalCtx).tap(_.betweenStages(m))

                loop(newGlobalCtx, acc)
              case Some(DefinedToken(name, _, modifyCtx, remapping)) =>
                val newGlobalCtx = copy(globalCtx).tap(_.betweenStages(m))
                val tokenCtx = copy(newGlobalCtx).tap(modifyCtx).tap(_.betweenLexems(m))
                val value = remapping(tokenCtx)

                val lexem = Lexem(name, value, tokenCtx)
                loop(newGlobalCtx, lexem :: acc)
              case None =>
                throw new AlgorithmError(s"$m matched but no token defined for it")

    loop(initialContext(input), Nil)
  }
}
