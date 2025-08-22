package alpaca.lexer

final case class Lexem[Name <: String, Ctx <: EmptyCtx](
  tpe: Name,
  value: Any | Null,
  ctx: Ctx,
)
