package alpaca.lexer

final case class Lexem[Name <: String, Ctx <: AnyLexemCtx](
  name: Name,
  value: Any,
  ctx: Ctx,
)
