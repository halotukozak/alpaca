package alpaca.lexer

final case class Lexem[Name <: String](
//  using Ctx
//)(
  tpe: Name | Null,
  value: Any | Null,
//  val lineno: Int = ctx.lineno,
  index: Int,
//  = ctx.index,
//  val end: Int = ctx.index,
)
