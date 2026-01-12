package example

case class Scope(
  key: AST.Tree,
  parent: Scope | Null,
  inLoop: Boolean,
  symbols: Map[String, AST.SymbolRef] = Map.empty,
):
  def getSymbol(name: String): Option[AST.SymbolRef] =
    if symbols.contains(name) then Some(symbols(name))
    else if parent != null then parent.getSymbol(name)
    else None

  def withSymbol(symbol: AST.SymbolRef): Scope =
    this.copy(symbols = this.symbols + (symbol.name -> symbol))

object MatrixScoper extends TreeAccumulator[Scope]:
  val handleNull: Scope => Nothing = _ => ???

  override given Process[AST.Block] = scope =>
    block =>
      val newScope = Result(Scope(block, scope, scope.inLoop))
      block.statements.foldLeft(newScope)((scope, ast) => scope.flatMap(ast.visit))

  override given Process[AST.Assign] = scope =>
    case AST.Assign(ref: AST.SymbolRef, expr, _) =>
      val newVarRef = ref.copy(tpe = expr.tpe)
      expr.visit(scope).map(_.withSymbol(newVarRef))

    case AST.Assign(ref, expr, line) =>
      for
        _ <- ref.visit(scope)
        _ <- expr.visit(scope)
      yield scope

  override given Process[AST.If] = scope =>
    case AST.If(condition, thenBranch, elseBranch, _) =>
      for
        _ <- condition.visit(scope)
        _ <- thenBranch.visit(scope)
        _ <- if elseBranch != null then elseBranch.visit(scope) else Result.unit
      yield scope

  override given Process[AST.While] = scope =>
    case AST.While(condition, body, _) =>
      for
        _ <- condition.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true))
      yield scope

  override given Process[AST.For] = scope =>
    case AST.For(varRef, range, body, _) =>
      for
        _ <- range.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true).withSymbol(varRef))
      yield scope

  override given Process[AST.Return] = scope =>
    case AST.Return(expr, _) =>
      for _ <- expr.visit(scope)
      yield scope

  override given Process[AST.Continue] = scope =>
    case _ if scope.inLoop => Result(scope)
    case tree => Result.error(tree.line)(scope, "Continue outside loop")

  override given Process[AST.Break] = scope =>
    case _ if scope.inLoop => Result(scope)
    case tree => Result.error(tree.line)(scope, "Break outside loop")

  override given Process[AST.Literal] = scope => _ => Result(scope)

  override given Process[AST.SymbolRef] = scope =>
    case AST.SymbolRef(tpe, name, line) =>
      val symbol = scope.getSymbol(name)
      if symbol.isEmpty then Result.error(line)(scope, s"Undefined variable $name")
      else Result(scope)

  override given Process[AST.VectorRef] = scope =>
    case AST.VectorRef(vector, element, _) =>
      for
        _ <- vector.visit(scope)
        _ <- element.visit(scope)
      yield scope

  override given Process[AST.MatrixRef] = scope =>
    case AST.MatrixRef(matrix, row, column, _) =>
      for
        _ <- matrix.visit(scope)
        _ <- row.visit(scope)
        _ <- column.visit(scope)
      yield scope

  override given Process[AST.Apply] = scope =>
    case AST.Apply(ref, args, _, _) =>
      for
        _ <- ref.visit(scope)
        _ <- args.foldLeft(Result(scope))((scope, ast) => scope.flatMap(ast.visit))
      yield scope

  override given Process[AST.Range] = scope =>
    case AST.Range(start, end, _) =>
      for
        _ <- start.visit(scope)
        _ <- end.visit(scope)
      yield scope
