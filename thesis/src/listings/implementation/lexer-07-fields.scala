val fieldTpe = definedTokens
  .unsafeFoldLeft[(Type[? <: Tuple], Type[? <: Tuple])]((Type.of[EmptyTuple], Type.of[EmptyTuple])):
    case (
          ('[type names <: Tuple; names], '[type types <: Tuple; types]),
          '{ $token: DefinedToken[name, Ctx, value] },
        ) =>
      (Type.of[name *: names], Type.of[Token[name, Ctx, value] *: types])
  .runtimeChecked
  .match
    case ('[type names <: Tuple; names], '[type types <: Tuple; types]) => TypeRepr.of[NamedTuple[names, types]]
