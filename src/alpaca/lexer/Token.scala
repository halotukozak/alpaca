package alpaca.lexer

import scala.annotation.compileTimeOnly
import java.util.concurrent.atomic.AtomicInteger

type ValidName = String & Singleton

type CtxManipulation[Ctx <: EmptyCtx] = Ctx => Ctx

object CtxManipulation {
  def empty[Ctx <: EmptyCtx]: CtxManipulation[Ctx] = identity
}
type Remapping[Ctx <: EmptyCtx] = Ctx => Any

object Remapping {
  def empty[Ctx <: EmptyCtx]: Remapping[Ctx] = _.text
}

sealed trait Token[Name <: ValidName, Ctx <: EmptyCtx] {
  val name: Name
  val pattern: String
  val ctxManipulation: CtxManipulation[Ctx]
}

object Token {

  // todo: we'd like not to require the explicit name for Ignored tokens
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored[Name <: ValidName](using ctx: EmptyCtx): Token[Name, ctx.type] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: EmptyCtx): Token[Name, ctx.type] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: EmptyCtx): Token[Name, ctx.type] = ???

  given Ordering[Token[?, ?]] = {
    case (x: IgnoredToken[?, ?], y: DefinedToken[?, ?]) => -1 // Ignored tokens are always less than any other token
    case (x: DefinedToken[?, ?], y: IgnoredToken[?, ?]) => 1
    case (x: DefinedToken[?, ?], y: DefinedToken[?, ?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?, ?], y: IgnoredToken[?, ?]) => x.pattern.compareTo(y.pattern)
  }
}

final case class DefinedToken[Name <: ValidName, Ctx <: EmptyCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx] = CtxManipulation.empty,
  remapping: Remapping[Ctx] = Remapping.empty,
) extends Token[Name, Ctx] {
  val index: Int = TokenImpl.nextIndex()

  override def toString: String =
    s"TokenImpl(name = $name, pattern = $pattern, index = $index, remapping = $remapping, ctxManipulation = $ctxManipulation)"
}

private object TokenImpl extends HasIndex

final case class IgnoredToken[Name <: ValidName, Ctx <: EmptyCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx] = CtxManipulation.empty,
) extends Token[Name, Ctx] {
  override def toString: String =
    s"IgnoredToken(pattern = $pattern, ctxManipulation = $ctxManipulation)"
}

private object IgnoredToken extends HasIndex

private sealed trait HasIndex {
  private val index = new AtomicInteger(0)

  def nextIndex(): Int = index.getAndIncrement()
}
