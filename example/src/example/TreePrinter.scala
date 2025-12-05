package example

import example.AST.*

trait TreePrinter[T <: AST.Tree]:
  def apply(ident: Int, tree: T): Unit

object TreePrinter:
  extension [T <: AST.Tree: TreePrinter as printer](tree: T)
    def printTree(indent: Int = 0): Unit =
      printer(indent, tree)

  given TreePrinter[AST.Tree] = (indent, tree) =>
    tree match
      case b: AST.Block => b.printTree(indent)
      case s: AST.Statement => s.printTree(indent)

  given TreePrinter[AST.Statement] = (indent, stmt) =>
    stmt match
      case e: AST.Expr => e.printTree(indent)
      case a: AST.Assign => a.printTree(indent)
      case i: AST.If => i.printTree(indent)
      case w: AST.While => w.printTree(indent)
      case f: AST.For => f.printTree(indent)
      case r: AST.Return => r.printTree(indent)
      case c: AST.Continue => c.printTree(indent)
      case b: AST.Break => b.printTree(indent)

  given TreePrinter[AST.Expr] = (indent, expr) =>
    expr match
      case l: AST.Literal => l.printTree(indent)
      case r: AST.Ref => r.printTree(indent)
      case a: AST.Apply => a.printTree(indent)
      case r: AST.Range => r.printTree(indent)

  given TreePrinter[AST.Ref] = (indent, ref) =>
    ref match
      case s: AST.SymbolRef => s.printTree(indent)
      case v: AST.VectorRef => v.printTree(indent)
      case m: AST.MatrixRef => m.printTree(indent)

  given TreePrinter[AST.Block] = (indent, block) =>
    println("|  " * indent + "BLOCK")
    block.statements.foreach(_.printTree(indent + 1))

  given TreePrinter[AST.Literal] = (indent, literal) => println("|  " * indent + literal.value.toString)

  given TreePrinter[AST.SymbolRef] = (indent, symRef) => println("|  " * indent + symRef.name)

  given TreePrinter[AST.VectorRef] = (indent, vectorRef) =>
    println("|  " * indent + "VECTORREF")
    println("|  " * (indent + 1) + "ARGUMENTS")
    vectorRef.vector.printTree(indent + 2)
    vectorRef.element.printTree(indent + 2)

  given TreePrinter[AST.MatrixRef] = (indent, matrixRef) =>
    println("|  " * indent + "MATRIXREF")
    println("|  " * (indent + 1) + "ARGUMENTS")
    matrixRef.matrix.printTree(indent + 2)
    if matrixRef.row.ne(null) then matrixRef.row.printTree(indent + 2)
    if matrixRef.col.ne(null) then matrixRef.col.printTree(indent + 2)

  given TreePrinter[AST.Apply] = (indent, apply) =>
    println("|  " * indent + s"${apply.ref.name}")
    println("|  " * (indent + 1) + "ARGUMENTS")
    apply.args.foreach(_.printTree(indent + 2))

  given TreePrinter[AST.Range] = (indent, range) =>
    println("|  " * indent + "RANGE")
    range.start.printTree(indent + 1)
    range.end.printTree(indent + 1)

  given TreePrinter[AST.Assign] = (indent, assign) =>
    println("|  " * indent + "=")
    assign.ref.printTree(indent + 1)
    assign.expr.printTree(indent + 1)

  given TreePrinter[AST.If] = (indent, ifStmt) =>
    println("|  " * indent + "IF")
    ifStmt.condition.printTree(indent + 1)
    println("|  " * indent + "THEN")
    ifStmt.thenBlock.statements.foreach(_.printTree(indent + 1))
    if ifStmt.elseBlock.ne(null) then
      println("|  " * indent + "ELSE")
      ifStmt.elseBlock.statements.foreach(_.printTree(indent + 1))

  given TreePrinter[AST.While] = (indent, whileStmt) =>
    println("|  " * indent + "WHILE")
    println("|  " * (indent + 1) + "CONDITION")
    whileStmt.condition.printTree(indent + 2)
    println("|  " * (indent + 1) + "BODY")
    whileStmt.body.statements.foreach(_.printTree(indent + 2))

  given TreePrinter[AST.For] = (indent, forStmt) =>
    println("|  " * indent + "FOR")
    println("|  " * (indent + 1) + forStmt.varRef.name)
    println("|  " * (indent + 1) + "RANGE")
    forStmt.range.start.printTree(indent + 2)
    forStmt.range.end.printTree(indent + 2)
    println("|  " * (indent + 1) + "BODY")
    forStmt.body.statements.foreach(_.printTree(indent + 2))

  given TreePrinter[AST.Return] = (indent, returnStmt) =>
    println("|  " * indent + "RETURN")
    returnStmt.expr.printTree(indent + 1)

  given TreePrinter[AST.Continue] = (indent, _) => println("|  " * indent + "CONTINUE")

  given TreePrinter[AST.Break] = (indent, _) => println("|  " * indent + "BREAK")
