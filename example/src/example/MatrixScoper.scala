package example

case class Scope(
  key: AST.Tree,
  parent: Scope | Null,
  inLoop: Boolean,
  symbols: Map[String, AST.SymbolRef] = Map.empty,
):
  def get(name: String): Option[AST.SymbolRef] =
    if symbols.contains(name) then Some(symbols(name))
    else if parent != null then parent.get(name)
    else None

  def withSymbol(symbol: AST.SymbolRef): Scope =
    this.copy(symbols = this.symbols + (symbol.name -> symbol))

trait MatrixScoper[T <: AST.Tree]:
  def visit(tree: T, scope: Scope): Result[Scope]

object MatrixScoper:

  extension [T <: AST.Tree: MatrixScoper as scoper](tree: T)
    def visit(scope: Scope): Result[Scope] = scoper.visit(tree, scope)

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
      case r: AST.Ref => r.visit(scope)
      case a: AST.Apply => a.visit(scope)
      case r: AST.Range => r.visit(scope)

  given MatrixScoper[AST.Ref] = (ref, scope) =>
    ref match
      case r: AST.SymbolRef => r.visit(scope)
      case v: AST.VectorRef => v.visit(scope)
      case m: AST.MatrixRef => m.visit(scope)

  given MatrixScoper[AST.Block] =
    case (block, scope) =>
      val newScope = Result(Scope(block, scope, scope.inLoop))
      block.statements.foldLeft(newScope)((scope, ast) => scope.flatMap(ast.visit(_)))

  given MatrixScoper[AST.Assign] =
    case (AST.Assign(ref: AST.SymbolRef, expr, _), scope) =>
      val newVarRef = ref.copy(tpe = expr.tpe)
      expr.visit(scope).map(_.withSymbol(newVarRef))

    case (AST.Assign(ref, expr, line), scope) =>
      for
        _ <- ref.visit(scope)
        _ <- expr.visit(scope)
      yield scope

  given MatrixScoper[AST.If] =
    case (AST.If(condition, thenBranch, elseBranch, _), scope) =>
      for
        _ <- condition.visit(scope)
        _ <- thenBranch.visit(scope)
        _ <- if elseBranch != null then elseBranch.visit(scope) else Result.unit
      yield scope

  given MatrixScoper[AST.While] =
    case (AST.While(condition, body, _), scope) =>
      for
        _ <- condition.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true))
      yield scope

  given MatrixScoper[AST.For] =
    case (AST.For(varRef, range, body, _), scope) =>
      for
        _ <- range.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true).withSymbol(varRef))
      yield scope

  given MatrixScoper[AST.Return] =
    case (AST.Return(expr, _), scope) =>
      for _ <- expr.visit(scope)
      yield scope

  given MatrixScoper[AST.Continue] =
    case (_, scope) if scope.inLoop => scope
    case (tree, scope) => Result.error(tree.line)(scope, "Continue outside loop")

  given MatrixScoper[AST.Break] =
    case (_, scope) if scope.inLoop => scope
    case (tree, scope) => Result.error(tree.line)(scope, "Break outside loop")

  given MatrixScoper[AST.Literal] =
    case (_, scope) => scope

  given MatrixScoper[AST.SymbolRef] =
    case (AST.SymbolRef(tpe, name, line), scope) =>
      val symbol = scope.get(name)
      if symbol.isEmpty then Result.error(line)(scope, s"Undefined variable $name")
      else scope

  given MatrixScoper[AST.VectorRef] =
    case (AST.VectorRef(vector, element, _), scope) =>
      for
        _ <- vector.visit(scope)
        _ <- element.visit(scope)
      yield scope

  given MatrixScoper[AST.MatrixRef] =
    case (AST.MatrixRef(matrix, row, column, _), scope) =>
      for
        _ <- matrix.visit(scope)
        _ <- if row != null then row.visit(scope) else Result.unit
        _ <- if column != null then column.visit(scope) else Result.unit
      yield scope

  given MatrixScoper[AST.Apply] =
    case (AST.Apply(ref, args, _, _), scope) =>
      for
        _ <- ref.visit(scope)
        _ <- args.foldLeft(Result(scope))((scope, ast) => scope.flatMap(ast.visit(_)))
      yield scope

  given MatrixScoper[AST.Range] =
    case (AST.Range(start, end, _), scope) =>
      for
        _ <- start.visit(scope)
        _ <- end.visit(scope)
      yield scope
