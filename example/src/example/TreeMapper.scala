package example

import scala.collection.Factory
import scala.util.NotGiven

type TreeVisitor[Acc] = TreeTraverser[Acc, [_] =>> Unit]

type TreeAccumulator[Acc] = TreeTraverser[Acc, [_] =>> Result[Acc]]

transparent trait TreeMapper[Acc] extends TreeTraverser[Acc, [T <: AST.Tree | Null] =>> Result[(ast: T, acc: Acc)]]:
  extension [T <: AST.Tree: Mapper, CC[X] <: Iterable[X]](trees: CC[T])(using factory: Factory[T, CC[T]])
    def visitAll(acc: Acc): Result[(CC[T], Acc)] =
      trees.iterator
        .foldLeft(Result.Success((Seq.empty[T], acc)): Result[(Seq[T], Acc)]): (acc, res) =>
          for
            (accValues, accEnv) <- acc
            (value, newEnv) <- res.visit(accEnv)
          yield (accValues ++ Option(value), newEnv)
        .map((seq, env) => (seq.to(factory), env))

transparent trait TreeTraverser[Acc, R[+_ <: AST.Tree | Null]]:
  final type Mapper[T <: AST.Tree] = Acc => T => ? <: R[T]

  extension [T <: AST.Tree](tree: T)(using mapper: Mapper[T] = given_Mapper_Tree)
    final def visit(acc: Acc): R[T] = mapper(acc)(tree)

  extension [T <: AST.Tree](tree: T | Null)(using mapper: Mapper[T] = given_Mapper_Tree)
    final def visitNullable(acc: Acc): R[T | Null] = tree match
      case null => handleNull(acc)
      case tree => mapper(acc)(tree)

  val handleNull: Acc => R[Null]

  given Mapper[AST.Literal] = compiletime.deferred

  given Mapper[AST.SymbolRef] = compiletime.deferred

  given Mapper[AST.VectorRef] = compiletime.deferred

  given Mapper[AST.MatrixRef] = compiletime.deferred

  given Mapper[AST.Apply] = compiletime.deferred

  given Mapper[AST.Range] = compiletime.deferred

  given Mapper[AST.Assign] = compiletime.deferred

  given Mapper[AST.If] = compiletime.deferred

  given Mapper[AST.While] = compiletime.deferred

  given Mapper[AST.For] = compiletime.deferred

  given Mapper[AST.Return] = compiletime.deferred

  given Mapper[AST.Continue] = compiletime.deferred

  given Mapper[AST.Break] = compiletime.deferred

  given Mapper[AST.Block] = compiletime.deferred

  given Mapper[AST.Ref] = acc =>
    case s: AST.SymbolRef => s.visit(acc)
    case v: AST.VectorRef => v.visit(acc)
    case m: AST.MatrixRef => m.visit(acc)

  given Mapper[AST.Expr] = acc =>
    case l: AST.Literal => l.visit(acc)
    case r: AST.Ref => r.visit(acc)
    case a: AST.Apply => a.visit(acc)
    case r: AST.Range => r.visit(acc)

  given Mapper[AST.Statement] = acc =>
    case e: AST.Expr => e.visit(acc)
    case a: AST.Assign => a.visit(acc)
    case i: AST.If => i.visit(acc)
    case w: AST.While => w.visit(acc)
    case f: AST.For => f.visit(acc)
    case r: AST.Return => r.visit(acc)
    case c: AST.Continue => c.visit(acc)
    case b: AST.Break => b.visit(acc)

  given Mapper[AST.Tree] = acc =>
    case tree: AST.Block => tree.visit(acc)
    case tree: AST.Statement => tree.visit(acc)
