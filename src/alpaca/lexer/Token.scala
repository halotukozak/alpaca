package alpaca.lexer

import scala.annotation.compileTimeOnly

sealed trait Token[Name <: String](val tpe: Name, val pattern: String, val ctxManipulation: (Ctx => Unit) | Null)
object Token {
  @compileTimeOnly("Should never be called outside the lexer definition")
  val Ignored: Ctx ?=> Token[?] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ConstString](using Ctx): Token[Name] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ConstString](value: Any)(using Ctx): Token[Name] = ???

  given Ordering[Token[?]] = {
    case (x: IgnoredToken[?], y: TokenImpl[?]) => -1 // Ignored tokens are always less than any other token
    case (x: TokenImpl[?], y: IgnoredToken[?]) => 1
    case (x: TokenImpl[?], y: TokenImpl[?]) => x.index.compareTo(y.index)
    case (x: IgnoredToken[?], y: IgnoredToken[?]) => x.pattern.compareTo(y.pattern)
  }

}

final class TokenImpl[Name <: String](
  tpe: Name,
  pattern: String,
  ctxManipulation: (Ctx => Unit) | Null = null,
  val remapping: (Ctx => Any) | Null = null,
) extends Token[Name](tpe, pattern, ctxManipulation) {
  val index: Int = TokenImpl.nextIndex()

  override def toString: String =
    s"TokenImpl(tpe = $tpe, pattern = $pattern, index = $index, remapping = $remapping, ctxManipulation = $ctxManipulation)"
}

private object TokenImpl {
  private var index = 0

  private def nextIndex(): Int = {
    index += 1
    index
  }
}

final class IgnoredToken[Name <: String](tpe: Name, pattern: String, ctxManipulation: (Ctx => Unit) | Null = null)
  extends Token(tpe, pattern, ctxManipulation) {
  override def toString: String =
    s"IgnoredToken(pattern = $pattern, ctxManipulation = $ctxManipulation)"
}
