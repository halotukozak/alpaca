package alpaca

transparent inline given ctx(using c: lexer.context.AnyGlobalCtx | parser.context.AnyGlobalCtx): c.type = c
