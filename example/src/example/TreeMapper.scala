package example

import scala.collection.Factory

type TreeVisitor[Acc] = TreeTraverser[Acc, [_] =>> Unit]

type TreeAccumulator[Acc] = TreeTraverser[Acc, [_] =>> Result[Acc]]

transparent trait TreeMapper[Acc] extends TreeTraverser[Acc, [T <: AST.Tree | Null] =>> Result[(ast: T, acc: Acc)]]:
  extension [T <: AST.Tree: Process, CC[X] <: Iterable[X]](trees: CC[T])(using factory: Factory[T, CC[T]])
    def visitAll(acc: Acc): Result[(CC[T], Acc)] =
      trees.iterator
        .foldLeft(Result.Success((Seq.empty[T], acc)): Result[(Seq[T], Acc)]): (acc, res) =>
          for
            (accValues, accEnv) <- acc
            (value, newEnv) <- res.visit(accEnv)
          yield (accValues ++ Option(value), newEnv)
        .map((seq, env) => (seq.to(factory), env))

transparent trait TreeTraverser[Acc, R[+_ <: AST.Tree | Null]]:
  final type Process[T <: AST.Tree] = Acc => T => ? <: R[T]

  extension [T <: AST.Tree](tree: T)(using mapper: Process[T] = given_Process_Tree)
    def visit(acc: Acc): R[T] = mapper(acc)(tree)

  extension [T <: AST.Tree](tree: T | Null)(using mapper: Process[T] = given_Process_Tree)
    def visitNullable(acc: Acc): R[T | Null] = tree match
      case null => handleNull(acc)
      case tree => mapper(acc)(tree)

  val handleNull: Acc => R[Null]

  given Process[AST.Literal] = compiletime.deferred

  given Process[AST.SymbolRef] = compiletime.deferred

  given Process[AST.VectorRef] = compiletime.deferred

  given Process[AST.MatrixRef] = compiletime.deferred

  given Process[AST.Apply] = compiletime.deferred

  given Process[AST.Range] = compiletime.deferred

  given Process[AST.Assign] = compiletime.deferred

  given Process[AST.If] = compiletime.deferred

  given Process[AST.While] = compiletime.deferred

  given Process[AST.For] = compiletime.deferred

  given Process[AST.Return] = compiletime.deferred

  given Process[AST.Continue] = compiletime.deferred

  given Process[AST.Break] = compiletime.deferred

  given Process[AST.Block] = compiletime.deferred

  given Process[AST.Ref] = acc =>
    case s: AST.SymbolRef => s.visit(acc)
    case v: AST.VectorRef => v.visit(acc)
    case m: AST.MatrixRef => m.visit(acc)

  given Process[AST.Expr] = acc =>
    case l: AST.Literal => l.visit(acc)
    case r: AST.Ref => r.visit(acc)
    case a: AST.Apply => a.visit(acc)
    case r: AST.Range => r.visit(acc)

  given Process[AST.Statement] = acc =>
    case e: AST.Expr => e.visit(acc)
    case a: AST.Assign => a.visit(acc)
    case i: AST.If => i.visit(acc)
    case w: AST.While => w.visit(acc)
    case f: AST.For => f.visit(acc)
    case r: AST.Return => r.visit(acc)
    case c: AST.Continue => c.visit(acc)
    case b: AST.Break => b.visit(acc)

  given Process[AST.Tree] = acc =>
    case tree: AST.Block => tree.visit(acc)
    case tree: AST.Statement => tree.visit(acc)
