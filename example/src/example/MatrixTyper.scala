package example

import example.AST.*
import example.MatrixTyper.{visit, given}

import java.util.concurrent.atomic.AtomicBoolean
import scala.languageFeature.experimental.macros

trait MatrixTyper[T <: AST.Tree | Null]:
  def visit(tree: T): Result[T]

object MatrixTyper:

  extension [T <: AST.Tree | Null: MatrixTyper as typer](tree: T) def visit(): Result[T] = typer.visit(tree)
  extension [T <: AST.Tree | Null: MatrixTyper as typer, CC[X] <: Iterable[X]](trees: CC[T])
    inline private def visitAll(): Result[CC[T]] = Result.sequence(trees.map(typer.visit))

  extension [T <: AST.Expr](expr: T)
    private def expectToBe(expectedType: Type): Result[T] =
      if expr.tpe <= expectedType then Result(expr)
      else Result.error(expr, s"Expected $expectedType, got ${expr.tpe}")

    private def expectDefined: Result[T] =
      if expr.tpe != Type.Undef then Result(expr)
      else Result.error(expr, s"Type could not be inferred")

  given [T: MatrixTyper as typer]: MatrixTyper[T | Null] =
    case null => Result(null)
    case t: T => typer.visit(t)
  given MatrixTyper[AST.Tree] =
    case b: AST.Block => b.visit()
    case s: AST.Statement => s.visit()

  given MatrixTyper[AST.Statement] =
    case e: AST.Expr => e.visit()
    case a: AST.Assign => a.visit()
    case i: AST.If => i.visit()
    case w: AST.While => w.visit()
    case f: AST.For => f.visit()
    case r: AST.Return => r.visit()
    case c: AST.Continue => c.visit()
    case b: AST.Break => b.visit()

  given MatrixTyper[AST.Expr] =
    case l: AST.Literal => l.visit()
    case r: AST.SymbolRef => r.visit()
    case v: AST.VectorRef => v.visit()
    case m: AST.MatrixRef => m.visit()
    case a: AST.Apply => a.visit()
    case r: AST.Range => r.visit()

  given MatrixTyper[AST.Block] =
    case AST.Block(statements, line) =>
      statements.visitAll().map(AST.Block(_, line))

  given MatrixTyper[AST.Assign] =
    case AST.Assign(ref: AST.SymbolRef, expr, line) =>
      for
        newRef <- ref.copy(tpe = expr.tpe).visit()
        expr <- expr.visit()
      yield AST.Assign(ref, expr, line)

  given MatrixTyper[AST.If] =
    case AST.If(condition, thenBlock, elseBlock: AST.Block, line) =>
      for
        cond <- condition.expectToBe(Type.Bool)
        thenB <- thenBlock.visit()
        elseB <- elseBlock.visit()
      yield AST.If(cond, thenB, elseB, line)
    case AST.If(condition, thenBlock, null, line) =>
      for
        cond <- condition.expectToBe(Type.Bool)
        thenB <- thenBlock.visit()
      yield AST.If(cond, thenB, null, line)

  given MatrixTyper[AST.While] =
    case AST.While(condition, body, line) =>
      for
        cond <- condition.visit()
        _ <- cond.expectToBe(Type.Bool)
        body <- body.visit()
      yield AST.While(cond, body, line)

  given MatrixTyper[AST.For] =
    case AST.For(varRef, range, body, line) =>
      for
        range <- range.visit()
        _ <- varRef.expectToBe(Type.Int)
        body <- body.visit()
      yield AST.For(varRef, range, body, line)

  given MatrixTyper[AST.Return] =
    case AST.Return(expr, line) =>
      expr.visit().map(AST.Return(_, line))

  given MatrixTyper[AST.Continue] = Result(_)
  given MatrixTyper[AST.Break] = Result(_)
  given MatrixTyper[AST.Literal] = Result(_)
  given MatrixTyper[AST.SymbolRef] = Result(_)

  given MatrixTyper[AST.VectorRef] =
    case (AST.VectorRef(vector, element, line), scope) =>
      for
        vector <- vector.visit()
        _ <- vector.expectToBe(Type.Vector())
        element <- element.visit()
        _ <- element.expectToBe(Type.Numerical)
      yield AST.VectorRef(vector, element, line)

  given MatrixTyper[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row, col, line), scope) =>
      for
        matrix <- matrix.visit()
        validatedMatrix <- matrix.expectToBe(Type.Matrix())
        row <- row.visit()
        col <- col.visit()
        _ <- if row ne null then row.expectToBe(Type.Int) else Result(null)
        _ <- if col ne null then col.expectToBe(Type.Int) else Result(null)
      yield AST.MatrixRef(matrix, row, col, line)

  given MatrixTyper[AST.Apply] =
    case (AST.Apply(ref, args, line), scope) =>
      for
        ref <- ref.visit()
        args <- args.visitAll()
        _ <- ref.tpe match
          case func: Type.Function =>

//            /         if isinstance(ref_type, TS.Function):
//             if ref_type.arity is not None and len(apply.args) != ref_type.arity:
//                 optional_s = "" if ref_type.arity == 1 else "s"
//                 report_error(self,
//                              f"Function {apply.ref.name} expects {ref_type.arity} argument{optional_s}, got {len(apply.args)}",
//                              apply.lineno)
//             match ref_type.args:
//                 case None:
//                     raise NotImplementedError
//                 case TS.VarArg(expected_type):
//                     for arg in apply.args:
//                         if arg.type != expected_type:
//                             report_error(self,
//                                          f"Function {apply.ref.name} expects {expected_type} arguments, got {arg.type}",
//                                          apply.lineno)
//                 case TS.Type():
//                     if apply.args[0].type != ref_type.args:
//                         report_error(self,
//                                      f"Function {apply.ref.name} expects {ref_type.args} argument, got {apply.args[0].type}",
//                                      apply.lineno)
//                 case _:  # Tuple
//                     for arg, expected_type in zip(apply.args, ref_type.args):
//                         if arg.type != expected_type:
//                             report_error(self,
//                                          f"Function {apply.ref.name} expects {expected_type} arguments, got {arg.type}",
//                                          apply.lineno)
            ???
          case Type.Undef =>
            Result.error(ref.line)(s"Undefined function ${ref.name} with ${args.map(_.tpe)} arguments")
          case _ => ???
      yield AST.Apply(ref, args, line)

  given MatrixTyper[AST.Range] =
    case AST.Range(start, end, line) =>
      for
        start <- start.expectToBe(Type.Int)
        end <- end.expectToBe(Type.Int)
      yield AST.Range(start, end, line)
