      val additions = entries
        .map(entry =>
          '{
            def avoidTooLargeMethod(): Unit = $builder += ${ Expr(entry) }
            avoidTooLargeMethod()
          }.asTerm,
        )
        .toList
