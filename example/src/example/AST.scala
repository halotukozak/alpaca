package example

import alpaca.DebugSettings.default
import scala.annotation.unchecked.uncheckedVariance

// +- Tree -+- Statement -+
//          +- Block      |
//                        +- Expr --------+- Literal
//                        |               +- Apply
//                        |               +- Number
//                        |               +- String
//                        |               +- Range
//                        |               +- Ref --------+- VectorRef
//                        |                              +- MatrixRef
//                        |                              +- SymbolRef
//                        +- Assign
//                        +- If
//                        +- While
//                        +- For
//                        +- Return
//                        +- Continue
//                        +- Break

object AST {
  sealed trait Tree:
    def line: Int | Null
    def children: List[Tree] = Nil

  sealed trait Statement extends Tree

  case class Block(statements: List[Statement], line: Int | Null = null) extends Tree:
    override def children: List[Tree] = statements

  sealed trait Expr extends Statement:
    def tpe: Type
    def line: Int

  object Expr {
//    def traverse[T <: Type](expr: AST.Expr[Type.VarArg[T]]): Seq[AST.Expr[T]] =
//      throw NotImplementedError(expr.toString)

    def traverse(expr: AST.Expr): Seq[AST.Expr] =
      throw NotImplementedError(expr.toString)

    def unapply(expr: AST.Expr): Some[Type] = Some(expr.tpe)
  }

  case class Literal(tpe: Type, value: Type.ToScala[tpe.type], line: Int) extends Expr

  sealed trait Ref extends Expr

  case class SymbolRef(tpe: Type, name: String, line: Int) extends Ref

  case class VectorRef(vector: SymbolRef, element: Expr, line: Int) extends Ref:
    val tpe: Type.Numerical = Type.Numerical
    override def children: List[Tree] = List(vector, element)

  case class MatrixRef(matrix: SymbolRef, row: Expr | Null, col: Expr | Null, line: Int) extends Ref:
    val tpe: Type.Numerical = Type.Numerical
    override def children: List[Tree] = List(matrix, row, col).collect { case t: Tree => t }

  case class Apply(ref: SymbolRef, args: List[Expr], tpe: Type, line: Int) extends Expr:
    override def children: List[Tree] = ref :: args

  case class Range(start: Expr, end: Expr, line: Int) extends Expr:
    val tpe: Type.Range = Type.Range
    override def children: List[Tree] = List(start, end)

  case class Assign(ref: Ref, expr: Expr, line: Int) extends Statement:
    override def children: List[Tree] = List(ref, expr)

  case class If(condition: Expr, thenBlock: Block, elseBlock: Block | Null, line: Int) extends Statement:
    override def children: List[Tree] = List(condition, thenBlock) ++ Option(elseBlock)

  case class While(condition: Expr, body: Block, line: Int) extends Statement:
    override def children: List[Tree] = List(condition, body)

  case class For(varRef: SymbolRef, range: Range, body: Block, line: Int) extends Statement:
    override def children: List[Tree] = List(varRef, range, body)

  case class Return(expr: Expr, line: Int) extends Statement:
    override def children: List[Tree] = List(expr)

  case class Continue(line: Int) extends Statement

  case class Break(line: Int) extends Statement
}
