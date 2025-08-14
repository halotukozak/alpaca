package alpaca.lexer

final class Lexem[Name <: String]
(
//  using Ctx
//)(
  val tpe: Name | Null,
  val value: Any | Null,
//  val lineno: Int = ctx.lineno,
  val index: Int 
//  = ctx.index,
//  val end: Int = ctx.index,
)
