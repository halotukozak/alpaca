package example

import example.AST.*

import scala.collection.Factory
import scala.util.chaining.scalaUtilChainingOps

type TypeEnv = Map[String, Type]

trait MatrixTyper[T <: AST.Tree | Null]:
  def visit(tree: T, env: TypeEnv): Result[(T, TypeEnv)]

object MatrixTyper:
  extension [T <: AST.Tree | Null: MatrixTyper as typer](tree: T)
    def visit(env: TypeEnv = Map.empty): Result[(T, TypeEnv)] = typer.visit(tree, env)

  extension [T <: AST.Tree | Null: MatrixTyper as typer, CC[X] <: Iterable[X]](trees: CC[T])
    inline private def visitAll(env: TypeEnv): Result[(CC[T], TypeEnv)] =
      trees.iterator
        .foldLeft(Result.Success((Seq.empty[T], env)): Result[(Seq[T], TypeEnv)]): (acc, res) =>
          for
            (accValues, accEnv) <- acc
            (value, newEnv) <- res.visit(accEnv)
          yield (accValues :+ value, newEnv)
        .map((seq, env) => (seq.to(compiletime.summonInline[Factory[T, CC[T]]]), env))

  extension [T <: AST.Expr](expr: T)
    private def expectToBe(expectedType: Type): Result[T] =
      if expr.tpe <= expectedType then Result(expr)
      else Result.error(expr.line)(expr, s"Expected $expectedType, got ${expr.tpe}")

  extension (func: Type.Function)
    private def checkArgs(args: List[AST.Expr])(line: Int): Result[Type] =
      if func.takes(args.map(_.tpe)) then Result(func)
      else Result.error(line)(Type.Undef, s"Expected ${func.args}, got ${args.map(_.tpe).mkString("(", ",", ")")}")

  given [T <: AST.Tree | Null: MatrixTyper as typer]: MatrixTyper[T | Null] =
    case (null, env) => Result((null, env))
    case (t: T @unchecked, env) => typer.visit(t, env)

  given MatrixTyper[AST.Tree] =
    case (b: AST.Block, env) => b.visit(env)
    case (s: AST.Statement, env) => s.visit(env)

  given MatrixTyper[AST.Statement] =
    case (e: AST.Expr, env) => e.visit(env)
    case (a: AST.Assign, env) => a.visit(env)
    case (i: AST.If, env) => i.visit(env)
    case (w: AST.While, env) => w.visit(env)
    case (f: AST.For, env) => f.visit(env)
    case (r: AST.Return, env) => r.visit(env)
    case (c: AST.Continue, env) => c.visit(env)
    case (b: AST.Break, env) => b.visit(env)

  given MatrixTyper[AST.Expr] =
    case (l: AST.Literal, env) => l.visit(env)
    case (r: AST.Ref, env) => r.visit(env)
    case (a: AST.Apply, env) => a.visit(env)
    case (r: AST.Range, env) => r.visit(env)

  given MatrixTyper[AST.Ref] =
    case (r: AST.SymbolRef, env) => r.visit(env)
    case (v: AST.VectorRef, env) => v.visit(env)
    case (m: AST.MatrixRef, env) => m.visit(env)

  given MatrixTyper[AST.Block] =
    case (AST.Block(statements, line), env) =>
      for (typedStmts, newEnv) <- statements.visitAll(env)
      yield (AST.Block(typedStmts, line), newEnv)

  given MatrixTyper[AST.Assign] =
    case (AST.Assign(ref: AST.SymbolRef, expr, line), env) =>
      for
        (typedExpr, env) <- expr.visit(env)
        typedRef = ref.copy(tpe = typedExpr.tpe)
      yield (AST.Assign(typedRef, typedExpr, line), env + (typedRef.name -> typedExpr.tpe))

    case (AST.Assign(ref, expr, line), env) =>
      for
        (typedExpr, env) <- expr.visit(env)
        (typedRef, env) <- ref.visit(env)
      yield (AST.Assign(typedRef, typedExpr, line), env)

  given MatrixTyper[AST.If] =
    case (AST.If(condition, thenBlock, elseBlock: AST.Block, line), env) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedThenBlock, env) <- thenBlock.visit(env)
        (typedElseBlock, env) <- elseBlock.visit(env)
      yield (AST.If(typedCondition, typedThenBlock, typedElseBlock, line), env)
    case (AST.If(condition, thenBlock, null, line), env) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedThenBlock, env) <- thenBlock.visit(env)
      yield (AST.If(typedCondition, typedThenBlock, null, line), env)

  given MatrixTyper[AST.While] =
    case (AST.While(condition, body, line), env) =>
      for
        (typedCondition, env) <- condition.visit(env)
        _ <- typedCondition.expectToBe(Type.Bool)
        (typedBody, env) <- body.visit(env)
      yield (AST.While(typedCondition, typedBody, line), env)

  given MatrixTyper[AST.For] =
    case (AST.For(varRef, range, body, line), env) =>
      for
        (typedRange, env) <- range.visit(env)
        (typedVarRef, env) <- varRef.visit(env)
        (typedBody, env) <- body.visit(env + (varRef.name -> Type.Int))
      yield (AST.For(typedVarRef, typedRange, typedBody, line), env)

  given MatrixTyper[AST.Return] =
    case (AST.Return(expr, line), env) =>
      for (typedExpr, env) <- expr.visit(env)
      yield (AST.Return(typedExpr, line), env)

  given MatrixTyper[AST.Continue] = (c, env) => Result((c, env))

  given MatrixTyper[AST.Break] = (b, env) => Result((b, env))

  given MatrixTyper[AST.Literal] = (l, env) => Result((l, env))

  given MatrixTyper[AST.SymbolRef] =
    (ref, env) =>
      val tpe = env.getOrElse(ref.name, Type.Undef)
      Result((ref.copy(tpe = tpe), env))

  given MatrixTyper[AST.VectorRef] =
    case (AST.VectorRef(vector, element, line), env) =>
      for
        (typedVector, env) <- vector.visit(env)
        _ <- typedVector.expectToBe(Type.Vector())
        (typedElement, env) <- element.visit(env)
        _ <- typedElement.expectToBe(Type.Numerical)
      yield (AST.VectorRef(typedVector, typedElement, line), env)

  given MatrixTyper[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row, col, line), env) =>
      for
        (typedMatrix, env) <- matrix.visit(env)
        _ <- typedMatrix.expectToBe(Type.Matrix())
        (typedRow, env) <- row.visit(env)
        (typedCol, env) <- col.visit(env)
        _ <- if typedRow ne null then typedRow.expectToBe(Type.Int) else Result(null)
        _ <- if typedCol ne null then typedCol.expectToBe(Type.Int) else Result(null)
      yield (AST.MatrixRef(typedMatrix, typedRow, typedCol, line), env)

  given MatrixTyper[AST.Apply] =
    case (AST.Apply(ref, args, _, line), env) =>
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

          case x => throw new NotImplementedError(x.toString)
      yield (AST.Apply(typedRef, typedArgs, typedResult, line), env)

  given MatrixTyper[AST.Range] =
    case (AST.Range(start, end, line), env) =>
      for
        (typedStart, env) <- start.visit(env)
        _ <- typedStart.expectToBe(Type.Int)
        (typedEnd, env) <- end.visit(env)
        _ <- typedEnd.expectToBe(Type.Int)
      yield (AST.Range(typedStart, typedEnd, line), env)
