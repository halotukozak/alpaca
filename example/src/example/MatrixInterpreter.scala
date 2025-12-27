package example

import example.runtime.*

import scala.collection.mutable
import scala.reflect.Typeable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NoStackTrace

enum Exit extends RuntimeException with NoStackTrace:
  case ReturnException(value: Any)
  case BreakException
  case ContinueException

case class Environment(
  parent: Environment | Null,
  memory: mutable.Map[String, Any] = mutable.Map.empty,
  functions: mutable.Map[String, runtime.Function] = mutable.Map.empty,
):

  def contains(name: String): Boolean =
    this.memory.contains(name) || (this.parent != null && this.parent.contains(name))

  def update(name: String, value: Any): Unit =
    if this.memory.contains(name) then this.memory(name) = value
    else if this.parent != null && this.parent.contains(name) then this.parent.update(name, value)
    else this.memory(name) = value

  def getRuntimeValue(name: String, line: Int): Any =
    this.memory
      .get(name)
      .orElse(Option(this.parent).map(parent => parent.getRuntimeValue(name, line)))
      .getOrElse:
        throw MatrixRuntimeException(s"Variable $name not found", line)

  def getRuntimeFunction(name: String, line: Int): runtime.Function =
    this.functions
      .get(name)
      .orElse(Option(this.parent).map(parent => parent.getRuntimeFunction(name, line)))
      .getOrElse:
        throw MatrixRuntimeException(s"Function $name not found", line)

type ScalaResult[+T <: AST.Tree | Null] = Any

object MatrixInterpreter extends TreeTraverser[Environment, ScalaResult]:
  extension [T <: AST.Expr: Process, R](expr: T)
    private def eval[X](env: Environment): X = expr.visit(env).asInstanceOf[X]

  override val handleNull: Environment => Unit = _ => ()

  override given Process[AST.Block] = env => block => block.statements.foreach(_.visit(env))

  override given Process[AST.Assign] = env =>
    case AST.Assign(varRef: AST.SymbolRef, expr, line) =>
      env(varRef.name) = expr.visit(env)

    case AST.Assign(varRef: AST.VectorRef, expr, line) =>
      val vector = env.getRuntimeValue(varRef.vector.name, line) match
        case v: Vector => v
        case _ => throw MatrixRuntimeException(s"Variable ${varRef.vector.name} is not a vector", line)

      vector(varRef.element.eval[Int](env)) = expr.eval[Number](env)

    case AST.Assign(varRef: AST.MatrixRef, expr, line) =>
      val matrix = env.getRuntimeValue(varRef.matrix.name, line) match
        case m: Matrix => m
        case _ => throw MatrixRuntimeException(s"Variable ${varRef.matrix.name} is not a matrix", line)

      matrix(varRef.row.eval[Int](env))(varRef.col.eval[Int](env)) = expr.eval[Number](env)

  override given Process[AST.If] = env =>
    case AST.If(condition, thenBranch, elseBranch, _) =>
      if condition.eval[Boolean](env) then thenBranch.visit(Environment(env))
      else if elseBranch != null then elseBranch.visit(Environment(env))

  override given Process[AST.While] = env =>
    case AST.While(condition, body, _) =>
      breakable:
        while condition.eval[Boolean](env) do
          try body.visit(Environment(env))
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  override given Process[AST.For] = env =>
    case AST.For(varRef, range, body, _) =>
      breakable:
        for i <- range.start.eval[Int](env) until range.end.eval[Int](env) do
          try
            val loopEnv = Environment(env)
            loopEnv(varRef.name) = i
            body.visit(loopEnv)
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  override given Process[AST.Return] = env =>
    case AST.Return(expr, _) => throw Exit.ReturnException(expr.visit(env))

  override given Process[AST.Continue] = env => throw Exit.ContinueException

  override given Process[AST.Break] = env => throw Exit.BreakException

  override given Process[AST.Literal] = env =>
    case AST.Literal(tpe, value, _) =>
      value

  override given Process[AST.SymbolRef] = env =>
    case AST.SymbolRef(tpe, name, line) =>
      env.getRuntimeValue(name, line)

  override given Process[AST.VectorRef] = env =>
    case AST.VectorRef(vector, element, _) =>
      vector.eval[Vector](env)(element.eval[Int](env))

  override given Process[AST.MatrixRef] = env =>
    case AST.MatrixRef(matrix, row: AST.Expr, column: AST.Expr, line) =>
      matrix.eval[Matrix](env)(row.eval[Int](env))(column.eval[Int](env))

  override given Process[AST.Apply] = env =>
    case AST.Apply(ref, args, _, line) =>
      env.getRuntimeFunction(ref.name, line).apply(args.map(_.visit(env)))

  override given Process[AST.Range] = env =>
    case AST.Range(start, end, _) =>
      start.eval[Int](env) until end.eval[Int](env)
