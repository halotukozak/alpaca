package alpaca.lexer

import alpaca.lexer.context.{AnyGlobalCtx, Lexem}

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv
import scala.quoted.*

private[lexer] type ValidName = String & Singleton

private[lexer] type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Unit

private[lexer] final case class TokenInfo[+Name <: ValidName] private (
  name: Name,
  regexGroupName: String,
  pattern: String,
)

//todo: why it cannot be private[lexer]
object TokenInfo {
  private val counter = AtomicInteger(0)

  def apply[Name <: ValidName](name: Name, pattern: String): TokenInfo[Name] =
    TokenInfo(name, s"token${counter.getAndIncrement()}", pattern)

  given FromExpr[TokenInfo[?]] with
    def unapply(x: Expr[TokenInfo[?]])(using Quotes): Option[TokenInfo[?]] = x match
      case '{ TokenInfo($name: ValidName, $pattern: String) } =>
        for
          name <- name.value
          pattern <- pattern.value
        yield TokenInfo(name, pattern)
      case _ => None
}

sealed trait Token[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value] {
  val info: TokenInfo[Name]
  val ctxManipulation: CtxManipulation[Ctx @uv]
}

object Token {
  @compileTimeOnly("Should never be called outside the lexer definition")
  def Ignored(using ctx: AnyGlobalCtx): Token[?, ctx.type, Nothing] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](using ctx: AnyGlobalCtx): Token[Name, ctx.type, String] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ValidName](value: Any)(using ctx: AnyGlobalCtx): Token[Name, ctx.type, value.type] = ???
}

//todo: may be invariant?
final case class DefinedToken[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value](
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value] {
  type LexemTpe = Lexem[Name, Value]
}

final case class IgnoredToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  info: TokenInfo[Name],
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]
