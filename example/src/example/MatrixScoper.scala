package example

import example.AST.{MatrixRef, SymbolRef, VectorRef}

import java.util.concurrent.atomic.AtomicBoolean
import scala.languageFeature.experimental.macros
import example.MatrixScoper.visit
import example.MatrixScoper.given

case class Scope(
  parent: Scope | Null,
  key: AST.Tree,
  inLoop: Boolean,
  symbols: Map[String, AST.SymbolRef] = Map.empty,
  children: Map[AST.Tree, Scope] = Map.empty,
):

  def get(name: String): Option[AST.SymbolRef] =
    if symbols.contains(name) then Some(symbols(name))
    else if parent != null then parent.get(name)
    else None

  def contains(symbol: AST.SymbolRef): Boolean =
    symbols.contains(symbol.name) || (parent != null && parent.contains(symbol))

  def withNewScope(tree: AST.Tree, inLoop: Boolean = this.inLoop): Scope =
    val child = Scope(this, key, inLoop).withChildren(tree.children*)
    this.copy(children = this.children + (key -> child))

  def withChildren(children: AST.Tree*): Scope =
    this.copy(children = this.children ++ children.map(ch => ch -> ch.visit(this)).toMap)

  def withSymbol(symbol: AST.SymbolRef): Scope =
    this.copy(symbols = this.symbols + (symbol.name -> symbol))

  def popScope(): Scope | Null = parent

trait MatrixScoper[T <: AST.Tree]:
  def visit(tree: T, scope: Scope): Scope

object MatrixScoper:

  extension [T <: AST.Tree: MatrixScoper as scoper](tree: T) def visit(scope: Scope): Scope = scoper.visit(tree, scope)

  given MatrixScoper[AST.Tree] = (tree, scope) =>
    tree match
      case b: AST.Block => b.visit(scope)
      case s: AST.Statement => s.visit(scope)

  given MatrixScoper[AST.Statement] = (statement, scope) =>
    statement match
      case e: AST.Expr => e.visit(scope)
      case a: AST.Assign => a.visit(scope)
      case i: AST.If => i.visit(scope)
      case w: AST.While => w.visit(scope)
      case f: AST.For => f.visit(scope)
      case r: AST.Return => r.visit(scope)
      case c: AST.Continue => c.visit(scope)
      case b: AST.Break => b.visit(scope)

  given MatrixScoper[AST.Expr] = (expr, scope) =>
    expr match
      case l: AST.Literal => l.visit(scope)
      case r: AST.SymbolRef => r.visit(scope)
      case v: AST.VectorRef => v.visit(scope)
      case m: AST.MatrixRef => m.visit(scope)
      case a: AST.Apply => a.visit(scope)
      case r: AST.Range => r.visit(scope)

  given MatrixScoper[AST.Block] =
    case (block, scope) =>
      scope.withNewScope(block)

  given MatrixScoper[AST.Assign] =
    case (AST.Assign(varRef: AST.SymbolRef, expr, _), scope) =>
      val newVarRef = varRef.copy(tpe = expr.tpe)
      scope.withChildren(expr).withSymbol(newVarRef)
    case _ => ???

  given MatrixScoper[AST.If] =
    case (AST.If(condition, thenBranch, null, _), scope) =>
      scope
        .withChildren(condition)
        .withNewScope(thenBranch)
    case (AST.If(condition, thenBranch, elseBranch: AST.Block, _), scope) =>
      scope
        .withChildren(condition)
        .withNewScope(thenBranch)
        .withNewScope(elseBranch)

  given MatrixScoper[AST.While] =
    case (AST.While(condition, body, _), scope) =>
      scope
        .withChildren(condition)
        .withNewScope(body, inLoop = true)

  given MatrixScoper[AST.For] =
    case (AST.For(varRef, range, body, _), scope) =>
      scope
        .withChildren(range.start, range.end)
        .withSymbol(varRef)
        .withNewScope(body, inLoop = true)

  given MatrixScoper[AST.Return] =
    case (AST.Return(expr, _), scope) =>
      scope.withChildren(expr)

  given MatrixScoper[AST.Continue] =
    case (_, scope) if scope.inLoop => scope
    case (tree, _) => throw CompilerException("Continue outside loop", tree.line)

  given MatrixScoper[AST.Break] =
    case (_, scope) if scope.inLoop => scope
    case (tree, _) => throw CompilerException("Break outside loop", tree.line)

  given MatrixScoper[AST.Literal] =
    case (_, scope) => scope

  given MatrixScoper[AST.SymbolRef] =
    case (AST.SymbolRef(tpe, name, line), scope) =>
      val symbol = scope.get(name)
      if symbol.isEmpty then throw Exception(s"Undefined variable $name at line $line")
      else
        // ref.type = symbol.type
        scope

  given MatrixScoper[AST.VectorRef] =
    case (AST.VectorRef(vector, element, _), scope) =>
      scope.withChildren(vector, element)

  given MatrixScoper[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row, column, _), scope) =>
      scope.withChildren(Seq(matrix, row, column).collectNonNullable*)

  given MatrixScoper[AST.Apply] =
    case (AST.Apply(ref, args, _), scope) =>
      scope.withChildren(ref :: args*)
  //    def visit_Apply(self, apply: Apply) -> None:
  //        self.visit(apply.ref)
  //        self.visit_all(apply.args)
  //        arg_types = [arg.type for arg in apply.args]
  //
  //        if not isinstance(apply.ref, SymbolRef):
  //            raise NotImplementedError
  //
  //        if isinstance(apply.ref.type, AnyOf):
  //            apply.ref.type = next(
  //                (type_ for type_ in apply.ref.type.all if
  //                 isinstance(type_, TS.Function) and type_.takes(arg_types)),
  //                TS.undef()
  //            )
  //            if apply.ref.type == TS.undef():
  //                apply.type = TS.undef()
  //            elif isinstance(apply.ref.type, TS.Function):
  //                if not apply.ref.type.result.is_final:
  //                    assert isinstance(apply.ref.type, TS.FunctionTypeFactory)
  //                    apply.ref.type = self.handle_result(apply.ref.type(apply.args), apply.lineno)
  //                assert isinstance(apply.ref.type, TS.Function)
  //                apply.type = apply.ref.type.result
  //            else:
  //                raise NotImplementedError
  //        elif isinstance(apply.ref.type, TS.Function):
  //            if not apply.ref.type.takes(arg_types):
  //                apply.type = TS.undef()
  //            else:
  //                if not apply.ref.type.result.is_final:
  //                    assert isinstance(apply.ref.type, TS.FunctionTypeFactory)
  //                    apply.ref.type = self.handle_result(apply.ref.type(apply.args), apply.lineno)
  //                assert isinstance(apply.ref.type, TS.Function)
  //                apply.type = apply.ref.type.result
  //        else:
  //            raise NotImplementedError
  //

  given MatrixScoper[AST.Range] =
    case (AST.Range(start, end, _), scope) =>
      scope.withChildren(start, end)
    //      if isinstance(range_.start, SymbolRef) and range_.start.type != TS.Int():
    //          report_error(self, f"Expected Int, got {range_.start.type}", range_.lineno)
    //      if isinstance(range_.end, SymbolRef) and range_.end.type != TS.Int():
    //          report_error(self, f"Expected Int, got {range_.end.type}", range_.lineno)
