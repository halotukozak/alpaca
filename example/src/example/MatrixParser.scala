package example

import MatrixLexer as ML

import alpaca.{Production as P, *}
import java.util.jar.Attributes.Name

object MatrixParser extends Parser {
  extension (sth: Any) def line = -1

  val root: Rule[AST.Tree] = rule { case Instructions.Option(is) =>
    AST.Block(is.toList.flatten, is.flatMap(_.headOption.map(_.line)).orNull)
  }

  def Instructions: Rule[List[AST.Statement]] = rule(
    { case Instruction(i) => i :: Nil },
    { case (Instruction(i), Instructions(is)) => i :: is },
  )

  def Instruction: Rule[AST.Statement] = rule(
    { case (Statement(s), ML.`;`(_)) => s },
    // { case (Statement(s), ML.`\\n+`(_)) => throw new Exception("Missing semicolon") },
    { case If(i) => i },
    { case While(w) => w },
    { case For(f) => f },
  )

  def Statement: Rule[AST.Statement] = rule(
    { case ML.break(l) => AST.Break(l.line) },
    { case ML.continue(l) => AST.Continue(l.line) },
    { case (ML.`return`(l), Expr(e)) => AST.Return(e, l.line) },
    { case (ML.print(l), Varargs(args)) => AST.Apply(symbols("PRINT"), args, Type.Undef, l.line) },
    { case Assignment(a) => a },
  )

  def Block: Rule[AST.Block] = rule(
    { case (ML.`\\{`(_), Instructions(is), ML.`\\}`(_)) => AST.Block(is, is.headOption.map(_.line).orNull) },
    { case Instruction(i) => AST.Block(i :: Nil, i.line) },
  )

