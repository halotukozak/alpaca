package alpaca
package integration.scalaparser

import org.scalatest.funsuite.AnyFunSuite

import scala.meta.*
import scala.meta.dialects.Scala3

/**
 * Cross-check: parse the same source with both ScalaParser (this project's
 * LALR implementation) and scala-meta (reference). Normalise each AST to a
 * common s-expression string and assert equality.
 *
 * The renderer is intentionally minimal — it only covers node shapes the
 * LALR parser can produce. Any input exercised here must be parseable by
 * both. Productions the LALR parser doesn't support (full match, lambdas,
 * typed patterns, ...) are simply absent from the renderer and their tests
 * live elsewhere.
 */
final class ScalaMetaCrossTest extends AnyFunSuite:

  private given Dialect = Scala3

  private def parseMine(src: String): ScalaTree =
    val (_, lexemes) = ScalaLexer.tokenize(src)
    val (_, result) = ScalaParser.parse(lexemes)
    result.nn

  private def parseMeta(src: String): Term = src.parse[Term].get

  // ---------- Renderer for Alpaca's ScalaTree ----------

  private def renderMine(t: ScalaTree): String = t match
    case ScalaTree.IntLit(v) => s"(Int $v)"
    case ScalaTree.FloatLit(v) => s"(Float $v)"
    case ScalaTree.BoolLit(v) => s"(Bool $v)"
    case ScalaTree.StringLit(v) => s"""(Str "$v")"""
    case ScalaTree.CharLit(v) => s"""(Char "$v")"""
    case ScalaTree.NullLit => "(Null)"
    case ScalaTree.Ident(n) => s"(Id $n)"
    case ScalaTree.Select(q, n) => s"(Select ${renderMine(q)} $n)"
    case ScalaTree.Apply(f, args) =>
      s"(Apply ${renderMine(f)} [${args.map(renderMine).mkString(",")}])"
    case ScalaTree.Prefix(op, e) => s"(Prefix $op ${renderMine(e)})"
    case ScalaTree.Infix(l, op, r) => s"(Infix ${renderMine(l)} $op ${renderMine(r)})"
    case ScalaTree.If(c, th, el) => s"(If ${renderMine(c)} ${renderMine(th)} ${renderMine(el)})"
    case ScalaTree.Block(ss, e) =>
      s"(Block [${ss.map(renderMine).mkString(",")}] ${renderMine(e)})"
    case ScalaTree.UnitBlock => "(Block [] (Unit))"
    case ScalaTree.ValDef(n, _, v) => s"(Val $n ${renderMine(v)})"
    case other => s"(UNSUPPORTED ${other.getClass.getSimpleName})"

  // ---------- Renderer for scala-meta's Tree ----------
  //
  // Uses method-based access on traits (fun, args, lhs, op, ...) so the
  // renderer is robust to the versioned `After_X_Y_0` / `Initial` case
  // classes scala-meta introduces as the AST evolves.

  private def renderMeta(t: Tree): String = t match
    case Lit.Int(v) => s"(Int $v)"
    case Lit.Long(v) => s"(Int $v)" // Alpaca stores all ints as Long
    case Lit.Double(v) => s"(Float $v)"
    case Lit.Float(v) => s"(Float ${v.toDouble})"
    case Lit.Boolean(v) => s"(Bool $v)"
    case Lit.String(v) => s"""(Str "$v")"""
    case Lit.Char(v) => s"""(Char "$v")"""
    case Lit.Null() => "(Null)"

    case n: Term.Name => s"(Id ${n.value})"
    case s: Term.Select => s"(Select ${renderMeta(s.qual)} ${s.name.value})"
    case a: Term.Apply => s"(Apply ${renderMeta(a.fun)} [${a.args.map(renderMeta).mkString(",")}])"
    case i: Term.ApplyInfix =>
      val args = i.args
      require(args.length == 1, s"Alpaca only supports single-arg infix; got $args")
      s"(Infix ${renderMeta(i.lhs)} ${i.op.value} ${renderMeta(args.head)})"
    case u: Term.ApplyUnary => s"(Prefix ${u.op.value} ${renderMeta(u.arg)})"
    case ifTree: Term.If => s"(If ${renderMeta(ifTree.cond)} ${renderMeta(ifTree.thenp)} ${renderMeta(ifTree.elsep)})"

    case b: Term.Block =>
      // scala-meta Block lumps everything into a single `stats` list; split
      // off the trailing Term to match Alpaca's Block(stmts, result) shape.
      b.stats.lastOption match
        case Some(last: Term) if b.stats.size > 1 =>
          s"(Block [${b.stats.init.map(renderMeta).mkString(",")}] ${renderMeta(last)})"
        case Some(last: Term) => s"(Block [] ${renderMeta(last)})"
        case _ => s"(Block [${b.stats.map(renderMeta).mkString(",")}] (Unit))"
    case v: Defn.Val =>
      // Defn.Val has pats: List[Pat]; we only handle the single-name case.
      val name = v.pats match
        case List(Pat.Var(Term.Name(n))) => n
        case other => sys.error(s"unexpected Defn.Val pats: $other")
      s"(Val $name ${renderMeta(v.rhs)})"

    case other => s"(UNSUPPORTED ${other.productPrefix})"

  // ---------- Helper ----------

  private def cross(input: String): Unit =
    val mine = renderMine(parseMine(input))
    val meta = renderMeta(parseMeta(input))
    assert(mine == meta, s"\nInput:      $input\nAlpaca:     $mine\nScala-meta: $meta")

  // ---------- Tests ----------

  test("cross: integer literal")(cross("42"))
  test("cross: float literal")(cross("3.14"))
  test("cross: boolean true")(cross("true"))
  test("cross: boolean false")(cross("false"))
  test("cross: null")(cross("null"))
  test("cross: string literal")(cross(""""hello""""))
  test("cross: identifier")(cross("x"))

  test("cross: addition")(cross("1 + 2"))
  test("cross: subtraction")(cross("10 - 3"))
  test("cross: multiplication")(cross("4 * 5"))
  test("cross: precedence: mul before add")(cross("1 + 2 * 3"))
  test("cross: left-assoc addition")(cross("1 + 2 + 3"))
  test("cross: mixed arithmetic")(cross("2 * 3 + 4 * 5"))

  test("cross: comparison")(cross("a < b"))
  test("cross: equality")(cross("x == y"))
  test("cross: arithmetic before comparison")(cross("a + b < c"))

  test("cross: logical and")(cross("a && b"))
  test("cross: logical or")(cross("a || b"))
  test("cross: and before or")(cross("a || b && c"))

  test("cross: unary minus")(cross("-x"))
  test("cross: logical not")(cross("!flag"))
  test("cross: unary minus precedence")(cross("-x * y"))

  test("cross: field selection")(cross("obj.field"))
  test("cross: chained selection")(cross("a.b.c"))

  test("cross: function call no args")(cross("f()"))
  test("cross: function call one arg")(cross("f(x)"))
  test("cross: function call many args")(cross("f(x, y, z)"))
  test("cross: method call")(cross("obj.method(x)"))
  test("cross: chained method calls")(cross("a.b().c()"))

  test("cross: if-else")(cross("if (x) a else b"))
  test("cross: if-else arithmetic")(cross("if (x > 0) x else -x"))

  test("cross: block single expr")(cross("{ 42 }"))
  test("cross: block val + expr")(cross("{ val x = 1; x }"))
  test("cross: block val with complex rhs")(cross("{ val y = a + b * c; y }"))
