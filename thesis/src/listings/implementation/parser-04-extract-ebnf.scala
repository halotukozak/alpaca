def extractEBNF(ruleName: String)
  : PartialFunction[Expr[Rule[?]], Seq[(production: Production, action: Expr[Action[Ctx]])]] =
  case '{ rule(${ Varargs(cases) }*) } =>
    // ...

    cases
      .map(extractProductionName)
      .map:
        case (Lambda(_, Match(_, List(caseDef))), name) => caseDef -> name
        case (Lambda(_, Match(_, caseDefs)), name) =>
          report.errorAndAbort("Productions definition with multiple cases is not supported yet")
        case (other, name) =>
          report.errorAndAbort(show"Unexpected production definition: $other")
      .unsafeFlatMap:
        case (CaseDef(pattern, Some(_), rhs), name) =>
          throw new NotImplementedError("Guards are not supported yet")
        // Tuple1
        case (CaseDef(skipTypedOrTest(pattern @ Unapply(_, _, List(_))), None, rhs), name) =>
          val (symbol, bind, others) = extractEBNFAndAction(pattern)
          (
            production = Production.NonEmpty(NonTerminal(ruleName), NEL(symbol), name),
            action = createAction(List(bind), rhs),
          ) :: others

        // TupleN, N > 1
        case (CaseDef(skipTypedOrTest(p @ Unapply(_, _, patterns)), None, rhs), name) =>
          val (symbols, binds, others) = patterns.map(extractEBNFAndAction).unzip3(using _.toTuple)
          (
            production = Production.NonEmpty(NonTerminal(ruleName), NEL(symbols.head, symbols.tail*), name),
            action = createAction(binds, rhs),
          ) :: others.flatten