  def If: Rule[AST.If] = rule(
    { case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock), ML.`else`(_), Block(elseBlock)) =>
      AST.If(cond, thenBlock, elseBlock, l.line)
    }: @name("if-else"),
    { case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock)) =>
      AST.If(cond, thenBlock, null, l.line)
    }: @name("if"),
  )

  def While: Rule[AST.While] = rule { case (ML.`while`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(body)) =>
    AST.While(cond, body, l.line)
  }

  def For: Rule[AST.For] = rule { case (ML.`for`(l), ML.ID(id), ML.`=`(_), Range(r), Block(body)) =>
    // val varRef = AST.SymbolRef(id.value, Type.Int, id.line)
    // AST.For(varRef, r, body, l.line)
    AST.For(AST.SymbolRef(Type.Int, id.value, id.line), r, body, l.line)
  }

  def Range: Rule[AST.Range] = rule { case (Expr(start), ML.`:`(_), Expr(end)) =>
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

  def Condition: Rule[AST.Expr] = rule { case (Expr(e1), Comparator(op), Expr(e2)) =>
    AST.Apply(
      symbols(op).asInstanceOf[AST.SymbolRef],
      scala.List(e1, e2),
      Type.Undef,
      e1.line,
    )
  }

  def AsssignOp: Rule[(AST.Expr, AST.Expr) => AST.Expr] = rule(
    { case ML.ADDASSIGN(_) => (e1, e2) => AST.Apply(symbols("+"), scala.List(e1, e2), Type.Undef, e1.line) },
    { case ML.SUBASSIGN(_) => (e1, e2) => AST.Apply(symbols("-"), scala.List(e1, e2), Type.Undef, e1.line) },
    { case ML.MULASSIGN(_) => (e1, e2) => AST.Apply(symbols("*"), scala.List(e1, e2), Type.Undef, e1.line) },
    { case ML.DIVASSIGN(_) => (e1, e2) => AST.Apply(symbols("/"), scala.List(e1, e2), Type.Undef, e1.line) },
    { case ML.`=`(_) => (e1, e2) => e2 },
  )

  def Assignment: Rule[AST.Assign] = rule(
    { case (Var(v), AsssignOp(op), Expr(e)) => AST.Assign(v, op(v, e), v.line) },
    { case (Element(el), AsssignOp(op), Expr(e)) => AST.Assign(el, op(el, e), el.line) },
  )

  def FunctionName = rule(
    { case ML.eye(l) => "EYE" },
    { case ML.zeros(l) => "ZEROS" },
    { case ML.ones(l) => "ONES" },
  )

  def Matrix = rule { case (ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    AST.Apply(symbols("INIT"), varArgs, Type.Undef, varArgs.headOption.map(_.line).getOrElse(-1))
  }

  def Element = rule { case (Var(v), ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    varArgs.length match
      case 1 =>
        AST.VectorRef(v, varArgs.head.asInstanceOf[AST.Expr], v.line)
      case 2 =>
        AST.MatrixRef(v, varArgs.head, varArgs(1), v.line)
      case _ =>
        // report_error(self, "Invalid matrix element reference", p.lineno)
        AST.MatrixRef(v, null, null, v.line)
  }

  def Var = rule { case ML.ID(id) => AST.SymbolRef(Type.Undef, id.value, id.line) }

  def Expr: Rule[AST.Expr] = rule(
    { case ML.INTNUM(l) => AST.Literal(Type.Int, l.value, l.line) },
    { case ML.FLOAT(l) => AST.Literal(Type.Float, l.value, l.line) },
    { case ML.STRING(l) => AST.Literal(Type.String, l.value, l.line) },
    { case (ML.`-`(l), Expr(e)) =>
      AST.Apply(symbols("UMINUS"), scala.List(e), Type.Undef, l.line)
    }: @name("uminus"),
    { case (Expr(e1), ML.`\\+`(_), Expr(e2)) =>
      AST.Apply(symbols("+"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("add"),
    { case (Expr(e1), ML.`-`(_), Expr(e2)) =>
      AST.Apply(symbols("-"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("sub"),
    { case (Expr(e1), ML.`\\*`(_), Expr(e2)) =>
      AST.Apply(symbols("*"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("mul"),
    { case (Expr(e1), ML.`/`(_), Expr(e2)) =>
      AST.Apply(symbols("/"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("div"),
    { case (Expr(e1), ML.`DOTADD`(_), Expr(e2)) =>
      AST.Apply(symbols("DOTADD"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("dotadd"),
    { case (Expr(e1), ML.`DOTSUB`(_), Expr(e2)) =>
      AST.Apply(symbols("DOTSUB"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("dotsub"),
    { case (Expr(e1), ML.`DOTMUL`(_), Expr(e2)) =>
      AST.Apply(symbols("DOTMUL"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("dotmul"),
    { case (Expr(e1), ML.`DOTDIV`(_), Expr(e2)) =>
      AST.Apply(symbols("DOTDIV"), scala.List(e1, e2), Type.Undef, e1.line)
    }: @name("dotdiv"),
    { case (Expr(e), ML.`'`(_)) => AST.Apply(symbols("TRANSPOSE"), scala.List(e), Type.Undef, e.line) },
    { case (ML.`\\(`(_), Expr(e), ML.`\\)`(_)) => e },
    { case Element(el) => el },
    { case Var(v) => v },
    { case Matrix(m) => m },
    { case (FunctionName(name), ML.`\\(`(_), Varargs(args), ML.`\\)`(_)) =>
      AST.Apply(symbols(name), args, Type.Undef, args.headOption.map(_.line).getOrElse(-1))
    },
  )

  def Varargs: Rule[List[AST.Expr]] = rule(
    { case Expr(l) => l :: Nil },
    { case (Varargs(vs), ML.`,`(_), Expr(e)) => vs :+ e },
  )

  override val resolutions: Set[ConflictResolution] = Set(
    ML.`'`.before(P.ofName("uminus")),
    P.ofName("uminus").before(P.ofName("dotmul"), P.ofName("dotdiv")),
    P.ofName("dotmul").before(ML.DOTMUL, ML.DOTDIV),
    ML.DOTMUL.before(P.ofName("mul"), P.ofName("div")),
    P.ofName("dotdiv").before(ML.DOTMUL, ML.DOTDIV),
    ML.DOTDIV.before(P.ofName("mul"), P.ofName("div")),
    P.ofName("mul").before(ML.`\\*`, ML.`/`),
    ML.`\\*`.before(P.ofName("dotadd"), P.ofName("dotsub")),
    P.ofName("div").before(ML.`\\*`, ML.`/`),
    ML.`/`.before(P.ofName("dotadd"), P.ofName("dotsub")),
    P.ofName("dotadd").before(ML.DOTADD, ML.DOTSUB),
    ML.DOTADD.before(P.ofName("add"), P.ofName("sub")),
    P.ofName("dotsub").before(ML.DOTADD, ML.DOTSUB),
    ML.DOTSUB.before(P.ofName("add"), P.ofName("sub")),
    P.ofName("add").before(ML.`\\+`, ML.`-`),
    P.ofName("sub").before(ML.`\\+`, ML.`-`),
    P.ofName("if-else").before(ML.`else`),
    ML.`else`.before(P.ofName("if")),
  )

// // #     def error(self, p: YaccProduction):
// // #         if p:
// // #             report_error(self, f"Syntax error: {p.type}('{p.value}')", p.lineno)
// // #         else:
// // #             report_error(self, "Syntax error", -1)
}
