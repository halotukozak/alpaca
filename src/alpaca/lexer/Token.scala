package alpaca.lexer

import scala.annotation.compileTimeOnly

type ValidName = String & Singleton

sealed trait Token[Name <: ValidName](
  val name: Name,
  val pattern: String,
  val ctxManipulation: (Ctx => Unit) | Null,
)
object Token {

  // todo: we'd like not to require the explicit name for Ignored tokens
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored[Name <: ValidName](using Ctx): Token[Name] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using Ctx): Token[Name] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using Ctx): Token[Name] = ???

  given Ordering[Token[?]] = {
    case (x: IgnoredToken[?], y: TokenImpl[?]) => -1 // Ignored tokens are always less than any other token
    case (x: TokenImpl[?], y: IgnoredToken[?]) => 1
    case (x: TokenImpl[?], y: TokenImpl[?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?], y: IgnoredToken[?]) => x.pattern.compareTo(y.pattern)
  }

}

final class TokenImpl[Name <: ValidName](
  name: Name,
  pattern: String,
  ctxManipulation: (Ctx => Unit) | Null = null,
  val remapping: (Ctx => Any) | Null = null,
) extends Token[Name](name, pattern, ctxManipulation) {
  val index: Int = TokenImpl.nextIndex()

  override def toString: String =
    s"TokenImpl(name = $name, pattern = $pattern, index = $index, remapping = $remapping, ctxManipulation = $ctxManipulation)"
}

private object TokenImpl {
  private var index = 0

  private def nextIndex(): Int = {
    index += 1
    index
  }
}

final class IgnoredToken[Name <: ValidName](
  name: Name,
  pattern: String,
  ctxManipulation: (Ctx => Unit) | Null = null,
) extends Token[Name](name, pattern, ctxManipulation) {
  override def toString: String =
    s"IgnoredToken(pattern = $pattern, ctxManipulation = $ctxManipulation)"
}
