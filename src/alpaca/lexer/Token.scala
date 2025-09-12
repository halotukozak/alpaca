package alpaca.lexer

import alpaca.lexer.context.AnyGlobalCtx

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv
import context.Lexem

type ValidName = String & Singleton

type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Ctx
type Remapping[Ctx <: AnyGlobalCtx, Value] = Ctx => Value

sealed trait Token[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value] {
  val name: Name
  val pattern: String
  val ctxManipulation: CtxManipulation[Ctx @uv]
}

object Token {

  // todo: we'd like not to require the explicit name for Ignored tokens
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type, String] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type, String] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: AnyGlobalCtx): Token[Name, ctx.type, value.type] = ???

  // todo: reconsider using or removing
  given Ordering[Token[?, ?, ?]] = {
    // Ignored tokens are always less than any other token
    case (x: IgnoredToken[?, ?], y: DefinedToken[?, ?, ?]) => -1
    case (x: DefinedToken[?, ?, ?], y: IgnoredToken[?, ?]) => 1
    case (x: DefinedToken[?, ?, ?], y: DefinedToken[?, ?, ?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?, ?], y: IgnoredToken[?, ?]) => x.pattern.compareTo(y.pattern)
  }
}

//todo: may be invariant?
final case class DefinedToken[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: Remapping[Ctx @uv, Value],
) extends Token[Name, Ctx, Value] {
  val index: Int = DefinedToken.nextIndex()

  // todo: find a better way to handle Value = Unit to avoid CalcLexer.PLUS(())
  def unapply(lexem: Lexem[Name, Value]): Option[Lexem[Name, Value]] = Some(lexem)
}

object DefinedToken extends HasIndex

final case class IgnoredToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]

private object IgnoredToken extends HasIndex

private sealed trait HasIndex {
  private val index = new AtomicInteger(0)

  def nextIndex(): Int = index.getAndIncrement()
}
