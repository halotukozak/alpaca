package example

import MatrixLexer as ML

import alpaca.*

object MatrixParser extends Parser {
  def root = rule { case Instructions.Option(is) => is }

  def Instructions: Rule[List[AST.Statement]] = rule(
    { case Instruction(i) => i :: Nil },
    { case (Instruction(i), Instructions(is)) => i :: is },
  )

  def Instruction: Rule[AST.Statement] = rule(
    { case (Statement(s), ML.`;`(_)) => s },
    { case (For(_), ML.ID(id), ML.`=`(_), Range(r), Block(b)) =>
      //  var = AST.SymbolRef(p.ID, p.lineno, TS.Int())
// // #         return AST.For(var, p.range, p.block, p.lineno)
      ???
    },
  )
  // { case (Statement(s), ML.`\\n`(_)) =>
  //   throw new Exception("Missing semicolon")
  // },

  def Block: Rule[AST.Block] = rule(
    { case (ML.`\\{`(_), Instructions(is), ML.`\\}`(_)) => AST.Block(is, is.headOption.map(_.line).orNull) },
    { case Instruction(i) => AST.Block(i :: Nil, i.line) },
  )

  def If: Rule[AST.If] = rule(
    { case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock)) =>
      AST.If(cond, thenBlock, null, l.line)
    },
    { case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock), ML.`else`(_), Block(elseBlock)) =>
      AST.If(cond, thenBlock, elseBlock, l.line)
    },
  )

  def While: Rule[AST.While] = rule { case (ML.`while`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(body)) =>
    AST.While(cond, body, l.line)
  }

  def For: Rule[AST.For] = rule { case (ML.`for`(l), ML.ID(id), ML.`=`(_), Range(r), Block(body)) =>
    val varRef = AST.SymbolRef(id.value, Type.Int, id.line)
    AST.For(varRef, r, body, l.line)
  }

  def Range: Rule[AST.Range] = rule {
    case (Expr(start: AST.Expr[Type.Int]), ML.`:`(_), Expr(end: AST.Expr[Type.Int])) =>
      AST.Range(start, end, start.line)
  }

  def Comparator: Rule[String] = rule(
    { case ML.`>`(_) => ">" },
    { case ML.`<`(_) => "<" },
    { case ML.EQUAL(_) => "==" },
    { case ML.NOT_EQUAL(_) => "!=" },
    { case ML.LESS_EQUAL(_) => "<=" },
    { case ML.GREATER_EQUAL(_) => ">=" },
  )

  def Condition: Rule[AST.Expr[Type.Bool]] = rule { case (Expr(e1), Comparator(op), Expr(e2)) =>
    // args = [p.expr0, p.expr1]
    // return AST.Apply(Predef.get_symbol(p.comparator), args, p.lineno)
    ???
  }

  def AsssignOp: Rule[String] = rule(
    { case ML.ADDASSIGN(_) => "+=" },
    { case ML.SUBASSIGN(_) => "-=" },
    { case ML.MULASSIGN(_) => "*=" },
    { case ML.DIVASSIGN(_) => "/=" },
    { case ML.`=`(_) => "=" },
  )

  def Statement: Rule[AST.Statement] = rule(
    { case ML.break(l) => AST.Break(l.line) },
    { case ML.continue(l) => AST.Continue(l.line) },
    { case (ML.`return`(l), Expr(e)) => AST.Return(e, l.line) },
    { case (ML.print(l), Varargs(args)) =>
      // AST.Apply(Predef.get_symbol("PRINT"), args, l.line)
      ???
    },
    { case (ML.ID(id), AsssignOp(op), Expr(e)) =>
      // // #     @_('ID assign_op expr')
// // #     def statement(self, p: YaccProduction):
// // #         var = AST.SymbolRef(p.ID, p.lineno, TS.undef())
// // #         match p.assign_op:
// // #             case "=":
// // #                 expr = p.expr
// // #             case _:
// // #                 expr = AST.Apply(Predef.get_symbol(p.assign_op[:-1]), [var, p.expr], p.lineno)
// // #         var.type = expr.type
// // #         return AST.Assign(var, expr, p.lineno)
      ???

    },
    { case (Element(el), AsssignOp(op), Expr(e)) =>
      // match p.assign_op:
// // #             case "=":
// // #                 expr = p.expr
// // #             case _:
// // #                 args = [p.element, p.expr]
// // #                 expr = AST.Apply(Predef.get_symbol(p.assign_op[:-1]), args, p.lineno)
// // #         return AST.Assign(p.element, expr, p.lineno)
      ???
    },
  )

  def FunctionName = rule(
    { case ML.eye(l) => "EYE" },
    { case ML.zeros(l) => "ZEROS" },
    { case ML.ones(l) => "ONES" },
  )

  def Matrix = rule { case (ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    // return AST.Apply(Predef.get_symbol("INIT"), p.var_args, p.lineno)
    ???
  }

  def Element = rule { case (Var(v), ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    varArgs.length match
      case 1 =>
        AST.VectorRef(v, varArgs.head.asInstanceOf[AST.Expr[Type.Int]], v.line)
      case 2 =>
        AST.MatrixRef(
          v,
          varArgs.head.asInstanceOf[AST.Expr[Type.Int]],
          varArgs(1).asInstanceOf[AST.Expr[Type.Int]],
          v.line,
        )
      case _ =>
        // report_error(self, "Invalid matrix element reference", p.lineno)
        AST.MatrixRef(v, null, null, v.line)
  }

  def Var = rule { case ML.ID(id) =>
    AST.SymbolRef(id.value, Type.Undef, id.line)
  }

  def Expr: Rule[AST.Expr[?]] = rule(
    { case ML.INTNUM(l) => AST.Literal.int(l.value, l.line) },
    { case ML.FLOAT(l) => AST.Literal.float(l.value, l.line) },
    { case ML.STRING(l) => AST.Literal.string(l.value, l.line) },
    { case (ML.`-`(l), Expr(e)) =>
      // return AST.Apply(Predef.get_symbol("UMINUS"), [p.expr], p.lineno)
      ???
    }: @name("uminus"),
    { case (Expr(e1), ML.`\\+`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("+"), [p.expr0, p.expr1], p.lineno
      ???
    }: @name("plus"),
    { case (Expr(e1), ML.`-`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("-"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`\\*`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("*"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`/`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("/"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`DOTADD`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("DOTADD"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`DOTSUB`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("DOTSUB"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`DOTMUL`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("DOTMUL"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e1), ML.`DOTDIV`(_), Expr(e2)) =>
      // return AST.Apply(Predef.get_symbol("DOTDIV"), [p.expr0, p.expr1], p.lineno
      ???
    },
    { case (Expr(e), ML.`'`(_)) =>
      // return AST.Apply(Predef.get_symbol("'"), [p.expr], p.lineno
      ???
    },
    { case (ML.`\\(`(_), Expr(e), ML.`\\)`(_)) => e },
    { case Element(el) => el },
    { case Var(v) => v },
    { case Matrix(m) => m },
    { case (FunctionName(name), ML.`\\(`(_), Varargs(args), ML.`\\)`(_)) =>
      // return AST.Apply(Predef.get_symbol(name), p.var_args, p.lineno
      ???
    },
  )

  def Varargs: Rule[List[AST.Expr[?]]] = rule(
    { case Expr(l) => l :: Nil },
    { case (Varargs(vs), ML.`,`(_), Expr(e)) => vs :+ e },
  )

// // #     def error(self, p: YaccProduction):
// // #         if p:
// // #             report_error(self, f"Syntax error: {p.type}('{p.value}')", p.lineno)
// // #         else:
// // #             report_error(self, "Syntax error", -1)

  import Production as P
  override val resolutions: Set[ConflictResolution] = Set(
    P.ofName("uminus").before(ML.`\\+`),
    P.ofName("uminus").before(ML.DOTADD),
    P.ofName("uminus").before(ML.DOTSUB),
  )
}

// // #     precedence = (
// // #         ('nonassoc', 'IFX'),
// // #         ('nonassoc', 'ELSE'),
// // #         ('nonassoc', '<', '>', 'LESS_EQUAL', 'GREATER_EQUAL', 'NOT_EQUAL', 'EQUAL'),
// // #         ('left', '+', '-'),
// // #         ('left', 'DOTADD', 'DOTSUB'),
// // #         ('left', '*', '/'),
// // #         ('left', 'DOTMUL', 'DOTDIV'),
// // #         ('right', 'UMINUS'),
// // #         ('left', "'"),
// // #     )
