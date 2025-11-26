package example

import example.AST.*

import scala.languageFeature.experimental.macros
import scala.collection.Factory

trait MatrixTyper[T <: AST.Tree | Null]:
  def visit(tree: T): Result[T]

object MatrixTyper:
  extension [T <: AST.Tree | Null: MatrixTyper as typer](tree: T) def visit(): Result[T] = typer.visit(tree)

  extension [T <: AST.Tree | Null: MatrixTyper as typer, CC[X] <: Iterable[X]](trees: CC[T])
    inline private def visitAll(): Result[CC[T]] = Result.traverse(trees)(_.visit())

  extension [T <: AST.Expr](expr: T)
    private def expectToBe(expectedType: Type): Result[T] =
      if expr.tpe <= expectedType then Result(expr)
      else Result.error(expr.line)(expr, s"Expected $expectedType, got ${expr.tpe}")

    private def expectDefined: Result[T] =
      if expr.tpe != Type.Undef then Result(expr)
      else Result.error(expr.line)(expr, s"Type could not be inferred")

  given [T <: AST.Tree | Null: MatrixTyper as typer]: MatrixTyper[T | Null] =
    case (null) => Result(null)
    case (t: T @unchecked) => typer.visit(t)

  given MatrixTyper[AST.Tree] =
    case (b: AST.Block) => b.visit()
    case (s: AST.Statement) => s.visit()

  given MatrixTyper[AST.Statement] =
    case (e: AST.Expr) => e.visit()
    case (a: AST.Assign) => a.visit()
    case (i: AST.If) => i.visit()
    case (w: AST.While) => w.visit()
    case (f: AST.For) => f.visit()
    case (r: AST.Return) => r.visit()
    case (c: AST.Continue) => c.visit()
    case (b: AST.Break) => b.visit()

  given MatrixTyper[AST.Expr] =
    case (l: AST.Literal) => l.visit()
    case (r: AST.Ref) => r.visit()
    case (a: AST.Apply) => a.visit()
    case (r: AST.Range) => r.visit()

  given MatrixTyper[AST.Ref] =
    case (r: AST.SymbolRef) => r.visit()
    case (v: AST.VectorRef) => v.visit()
    case (m: AST.MatrixRef) => m.visit()

  given MatrixTyper[AST.Block] =
    case (AST.Block(statements, line)) =>
      for typedStmts <- statements.visitAll()
      yield AST.Block(typedStmts, line)

  given MatrixTyper[AST.Assign] =
    case (AST.Assign(ref: AST.SymbolRef, expr, line)) =>
      for
        typedExpr <- expr.visit()
        newRef = ref.copy(tpe = typedExpr.tpe)
      yield AST.Assign(newRef, typedExpr, line)

    case (AST.Assign(ref, expr, line)) =>
      for
        typedExpr <- expr.visit()
        typedRef <- ref.visit()
      yield AST.Assign(typedRef, typedExpr, line)

  given MatrixTyper[AST.If] =
    case (AST.If(condition, thenBlock, elseBlock: AST.Block, line)) =>
      for
        typedCondition <- condition.visit()
        _ <- typedCondition.expectToBe(Type.Bool)
        typedThenBlock <- thenBlock.visit()
        typedElseBlock <- elseBlock.visit()
      yield AST.If(typedCondition, typedThenBlock, typedElseBlock, line)
    case (AST.If(condition, thenBlock, null, line)) =>
      for
        typedCondition <- condition.visit()
        _ <- typedCondition.expectToBe(Type.Bool)
        typedThenBlock <- thenBlock.visit()
      yield AST.If(typedCondition, typedThenBlock, null, line)

  given MatrixTyper[AST.While] =
    case (AST.While(condition, body, line)) =>
      for
        typedCondition <- condition.visit()
        _ <- typedCondition.expectToBe(Type.Bool)
        typedBody <- body.visit()
      yield AST.While(typedCondition, typedBody, line)

  given MatrixTyper[AST.For] =
    case (AST.For(varRef, range, body, line)) =>
      for
        typedRange <- range.visit()
        typedVarRef <- varRef.visit()
        _ <- typedVarRef.expectToBe(Type.Int)
        typedBody <- body.visit()
      yield AST.For(typedVarRef, typedRange, typedBody, line)

  given MatrixTyper[AST.Return] =
    case (AST.Return(expr, line)) =>
      for typedExpr <- expr.visit()
      yield AST.Return(typedExpr, line)

  given MatrixTyper[AST.Continue] = c => Result(c)

  given MatrixTyper[AST.Break] = b => Result(b)

  given MatrixTyper[AST.Literal] = l => Result(l)

  given MatrixTyper[AST.SymbolRef] = ref => Result(ref)

  given MatrixTyper[AST.VectorRef] =
    case (AST.VectorRef(vector, element, line)) =>
      for
        typedVector <- vector.visit()
        _ <- vector.expectToBe(Type.Vector())
        typedElement <- element.visit()
        _ <- element.expectToBe(Type.Numerical)
      yield AST.VectorRef(typedVector, typedElement, line)

  given MatrixTyper[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row, col, line)) =>
      for
        typedMatrix <- matrix.visit()
        _ <- matrix.expectToBe(Type.Matrix())
        typedRow <- row.visit()
        typedCol <- col.visit()
        _ <- if typedRow ne null then typedRow.expectToBe(Type.Int) else Result(null)
        _ <- if typedCol ne null then typedCol.expectToBe(Type.Int) else Result(null)
      yield AST.MatrixRef(typedMatrix, typedRow, typedCol, line)

  given MatrixTyper[AST.Apply] =
    case (AST.Apply(ref, args, _, line)) =>
      for
        typedRef <- ref.visit()
        typedArgs <- args.visitAll()
        argTypes = typedArgs.map(_.tpe)
        tpe <- ref.tpe match
          case func: Type.Function =>
            if func.takes(argTypes) then Result(func.result)
            else
              Result.error(line)(
                Type.Undef,
                s"Function ${ref.name} expects ${func.args}, got ${argTypes.mkString("(", ",", ")")}",
              )

          case factory: Type.FunctionTypeFactory =>
            for
              func <- factory(typedArgs)
              result <-
                if func.takes(argTypes) then Result(func.result)
                else
                  Result.error(line)(
                    Type.Undef,
                    s"Function ${ref.name} expects ${func.args}, got ${argTypes.mkString("(", ",", ")")}",
                  )
            yield result

          case anyOf: Type.AnyOf =>
            anyOf.all
              .collectFirst:
                case func: Type.Function if func.takes(argTypes) => Result(func.result)
              .getOrElse(
                Result.error(line)(
                  Type.Undef,
                  s"Function ${ref.name} expects $anyOf, got ${argTypes.mkString("(", ",", ")")}",
                ),
              )

          case x => throw new NotImplementedError(x.toString)
      yield AST.Apply(typedRef, typedArgs, tpe, line)

  given MatrixTyper[AST.Range] =
    case (AST.Range(start, end, line)) =>
      for
        typedStart <- start.visit()
        _ <- typedStart.expectToBe(Type.Int)
        typedEnd <- end.visit()
        _ <- typedEnd.expectToBe(Type.Int)
      yield AST.Range(typedStart, typedEnd, line)
