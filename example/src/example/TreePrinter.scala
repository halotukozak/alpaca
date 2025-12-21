package example

import example.AST.*

object TreePrinter extends TreeVisitor[Int]:
  extension (tree: AST.Tree) def printTree(indent: Int = 0): Unit = tree.visit(indent)

  override val handleNull = _ => ()

  override given Mapper[AST.Block] = indent =>
    block =>
      println("|  " * indent + "BLOCK")
      block.statements.foreach(_.printTree(indent + 1))

  override given Mapper[AST.Literal] = indent => literal => println("|  " * indent + literal.value.toString)

  override given Mapper[AST.SymbolRef] = indent => symbolRef => println("|  " * indent + symbolRef.name)

  override given Mapper[AST.VectorRef] = indent =>
    vectorRef =>
      println("|  " * indent + "VECTORREF")
      println("|  " * (indent + 1) + "ARGUMENTS")
      vectorRef.vector.printTree(indent + 2)
      vectorRef.element.printTree(indent + 2)

  override given Mapper[AST.MatrixRef] = indent =>
    matrixRef =>
      println("|  " * indent + "MATRIXREF")
      println("|  " * (indent + 1) + "ARGUMENTS")
      matrixRef.matrix.printTree(indent + 2)
      if matrixRef.row.ne(null) then matrixRef.row.printTree(indent + 2)
      if matrixRef.col.ne(null) then matrixRef.col.printTree(indent + 2)

  override given Mapper[AST.Apply] = indent =>
    apply =>
      println("|  " * indent + s"${apply.ref.name}")
      println("|  " * (indent + 1) + "ARGUMENTS")
      apply.args.foreach(_.printTree(indent + 2))

  override given Mapper[AST.Range] = indent =>
    range =>
      println("|  " * indent + "RANGE")
      range.start.printTree(indent + 1)
      range.end.printTree(indent + 1)

  override given Mapper[AST.Assign] = indent =>
    assign =>
      println("|  " * indent + "=")
      assign.ref.printTree(indent + 1)
      assign.expr.printTree(indent + 1)

  override given Mapper[AST.If] = indent =>
    ifStmt =>
      println("|  " * indent + "IF")
      ifStmt.condition.printTree(indent + 1)
      println("|  " * indent + "THEN")
      ifStmt.thenBlock.statements.foreach(_.printTree(indent + 1))
      if ifStmt.elseBlock.ne(null) then
        println("|  " * indent + "ELSE")
        ifStmt.elseBlock.statements.foreach(_.printTree(indent + 1))

  override given Mapper[AST.While] = indent =>
    whileStmt =>
      println("|  " * indent + "WHILE")
      println("|  " * (indent + 1) + "CONDITION")
      whileStmt.condition.printTree(indent + 2)
      println("|  " * (indent + 1) + "BODY")
      whileStmt.body.statements.foreach(_.printTree(indent + 2))

  override given Mapper[AST.For] = indent =>
    forStmt =>
      println("|  " * indent + "FOR")
      println("|  " * (indent + 1) + forStmt.varRef.name)
      println("|  " * (indent + 1) + "RANGE")
      forStmt.range.start.printTree(indent + 2)
      forStmt.range.end.printTree(indent + 2)
      println("|  " * (indent + 1) + "BODY")
      forStmt.body.statements.foreach(_.printTree(indent + 2))

  override given Mapper[AST.Return] = indent =>
    returnStmt =>
      println("|  " * indent + "RETURN")
      returnStmt.expr.printTree(indent + 1)

  override given Mapper[AST.Continue] = indent => _ => println("|  " * indent + "CONTINUE")

  override given Mapper[AST.Break] = indent => _ => println("|  " * indent + "BREAK")
