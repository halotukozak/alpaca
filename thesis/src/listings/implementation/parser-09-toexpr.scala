  given ToExpr[ParseTable] with
    def apply(entries: ParseTable)(using quotes: Quotes): Expr[ParseTable] =
      import quotes.reflect.*

      type BuilderTpe = mutable.Builder[
        ((state: Int, stepSymbol: parser.Symbol), Shift | Reduction),
        Map[(state: Int, stepSymbol: parser.Symbol), Shift | Reduction],
      ]

      val symbol = Symbol.newVal(
        Symbol.spliceOwner,
        Symbol.freshName("builder"),
        TypeRepr.of[BuilderTpe],
        Flags.Mutable,
        Symbol.noSymbol,
      )

      val valDef = ValDef(symbol, Some('{ Map.newBuilder: BuilderTpe }.asTerm))

      val builder = Ref(symbol).asExprOf[BuilderTpe]

      val additions = entries
        .map(entry =>
          '{
            def avoidTooLargeMethod(): Unit = $builder += ${ Expr(entry) }
            avoidTooLargeMethod()
          }.asTerm,
        )
        .toList

      val result = '{ $builder.result() }.asTerm

      Block(valDef :: additions, result).asExprOf[ParseTable]
