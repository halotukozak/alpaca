definedTokens
  .unsafeFoldLeft(TypeRepr.of[Tokenization[Ctx]]):
    case (tpe, '{ $token: DefinedToken[name, Ctx, value] }) =>
      Refinement(tpe, ValidName.from[name], token.asTerm.tpe)
  .asType match
  case '[refinedTpe] =>
    val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])
    Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
