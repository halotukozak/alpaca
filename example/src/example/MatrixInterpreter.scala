package example

import example.MatrixInterpreter.ScalaResult

import scala.collection.mutable
import scala.languageFeature.experimental.macros
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NoStackTrace

enum Exit extends RuntimeException with NoStackTrace:
  case ReturnException(value: Any)
  case BreakException
  case ContinueException

case class Env(
  parent: Env | Null,
  memory: mutable.Map[String, Any] = mutable.Map.empty,
  functions: mutable.Map[String, DynamicFunction] = mutable.Map.empty,
):

  def contains(name: String): Boolean =
    this.memory.contains(name) || (this.parent != null && this.parent.contains(name))

  def update(name: String, value: Any): Unit =
    if this.memory.contains(name) then this.memory(name) = value
    else if this.parent != null && this.parent.contains(name) then this.parent.update(name, value)
    else this.memory(name) = value

  def getValue[T](name: String, line: Int): T =
    this.memory
      .get(name)
      .orElse:
        for
          parent <- Option(this.parent)
          value <- Option(parent.getValue(name, line))
        yield value
      .map(_.asInstanceOf[T])
      .getOrElse:
        throw MatrixRuntimeException(s"Variable $name not found", line)

  def getFunction(name: String, line: Int): DynamicFunction =
    this.functions
      .get(name)
      .orElse:
        for
          parent <- Option(this.parent)
          func <- Option(parent.getFunction(name, line))
        yield func
      .getOrElse:
        throw MatrixRuntimeException(s"Function $name not found", line)

trait MatrixInterpreter[T <: AST.Tree]:
  def visit(tree: T, env: Env): ScalaResult[T]

object MatrixInterpreter:

  type ScalaResult[T <: AST.Tree] = Any
//
//    T match
//    case AST.Literal => Any
//    case AST.SymbolRef => Any
//    case AST.VectorRef => Numerical
//    case AST.MatrixRef => Numerical
//    case AST.Apply => Unit
//    case AST.Range => Range
//    case AST.Assign => Unit
//    case AST.If => Unit
//    case AST.While => Unit
//    case AST.For => Unit
//    case AST.Return => Nothing
//    case AST.Continue => Nothing
//    case AST.Break => Nothing
//    case AST.Block => Unit
//    case _ => Unit

  extension [T <: AST.Tree: MatrixInterpreter as interpreter](tree: T)
    inline def visit(env: Env): ScalaResult[T] = interpreter.visit(tree, env)

  extension [T <: AST.Expr: MatrixInterpreter, R](expr: T)
    private def eval[X](env: Env): X = expr.visit(env).asInstanceOf[X]

  given MatrixInterpreter[AST.Tree] = (tree, env) =>
    tree match
      case b: AST.Block => b.visit(env)
      case s: AST.Statement => s.visit(env)

  given MatrixInterpreter[AST.Statement] = (statement, env) =>
    statement match
      case e: AST.Expr => e.visit(env)
      case a: AST.Assign => a.visit(env)
      case i: AST.If => i.visit(env)
      case w: AST.While => w.visit(env)
      case f: AST.For => f.visit(env)
      case r: AST.Return => r.visit(env)
      case c: AST.Continue => c.visit(env)
      case b: AST.Break => b.visit(env)

  given MatrixInterpreter[AST.Expr] = (expr, env) =>
    expr match
      case l: AST.Literal => l.visit(env)
      case r: AST.SymbolRef => r.visit(env)
      case v: AST.VectorRef => v.visit(env)
      case m: AST.MatrixRef => m.visit(env)
      case a: AST.Apply => a.visit(env)
      case r: AST.Range => r.visit(env)

  given MatrixInterpreter[AST.Block] =
    case (AST.Block(statements, _), env) =>
      statements.foreach(_.visit(env))

  given MatrixInterpreter[AST.Assign] =
    case (AST.Assign(varRef: AST.SymbolRef, expr, line), env) =>
      env(varRef.name) = expr.visit(env)

    case (AST.Assign(varRef: AST.VectorRef, expr, line), env) =>
      val vector = env.getValue[Vector](varRef.vector.name, line)
      vector(varRef.element.eval[Int](env)) = expr.eval[Int](env)

    case (AST.Assign(AST.MatrixRef(matrix, row: AST.Expr, col: AST.Expr, _), expr, line), env) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(col.eval[Int](env)) = expr.eval[Int](env)

    case _ => ???

  given MatrixInterpreter[AST.If] =
    case (AST.If(condition, thenBranch, elseBranch, _), env) =>
      if condition.eval[Boolean](env) then thenBranch.visit(Env(env))
      else if elseBranch != null then elseBranch.visit(Env(env))

  given MatrixInterpreter[AST.While] =
    case (AST.While(condition, body, _), env) =>
      breakable:
        while condition.eval[Boolean](env) do
          try body.visit(Env(env))
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  given MatrixInterpreter[AST.For] =
    case (AST.For(varRef, range, body, _), env) =>
      breakable:
        for i <- range.start.eval[Int](env) until range.end.eval[Int](env) do
          try
            val loopEnv = Env(env)
            loopEnv(varRef.name) = i
            body.visit(loopEnv)
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  given MatrixInterpreter[AST.Return] =
    case (AST.Return(expr, _), env) =>
      throw Exit.ReturnException(expr)

  given MatrixInterpreter[AST.Continue] =
    throw Exit.ContinueException

  given MatrixInterpreter[AST.Break] =
    throw Exit.BreakException

  given MatrixInterpreter[AST.Literal] =
    case (AST.Literal(tpe, value, _), env) =>
      value

  given MatrixInterpreter[AST.SymbolRef] =
    case (AST.SymbolRef(tpe, name, line), env) =>
      env.getValue[Any](name, line)

  given MatrixInterpreter[AST.VectorRef] =
    case (AST.VectorRef(vector, element, _), env) =>
      vector.eval[Vector](env)(element.eval[Int](env))

  given MatrixInterpreter[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row: AST.Expr, column: AST.Expr, line), env) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(column.eval[Int](env))
    case _ => ???

  given MatrixInterpreter[AST.Apply] =
    case (AST.Apply(ref, args, _, line), env) =>
      env.getFunction(ref.name, line).apply(args.map(_.visit(env)).toTuple)

  given MatrixInterpreter[AST.Range] =
    case (AST.Range(start, end, _), env) =>
      start.eval[Int](env) until end.eval[Int](env)
