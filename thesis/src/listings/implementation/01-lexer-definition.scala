type LexerDefinition[Ctx <: LexerCtx] = PartialFunction[String, Token[?, Ctx, ?]]
