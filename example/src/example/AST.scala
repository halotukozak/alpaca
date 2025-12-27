package example

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

object AST:
  sealed trait Tree:
    def line: Int
    def children: List[Tree] = Nil

  sealed trait Statement extends Tree

  case class Block(statements: List[Statement], line: Int) extends Tree:
    override def children: List[Tree] = statements

  sealed trait Expr extends Statement:
    val tpe: Type
    val line: Int

  object Expr:
    def unapply(expr: AST.Expr): Some[expr.tpe.type] = Some(expr.tpe)

  case class Literal(tpe: Type, value: Type.ToScala[tpe.type], line: Int) extends Expr

  sealed trait Ref extends Expr

  case class SymbolRef(tpe: Type, name: String, line: Int) extends Ref

  case class VectorRef(vector: SymbolRef, element: Expr, line: Int) extends Ref:
    val tpe: Type.Numerical = Type.Numerical

  case class MatrixRef(matrix: SymbolRef, row: Expr | Null, col: Expr | Null, line: Int) extends Ref:
    val tpe: Type.Numerical = Type.Numerical

  case class Apply(ref: SymbolRef, args: List[Expr], tpe: Type, line: Int) extends Expr

  case class Range(start: Expr, end: Expr, line: Int) extends Expr:
    val tpe: Type.Range = Type.Range

  case class Assign(ref: Ref, expr: Expr, line: Int) extends Statement

  case class If(condition: Expr, thenBlock: Block, elseBlock: Block | Null, line: Int) extends Statement

  case class While(condition: Expr, body: Block, line: Int) extends Statement

  case class For(varRef: SymbolRef, range: Range, body: Block, line: Int) extends Statement

  case class Return(expr: Expr, line: Int) extends Statement

  case class Continue(line: Int) extends Statement

  case class Break(line: Int) extends Statement

extension (func: runtime.Function) private def toRef = AST.SymbolRef(func.tpe, func.productPrefix, -1)
