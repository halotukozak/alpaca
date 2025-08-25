package alpaca.lexer

final case class Lexem[Name <: String, Ctx <: EmptyCtx](
  name: Name,
  value: Any | Null,
  ctx: Ctx,
)
