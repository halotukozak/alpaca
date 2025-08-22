package alpaca.lexer

import scala.annotation.compileTimeOnly
import java.util.concurrent.atomic.AtomicInteger

type ValidName = String & Singleton

type CtxManipulation = Ctx => Ctx
object CtxManipulation {
  val empty: CtxManipulation = identity
}
type Remapping = Ctx => Any
object Remapping {
  val empty: Remapping = _.text
}

sealed trait Token[Name <: ValidName] {
  val name: Name
  val pattern: String
  val ctxManipulation: CtxManipulation
}
object Token {

  // todo: we'd like not to require the explicit name for Ignored tokens
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored[Name <: ValidName](using Ctx): Token[Name] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using Ctx): Token[Name] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using Ctx): Token[Name] = ???

  given Ordering[Token[?]] = {
    case (x: IgnoredToken[?], y: DefinedToken[?]) => -1 // Ignored tokens are always less than any other token
    case (x: DefinedToken[?], y: IgnoredToken[?]) => 1
    case (x: DefinedToken[?], y: DefinedToken[?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?], y: IgnoredToken[?]) => x.pattern.compareTo(y.pattern)
  }
}

final case class DefinedToken[Name <: ValidName](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation = CtxManipulation.empty,
  remapping: Remapping = Remapping.empty,
) extends Token[Name] {
  val index: Int = TokenImpl.nextIndex()

  override def toString: String =
    s"TokenImpl(name = $name, pattern = $pattern, index = $index, remapping = $remapping, ctxManipulation = $ctxManipulation)"
}

private object TokenImpl extends HasIndex

final case class IgnoredToken[Name <: ValidName](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation = CtxManipulation.empty,
) extends Token {
  override def toString: String =
    s"IgnoredToken(pattern = $pattern, ctxManipulation = $ctxManipulation)"
}

private object IgnoredToken extends HasIndex

private sealed trait HasIndex {
  private val index = new AtomicInteger(0)

  def nextIndex(): Int = index.getAndIncrement()
}
