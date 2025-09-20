package alpaca.lexer

import alpaca.lexer.context.AnyGlobalCtx

import java.util.concurrent.atomic.AtomicInteger
import scala.annotation.compileTimeOnly
import scala.annotation.unchecked.uncheckedVariance as uv
import context.Lexem
import alpaca.lexer.context.GlobalCtx

type ValidName = String & Singleton

type CtxManipulation[Ctx <: AnyGlobalCtx] = Ctx => Unit

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
}

//todo: may be invariant?
final case class DefinedToken[Name <: ValidName, +Ctx <: AnyGlobalCtx, Value](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
  remapping: (Ctx @uv) => Value,
) extends Token[Name, Ctx, Value] {
  // todo: find a better way to handle Value = Unit to avoid CalcLexer.PLUS(()) or CalcLexer.PLUS(_)
  def unapply(lexem: Lexem[Name, Value]): Option[Lexem[Name, Value]] = Some(lexem)
}

final case class IgnoredToken[Name <: ValidName, +Ctx <: AnyGlobalCtx](
  name: Name,
  pattern: String,
  ctxManipulation: CtxManipulation[Ctx @uv],
) extends Token[Name, Ctx, Nothing]
