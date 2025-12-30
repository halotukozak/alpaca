enum AST:
  case Block(is: List[Statement])
  case If(cond: Expr, thenStmt: Statement, elseStmt: Option[Statement])
  case Break
  case Continue
  case Return(value: Expr)
  case Assignment(target: Expr, value: Expr)
  // ...

object MatrixParser extends Parser:
  val root: Rule[AST.Tree] = rule { case Instruction.List(is) =>
    AST.Block(is)
  }

  def Instruction: Rule[AST.Statement] = rule(
    { case (Statement(s), ML.`;`(_)) => s },
    { case If(i) => i },
    { case Break(b) => b },
    { case Continue(c) => c },
    { case Return(r) => r },
    { case Assignment(a) => a },
  )

  def Statement: Rule[AST.Statement] = rule(
    { case ML.break(l) => AST.Break },
    { case ML.continue(l) => AST.Continue },
    { case (ML.`return`(l), Expr(e)) => AST.Return(e) },
    { case Assignment(a) => a },
  )

  def Block: Rule[AST.Block] = rule(
    { case (ML.`\\{`(_), Instruction.List(is), ML.`\\}`(_)) =>
      AST.Block(is)
    },
    { case Instruction(i) => AST.Block(i :: Nil) },
  )

  // ...
