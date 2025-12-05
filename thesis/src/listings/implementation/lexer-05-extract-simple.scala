def extractSimple(
  ctxManipulation: Expr[CtxManipulation[Ctx]],
): PartialFunction[Expr[ThisToken], List[Expr[ThisToken]]] =
  case '{ Token.Ignored(using $ctx) } =>
  // ...

  case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
  // ...

  case '{ type t <: ValidName; Token.apply[t]($value: String)(using $ctx) } if value.asTerm.symbol == tree.symbol =>
  // ...

  case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } =>
  // ...
