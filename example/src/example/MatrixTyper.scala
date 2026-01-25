package example

type TypeEnv = Map[String, Type]

object MatrixTyper extends TreeMapper[TypeEnv]:
  extension [T <: AST.Expr](expr: T)
    private def expectToBe(expectedType: Type): Result[T] =
      if expr.tpe <= expectedType then Result(expr)
      else Result.error(expr.line)(expr, s"Expected $expectedType, got ${expr.tpe}")

  extension (func: Type.Function)
    private def checkArgs(args: List[AST.Expr])(line: Int): Result[Type] =
      if func.takes(args.map(_.tpe)) then Result(func.result)
      else Result.error(line)(Type.Undef, s"Expected ${func.args}, got ${args.map(_.tpe).mkString("(", ",", ")")}")

  override val handleNull: TypeEnv => Result[(Null, TypeEnv)] = env => Result((null, env))

  override given Process[AST.Block] = env =>
    case AST.Block(statements, line) =>
      for (typedStmts, newEnv) <- statements.visitAll(env)
      yield (AST.Block(typedStmts, line), newEnv)

  override given Process[AST.Assign] = env =>
    case AST.Assign(ref: AST.SymbolRef, expr, line) =>
      for
        (typedExpr, env) <- expr.visit(env)
        typedRef = ref.copy(tpe = typedExpr.tpe)
      yield (AST.Assign(typedRef, typedExpr, line), env + (typedRef.name -> typedExpr.tpe))
    case AST.Assign(ref, expr, line) =>
      for
        (typedExpr, env) <- expr.visit(env)
        (typedRef, env) <- ref.visit(env)
      yield (AST.Assign(typedRef, typedExpr, line), env)

  override given Process[AST.If] = env =>
    case AST.If(condition, thenBlock, elseBlock: AST.Block, line) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedThenBlock, env) <- thenBlock.visit(env)
        (typedElseBlock, env) <- elseBlock.visit(env)
      yield (AST.If(typedCondition, typedThenBlock, typedElseBlock, line), env)
    case AST.If(condition, thenBlock, null, line) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedThenBlock, env) <- thenBlock.visit(env)
      yield (AST.If(typedCondition, typedThenBlock, null, line), env)

  override given Process[AST.While] = env =>
    case AST.While(condition, body, line) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedBody, env) <- body.visit(env)
      yield (AST.While(typedCondition, typedBody, line), env)

  override given Process[AST.Return] = env =>
    case AST.Return(expr, line) =>
      for (typedExpr, env) <- expr.visit(env)
      yield (AST.Return(typedExpr, line), env)

  override given Process[AST.Continue] = env => c => Result((c, env))

  override given Process[AST.Break] = env => b => Result((b, env))

  override given Process[AST.Literal] = env => l => Result((l, env))

  override given Process[AST.SymbolRef] = env =>
    ref =>
      val tpe = env.getOrElse(ref.name, Type.Undef)
      Result((ref.copy(tpe = tpe), env))

  override given Process[AST.VectorRef] = env =>
    case AST.VectorRef(vector, element, line) =>
      for
        (typedVector, env) <- vector.visit(env)
        _ <- typedVector.expectToBe(Type.Vector())
        (typedElement, env) <- element.visit(env)
        _ <- typedElement.expectToBe(Type.Numerical)
      yield (AST.VectorRef(typedVector, typedElement, line), env)

  override given Process[AST.MatrixRef] = env =>
    case AST.MatrixRef(matrix, row, col, line) =>
      for
        (typedMatrix, env) <- matrix.visit(env)
        _ <- typedMatrix.expectToBe(Type.Matrix())
        (typedRow, env) <- row.visit(env)
        (typedCol, env) <- col.visit(env)
        _ <- typedRow.expectToBe(Type.Int)
        _ <- typedCol.expectToBe(Type.Int)
      yield (AST.MatrixRef(typedMatrix, typedRow, typedCol, line), env)

  override given Process[AST.Apply] = env =>
    case AST.Apply(ref, args, _, line) =>
      for
        (typedRef, env) <- ref.visit(env)
        (typedArgs, env) <- args.visitAll(env)
        argTypes = typedArgs.map(_.tpe)
        typedResult <- ref.tpe match
          case func: Type.Function => func.checkArgs(typedArgs)(line)

          case overloaded: Type.OverloadedFunction =>
            for
              func <- overloaded(typedArgs)
              result <- func.checkArgs(typedArgs)(line)
            yield result

          case anyOf: Type.AnyOf =>
            anyOf.all.iterator
              .map:
                case func: Type.Function => func.checkArgs(typedArgs)(line)
                case overloaded: Type.OverloadedFunction =>
                  for
                    func <- overloaded(typedArgs)
                    result <- func.checkArgs(typedArgs)(line)
                  yield result
                case _ => null
              .foldLeft[Result[Type] | Null](null):
                case (success: Result.Success[Type], _) => success
                case (_, success: Result.Success[Type]) => success
                case (acc, null) => acc
                case (acc: Result.Failure[Type], next: Result.Failure[Type]) => acc.flatMap(_ => next)
                case (null, next: Result.Failure[Type]) => next
              .match
                case null => Type.Function(Tuple(anyOf), Type.Undef).checkArgs(typedArgs)(line)
                case result => result

          case otherType => Result.error(line)(Type.Undef, s"Cannot apply expression of type $otherType as a function")
      yield (AST.Apply(typedRef, typedArgs, typedResult, line), env)

  override given Process[AST.Range] = env =>
    case AST.Range(start, end, line) =>
      for
        (typedStart, env) <- start.visit(env)
        _ <- typedStart.expectToBe(Type.Int)
        (typedEnd, env) <- end.visit(env)
        _ <- typedEnd.expectToBe(Type.Int)
      yield (AST.Range(typedStart, typedEnd, line), env)

  override given Process[AST.For] = env =>
    case AST.For(varRef, range, body, line) =>
      for
        (typedRange, env) <- range.visit(env)
        (typedVarRef, env) <- varRef.visit(env)
        (typedBody, env) <- body.visit(env + (varRef.name -> Type.Int))
      yield (AST.For(typedVarRef, typedRange, typedBody, line), env)
