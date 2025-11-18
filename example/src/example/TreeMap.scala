package example

trait TreeAccumulator[X]:

  // Ties the knot of the traversal: call `foldOver(x, tree))` to dive in the `tree` node.
  def foldTree(x: X, tree: AST.Tree): X

  final def foldTrees(x: X, trees: Iterable[AST.Tree]): X = trees.foldLeft(x)((acc, y) => foldTree(acc, y))

  final def foldNullableTree(x: X, tree: AST.Tree | Null): X = if tree.ne(null) then foldOverTree(x, tree) else x

  final def foldOverTree(x: X, tree: AST.Tree): X = tree match
    case AST.Block(statements, _) => foldTrees(x, statements)
    case AST.Literal(_, _, _) => x
    case AST.SymbolRef(_, _, _) => x
    case AST.VectorRef(vector, element, _) => foldTree(foldTree(x, vector), element)
    case AST.MatrixRef(matrix, row, col, _) => foldNullableTree(foldNullableTree(foldTree(x, matrix), row), col)
    case AST.Apply(ref, args, _) => foldTrees(foldTree(x, ref), args)
    case AST.Range(start, end, _) => foldTree(foldTree(x, start), end)
    case AST.Assign(varRef, expr, _) => foldTree(foldTree(x, varRef), expr)
    case AST.If(condition, thenBlock, elseBlock, _) =>
      foldNullableTree(foldNullableTree(foldTree(x, condition), thenBlock), elseBlock)
    case AST.While(condition, body, _) => foldTree(foldTree(x, condition), body)
    case AST.For(varRef, range, body, _) => foldTree(foldTree(foldTree(x, varRef), range), body)
    case AST.Return(expr, _) => foldTree(x, expr)
    case AST.Continue(_) => x
    case AST.Break(_) => x
