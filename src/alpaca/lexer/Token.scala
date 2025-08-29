package alpaca.lexer

import alpaca.lexer.context.AnyGlobalCtx

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv

type ValidName = String & Singleton

type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Ctx

type Remapping[Ctx <: AnyGlobalCtx] = Ctx => Any

sealed trait Token[Name <: ValidName, +Ctx <: AnyGlobalCtx] {
  val name: Name
  val pattern: String
  val ctxManipulation: CtxManipulation[Ctx @uv]
}

object Token {

  // todo: we'd like not to require the explicit name for Ignored tokens
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: AnyGlobalCtx): Token[Name, ctx.type] = ???

  // todo: reconsider using or removing
  given Ordering[Token[?, ?]] = {
    case (x: IgnoredToken[?, ?], y: DefinedToken[?, ?]) => -1 // Ignored tokens are always less than any other token
    case (x: DefinedToken[?, ?], y: IgnoredToken[?, ?]) => 1
    case (x: DefinedToken[?, ?], y: DefinedToken[?, ?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?, ?], y: IgnoredToken[?, ?]) => x.pattern.compareTo(y.pattern)
  }
}

final case class DefinedToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: Remapping[Ctx @uv],
) extends Token[Name, Ctx] {
  val index: Int = TokenImpl.nextIndex()

  override def toString: String =
    s"TokenImpl(name = $name, pattern = $pattern, index = $index, remapping = $remapping, ctxManipulation = $ctxManipulation)"
}

private object TokenImpl extends HasIndex

final case class IgnoredToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx] {
  override def toString: String =
    s"IgnoredToken(pattern = $pattern, ctxManipulation = $ctxManipulation)"
}

private object IgnoredToken extends HasIndex

private sealed trait HasIndex {
  private val index = new AtomicInteger(0)

  def nextIndex(): Int = index.getAndIncrement()
}
