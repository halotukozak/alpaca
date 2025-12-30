package example

import MatrixLexer as ML

import alpaca.{Production as P, *}

import java.util.jar.Attributes.Name
import scala.util.chaining.scalaUtilChainingOps

object MatrixParser extends Parser:
  val root: Rule[AST.Tree] = rule { case Instruction.List(is) =>
    AST.Block(is, is.head.line)
  }

  def Instruction: Rule[AST.Statement] = rule(
    { case (Statement(s), ML.`;`(_)) => s },
    // { case (Statement(s), ML.`\\n+`(_)) => throw new Exception("Missing semicolon") }, //todo: ignored tokens
    { case If(i) => i },
    { case While(w) => w },
    { case For(f) => f },
  )

  def Statement: Rule[AST.Statement] = rule(
    { case ML.break(l) => AST.Break(l.line) },
    { case ML.continue(l) => AST.Continue(l.line) },
    { case (ML.`return`(l), Expr(e)) => AST.Return(e, l.line) },
    { case (ML.print(l), Varargs(args)) => AST.Apply(runtime.Function.PRINT.toRef, args, Type.Undef, l.line) },
    { case Assignment(a) => a },
  )

  def Block: Rule[AST.Block] = rule(
    { case (ML.`\\{`(_), Instruction.List(is), ML.`\\}`(_)) =>
      AST.Block(is, is.headOption.map(_.line).orNull.asInstanceOf[Int])
    },
    { case Instruction(i) => AST.Block(i :: Nil, i.line) },
  )

  def If: Rule[AST.If] = rule(
    "if-else" {
      case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock), ML.`else`(_), Block(elseBlock)) =>
        AST.If(cond, thenBlock, elseBlock, l.line)
    },
    "if" { case (ML.`if`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(thenBlock)) =>
      AST.If(cond, thenBlock, null, l.line)
    },
  )

  def While: Rule[AST.While] = rule { case (ML.`while`(l), ML.`\\(`(_), Condition(cond), ML.`\\)`(_), Block(body)) =>
    AST.While(cond, body, l.line)
  }

  def For: Rule[AST.For] = rule { case (ML.`for`(l), ML.Id(id), ML.`=`(_), Range(r), Block(body)) =>
    AST.For(AST.SymbolRef(Type.Int, id.value, id.line), r, body, l.line)
  }

  def Range: Rule[AST.Range] = rule { case (Expr(start), ML.`:`(_), Expr(end)) =>
    AST.Range(start, end, start.line)
  }

  def Comparator = rule(
    { case ML.LongComparator(op) => op },
    { case ML.ShortComparator(op) => op },
  )

  def Condition: Rule[AST.Expr] = rule { case (Expr(e1), Comparator(op), Expr(e2)) =>
    AST.Apply(runtime.Function.valueOf(op.value).toRef, List(e1, e2), Type.Undef, e1.line)
  }

  def AssignTarget: Rule[AST.Ref] = rule(
    { case Var(v) => v },
    { case Element(el) => el },
  )

  def Assignment: Rule[AST.Assign] = rule(
    { case (AssignTarget(target), ML.AssignOp(op), Expr(e)) =>
      AST.Assign(
        target,
        AST.Apply(runtime.Function.valueOf(op.value).toRef, List(target, e), Type.Undef, target.line),
        target.line,
      )
    },
    { case (AssignTarget(target), ML.`=`(_), Expr(e)) => AST.Assign(target, e, target.line) },
  )

  def Matrix: Rule[AST.Apply] = rule { case (ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    AST.Apply(runtime.Function.INIT.toRef, varArgs, Type.Undef, varArgs.headOption.map(_.line).getOrElse(-1))
  }

  def Element: Rule[AST.Ref] = rule { case (Var(v), ML.`\\[`(_), Varargs(varArgs), ML.`\\]`(_)) =>
    varArgs.length match
      case 1 =>
        AST.VectorRef(v, varArgs.head.asInstanceOf[AST.Expr], v.line)
      case 2 =>
        AST.MatrixRef(v, varArgs.head, varArgs(1), v.line)
      case _ =>
        throw new MatrixRuntimeException("Invalid matrix element reference", v.line)
  }

  def Var: Rule[AST.SymbolRef] = rule { case ML.Id(id) => AST.SymbolRef(Type.Undef, id.value, id.line) }

  def Expr: Rule[AST.Expr] = rule(
    { case ML.Int(l) => AST.Literal(Type.Int, l.value, l.line) },
    { case ML.Float(l) => AST.Literal(Type.Float, l.value, l.line) },
    { case ML.String(l) => AST.Literal(Type.String, l.value, l.line) },
    "uminus" { case (ML.`-`(l), Expr(e)) =>
      AST.Apply(runtime.Function.UMINUS.toRef, List(e), Type.Undef, l.line)
    },
    "add" { case (Expr(e1), ML.`\\+`(_), Expr(e2)) =>
      AST.Apply(runtime.Function.+.toRef, List(e1, e2), Type.Undef, e1.line)
    },
    "sub" { case (Expr(e1), ML.`-`(_), Expr(e2)) =>
      AST.Apply(runtime.Function.-.toRef, List(e1, e2), Type.Undef, e1.line)
    },
    "mul" { case (Expr(e1), ML.`\\*`(_), Expr(e2)) =>
      AST.Apply(runtime.Function.*.toRef, List(e1, e2), Type.Undef, e1.line)
    },
    "div" { case (Expr(e1), ML.`/`(_), Expr(e2)) =>
      AST.Apply(runtime.Function./.toRef, List(e1, e2), Type.Undef, e1.line)
    },
    "dot" { case (Expr(e1), ML.DotOp(op), Expr(e2)) =>
      AST.Apply(runtime.Function.valueOf(op.value).toRef, List(e1, e2), Type.Undef, e1.line)
    },
    { case (Expr(e), ML.`'`(_)) =>
      AST.Apply(runtime.Function.TRANSPOSE.toRef, List(e), Type.Undef, e.line)
    },
    { case (ML.`\\(`(_), Expr(e), ML.`\\)`(_)) => e },
    { case Element(el) => el },
    { case Var(v) => v },
    { case Matrix(m) => m },
    { case (ML.Function(name), ML.`\\(`(_), Varargs(args), ML.`\\)`(_)) =>
      AST.Apply(runtime.Function.valueOf(name.value).toRef, args, Type.Undef, args.headOption.map(_.line).getOrElse(-1))
    },
  )

  def Varargs: Rule[List[AST.Expr]] = rule(
    { case Expr(l) => l :: Nil },
    { case (Varargs(vs), ML.`,`(_), Expr(e)) => vs :+ e },
  )

  override val resolutions: Set[ConflictResolution] = Set(
    ML.`'`.before(production.uminus),
    production.uminus.before(production.dot),
    production.dot.before(production.mul, production.div),
    production.dot.before(ML.DotOp),
    ML.DotOp.before(production.mul, production.div),
    production.mul.before(ML.`\\*`, ML.`/`),
    production.div.before(ML.`\\*`, ML.`/`),
    production.add.before(ML.`\\+`, ML.`-`),
    production.add.after(ML.`\\*`, ML.`/`),
    production.sub.after(ML.`\\*`, ML.`/`),
    production.sub.before(ML.`\\+`, ML.`-`),
    production.`if-else`.before(ML.`else`),
    ML.`else`.before(production.`if`),
  )

  // todo: error reporting
// // #     def error(self, p: YaccProduction):
// // #         if p:
// // #             report_error(self, f"Syntax error: {p.type}('{p.value}')", p.lineno)
// // #         else:
// // #             report_error(self, "Syntax error", -1)
