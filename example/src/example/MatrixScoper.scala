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

object MatrixScoper extends TreeAccumulator[Scope]:
  val handleNull = _ => ???

  override given Mapper[AST.Block] = scope =>
    block =>
      val newScope = Result(Scope(block, scope, scope.inLoop))
      block.statements.foldLeft(newScope)((scope, ast) => scope.flatMap(ast.visit(_)))

  override given Mapper[AST.Assign] = scope =>
    case AST.Assign(ref: AST.SymbolRef, expr, _) =>
      val newVarRef = ref.copy(tpe = expr.tpe)
      expr.visit(scope).map(_.withSymbol(newVarRef))

    case AST.Assign(ref, expr, line) =>
      for
        _ <- ref.visit(scope)
        _ <- expr.visit(scope)
      yield scope

  override given Mapper[AST.If] = scope =>
    case AST.If(condition, thenBranch, elseBranch, _) =>
      for
        _ <- condition.visit(scope)
        _ <- thenBranch.visit(scope)
        _ <- if elseBranch != null then elseBranch.visit(scope) else Result.unit
      yield scope

  override given Mapper[AST.While] = scope =>
    case AST.While(condition, body, _) =>
      for
        _ <- condition.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true))
      yield scope

  override given Mapper[AST.For] = scope =>
    case AST.For(varRef, range, body, _) =>
      for
        _ <- range.visit(scope)
        _ <- body.visit(scope.copy(inLoop = true).withSymbol(varRef))
      yield scope

  override given Mapper[AST.Return] = scope =>
    case AST.Return(expr, _) =>
      for _ <- expr.visit(scope)
      yield scope

  override given Mapper[AST.Continue] = scope =>
    case _ if scope.inLoop => scope
    case tree => Result.error(tree.line)(scope, "Continue outside loop")

  override given Mapper[AST.Break] = scope =>
    case _ if scope.inLoop => scope
    case tree => Result.error(tree.line)(scope, "Break outside loop")

  override given Mapper[AST.Literal] = scope => _ => scope

  override given Mapper[AST.SymbolRef] = scope =>
    case AST.SymbolRef(tpe, name, line) =>
      val symbol = scope.get(name)
      if symbol.isEmpty then Result.error(line)(scope, s"Undefined variable $name")
      else scope

  override given Mapper[AST.VectorRef] = scope =>
    case AST.VectorRef(vector, element, _) =>
      for
        _ <- vector.visit(scope)
        _ <- element.visit(scope)
      yield scope

  override given Mapper[AST.MatrixRef] = scope =>
    case AST.MatrixRef(matrix, row, column, _) =>
      for
        _ <- matrix.visit(scope)
        _ <- if row != null then row.visit(scope) else Result.unit
        _ <- if column != null then column.visit(scope) else Result.unit
      yield scope

  override given Mapper[AST.Apply] = scope =>
    case AST.Apply(ref, args, _, _) =>
      for
        _ <- ref.visit(scope)
        _ <- args.foldLeft(Result(scope))((scope, ast) => scope.flatMap(ast.visit(_)))
      yield scope

  override given Mapper[AST.Range] = scope =>
    case AST.Range(start, end, _) =>
      for
        _ <- start.visit(scope)
        _ <- end.visit(scope)
      yield scope
