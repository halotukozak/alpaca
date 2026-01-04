      def createAction(binds: List[Option[Bind]], rhs: Term) = createLambda[Action[Ctx]]:
        case (methSym, (ctx: Term) :: (param: Term) :: Nil) =>
          val seqApplyMethod = param.select(TypeRepr.of[Seq[Any]].typeSymbol.methodMember("apply").head)
          val seq = param.asExprOf[Seq[Any]]

          val replacements = (find = ctxSymbol, replace = ctx) ::
            binds.zipWithIndex
              .collect:
                case (Some(bind), idx) => ((bind.symbol, bind.symbol.typeRef.asType), Expr(idx))
              .unsafeFlatMap:
                case ((bind, '[t]), idx) => Some((find = bind, replace = '{ $seq($idx).asInstanceOf[t] }.asTerm))

          replaceRefs(replacements*).transformTerm(rhs)(methSym)
