package example

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

type ScalaResult[+T <: AST.Tree | Null] = Any

object MatrixInterpreter extends TreeTraverser[Env, ScalaResult]:
  extension [T <: AST.Expr: Mapper, R](expr: T) private def eval[X](env: Env): X = expr.visit(env).asInstanceOf[X]

  val handleNull = _ => ()

  override given Mapper[AST.Block] = env => block => block.statements.foreach(_.visit(env))

  override given Mapper[AST.Assign] = env =>
    case AST.Assign(varRef: AST.SymbolRef, expr, line) =>
      env(varRef.name) = expr.visit(env)

    case AST.Assign(varRef: AST.VectorRef, expr, line) =>
      val vector = env.getValue[Vector](varRef.vector.name, line)
      vector(varRef.element.eval[Int](env)) = expr.eval[Int](env)

    case AST.Assign(AST.MatrixRef(matrix, row: AST.Expr, col: AST.Expr, _), expr, line) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(col.eval[Int](env)) = expr.eval[Int](env)

    case _ => ???

  override given Mapper[AST.If] = env =>
    case AST.If(condition, thenBranch, elseBranch, _) =>
      if condition.eval[Boolean](env) then thenBranch.visit(Env(env))
      else if elseBranch != null then elseBranch.visit(Env(env))

  override given Mapper[AST.While] = env =>
    case AST.While(condition, body, _) =>
      breakable:
        while condition.eval[Boolean](env) do
          try body.visit(Env(env))
          catch
            case Exit.BreakException => break()
            case Exit.ContinueException => ()

  override given Mapper[AST.For] = env =>
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

  override given Mapper[AST.Return] = env =>
    case AST.Return(expr, _) =>
      throw Exit.ReturnException(expr)

  override given Mapper[AST.Continue] = env => throw Exit.ContinueException

  override given Mapper[AST.Break] = env => throw Exit.BreakException

  override given Mapper[AST.Literal] = env =>
    case AST.Literal(tpe, value, _) =>
      value

  override given Mapper[AST.SymbolRef] = env =>
    case AST.SymbolRef(tpe, name, line) =>
      env.getValue[Any](name, line)

  override given Mapper[AST.VectorRef] = env =>
    case AST.VectorRef(vector, element, _) =>
      vector.eval[Vector](env)(element.eval[Int](env))

  override given Mapper[AST.MatrixRef] = env =>
    case AST.MatrixRef(matrix, row: AST.Expr, column: AST.Expr, line) =>
      env.getValue[Matrix](matrix.name, line)(row.eval[Int](env))(column.eval[Int](env))
    case _ => ???

  override given Mapper[AST.Apply] = env =>
    case AST.Apply(ref, args, _, line) =>
      env.getFunction(ref.name, line).apply(args.map(_.visit(env)).toTuple)

  override given Mapper[AST.Range] = env =>
    case AST.Range(start, end, _) =>
      start.eval[Int](env) until end.eval[Int](env)
