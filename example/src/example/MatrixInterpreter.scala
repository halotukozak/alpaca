package example

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NoStackTrace

import runtime._

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

type ScalaResult[+T <: AST.Tree | Null] = Any

object MatrixInterpreter extends TreeTraverser[Env, ScalaResult]:
  extension [T <: AST.Expr: Process, R](expr: T) private def eval[X](env: Env): X = expr.visit(env).asInstanceOf[X]

  val handleNull: Env => Unit = _ => ()

  override given Process[AST.Block] = env => block => block.statements.foreach(_.visit(env))

  override given Process[AST.Assign] = env =>
    case AST.Assign(varRef: AST.SymbolRef, expr, line) =>
      env(varRef.name) = expr.visit(env)

    case AST.Assign(varRef: AST.VectorRef, expr, line) =>
      val vector = env.getValue[Vector](varRef.vector.name, line)
      vector(varRef.element.eval[Int](env)) = expr.eval[Number](env)

    case AST.Assign(AST.MatrixRef(matrix, row: AST.Expr, col: AST.Expr, _), expr, line) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(col.eval[Int](env)) = expr.eval[Number](env)

    case _ => ???

  override given Process[AST.If] = env =>
    case AST.If(condition, thenBranch, elseBranch, _) =>
      if condition.eval[Boolean](env) then thenBranch.visit(Env(env))
      else if elseBranch != null then elseBranch.visit(Env(env))

  override given Process[AST.While] = env =>
    case AST.While(condition, body, _) =>
      breakable:
        while condition.eval[Boolean](env) do
          try body.visit(Env(env))
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  override given Process[AST.For] = env =>
    case AST.For(varRef, range, body, _) =>
      breakable:
        for i <- range.start.eval[Int](env) until range.end.eval[Int](env) do
          try
            val loopEnv = Env(env)
            loopEnv(varRef.name) = i
            body.visit(loopEnv)
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  override given Process[AST.Return] = env =>
    case AST.Return(expr, _) =>
      throw Exit.ReturnException(expr)

  override given Process[AST.Continue] = env => throw Exit.ContinueException

  override given Process[AST.Break] = env => throw Exit.BreakException

  override given Process[AST.Literal] = env =>
    case AST.Literal(tpe, value, _) =>
      value

  override given Process[AST.SymbolRef] = env =>
    case AST.SymbolRef(tpe, name, line) =>
      env.getValue[Any](name, line)

  override given Process[AST.VectorRef] = env =>
    case AST.VectorRef(vector, element, _) =>
      vector.eval[Vector](env)(element.eval[Int](env))

  override given Process[AST.MatrixRef] = env =>
    case AST.MatrixRef(matrix, row: AST.Expr, column: AST.Expr, line) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(column.eval[Int](env))
    case _ => ???

  override given Process[AST.Apply] = env =>
    case AST.Apply(ref, args, _, line) =>
      env.getFunction(ref.name, line).apply(args.map(_.visit(env)).toTuple)

  override given Process[AST.Range] = env =>
    case AST.Range(start, end, _) =>
      start.eval[Int](env) until end.eval[Int](env)
