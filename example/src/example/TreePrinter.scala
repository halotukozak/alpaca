package example

object TreePrinter extends TreeVisitor[Int]:
  override val handleNull: Int => Unit = _ => ()

  override given Process[AST.Block] = indent =>
    block =>
      println("|  " * indent + "BLOCK")
      block.statements.foreach(_.visit(indent + 1))

  override given Process[AST.Literal] = indent => literal => println("|  " * indent + literal.value.toString)

  override given Process[AST.SymbolRef] = indent => symbolRef => println("|  " * indent + symbolRef.name)

  override given Process[AST.VectorRef] = indent =>
    vectorRef =>
      println("|  " * indent + "VECTORREF")
      println("|  " * (indent + 1) + "ARGUMENTS")
      vectorRef.vector.visit(indent + 2)
      vectorRef.element.visit(indent + 2)

  override given Process[AST.MatrixRef] = indent =>
    matrixRef =>
      println("|  " * indent + "MATRIXREF")
      println("|  " * (indent + 1) + "ARGUMENTS")
      matrixRef.matrix.visit(indent + 2)
      if matrixRef.row.ne(null) then matrixRef.row.visit(indent + 2)
      if matrixRef.col.ne(null) then matrixRef.col.visit(indent + 2)

  override given Process[AST.Apply] = indent =>
    apply =>
      println("|  " * indent + s"${apply.ref.name}")
      println("|  " * (indent + 1) + "ARGUMENTS")
      apply.args.foreach(_.visit(indent + 2))

  override given Process[AST.Range] = indent =>
    range =>
      println("|  " * indent + "RANGE")
      range.start.visit(indent + 1)
      range.end.visit(indent + 1)

  override given Process[AST.Assign] = indent =>
    assign =>
      println("|  " * indent + "=")
      assign.ref.visit(indent + 1)
      assign.expr.visit(indent + 1)

  override given Process[AST.If] = indent =>
    ifStmt =>
      println("|  " * indent + "IF")
      ifStmt.condition.visit(indent + 1)
      println("|  " * indent + "THEN")
      ifStmt.thenBlock.statements.foreach(_.visit(indent + 1))
      if ifStmt.elseBlock.ne(null) then
        println("|  " * indent + "ELSE")
        ifStmt.elseBlock.statements.foreach(_.visit(indent + 1))

  override given Process[AST.While] = indent =>
    whileStmt =>
      println("|  " * indent + "WHILE")
      println("|  " * (indent + 1) + "CONDITION")
      whileStmt.condition.visit(indent + 2)
      println("|  " * (indent + 1) + "BODY")
      whileStmt.body.statements.foreach(_.visit(indent + 2))

  override given Process[AST.For] = indent =>
    forStmt =>
      println("|  " * indent + "FOR")
      println("|  " * (indent + 1) + forStmt.varRef.name)
      println("|  " * (indent + 1) + "RANGE")
      forStmt.range.start.visit(indent + 2)
      forStmt.range.end.visit(indent + 2)
      println("|  " * (indent + 1) + "BODY")
      forStmt.body.statements.foreach(_.visit(indent + 2))

  override given Process[AST.Return] = indent =>
    returnStmt =>
      println("|  " * indent + "RETURN")
      returnStmt.expr.visit(indent + 1)

  override given Process[AST.Continue] = indent => _ => println("|  " * indent + "CONTINUE")

  override given Process[AST.Break] = indent => _ => println("|  " * indent + "BREAK")
