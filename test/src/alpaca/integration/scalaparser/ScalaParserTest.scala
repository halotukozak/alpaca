package alpaca
package integration.scalaparser

import ScalaTree.*
import ScalaPattern.*
import ScalaType.*
import org.scalatest.funsuite.AnyFunSuite

final class ScalaParserTest extends AnyFunSuite:

  private def parse(input: String): ScalaTree =
    val (_, lexemes) = ScalaLexer.tokenize(input)
    val (_, result) = ScalaParser.parse(lexemes)
    result.nn

  // ===========================================================
  // Literals
  // ===========================================================

  test("integer literal") {
    assert(parse("42") == IntLit(42L))
  }

  test("float literal") {
    assert(parse("3.14") == FloatLit(3.14))
  }

  test("boolean literal true") {
    assert(parse("true") == BoolLit(true))
  }

  test("boolean literal false") {
    assert(parse("false") == BoolLit(false))
  }

  test("string literal") {
    assert(parse(""""hello"""") == StringLit("hello"))
  }

  test("null literal") {
    assert(parse("null") == NullLit)
  }

  // ===========================================================
  // Identifiers
  // ===========================================================

  test("identifier") {
    assert(parse("x") == Ident("x"))
  }

  test("identifier with digits and underscore") {
    assert(parse("my_val2") == Ident("my_val2"))
  }

  test("keyword prefix not treated as keyword") {
    // "iffy" must lex as a single identifier, not "if" + "fy"
    assert(parse("iffy") == Ident("iffy"))
  }

  // ===========================================================
  // Arithmetic: basic operators
  // ===========================================================

  test("addition") {
    assert(parse("1 + 2") == Infix(IntLit(1), "+", IntLit(2)))
  }

  test("subtraction") {
    assert(parse("10 - 3") == Infix(IntLit(10), "-", IntLit(3)))
  }

  test("multiplication") {
    assert(parse("4 * 5") == Infix(IntLit(4), "*", IntLit(5)))
  }

  test("division") {
    assert(parse("10 / 2") == Infix(IntLit(10), "/", IntLit(2)))
  }

  test("modulo") {
    assert(parse("7 % 3") == Infix(IntLit(7), "%", IntLit(3)))
  }

  // ===========================================================
  // Arithmetic: precedence
  // ===========================================================

  test("multiplication before addition") {
    // 1 + 2 * 3  ==  1 + (2 * 3)
    assert(parse("1 + 2 * 3") == Infix(IntLit(1), "+", Infix(IntLit(2), "*", IntLit(3))))
  }

  test("multiplication before subtraction") {
    // 10 - 2 * 3  ==  10 - (2 * 3)
    assert(parse("10 - 2 * 3") == Infix(IntLit(10), "-", Infix(IntLit(2), "*", IntLit(3))))
  }

  test("mixed arithmetic precedence") {
    // 2 * 3 + 4 * 5  ==  (2*3) + (4*5)
    assert(parse("2 * 3 + 4 * 5") == Infix(Infix(IntLit(2), "*", IntLit(3)), "+", Infix(IntLit(4), "*", IntLit(5))))
  }

  // ===========================================================
  // Arithmetic: left-associativity
  // ===========================================================

  test("addition is left-associative") {
    // 1 + 2 + 3  ==  (1 + 2) + 3
    assert(parse("1 + 2 + 3") == Infix(Infix(IntLit(1), "+", IntLit(2)), "+", IntLit(3)))
  }

  test("subtraction is left-associative") {
    // 10 - 3 - 2  ==  (10 - 3) - 2
    assert(parse("10 - 3 - 2") == Infix(Infix(IntLit(10), "-", IntLit(3)), "-", IntLit(2)))
  }

  test("multiplication is left-associative") {
    // 2 * 3 * 4  ==  (2 * 3) * 4
    assert(parse("2 * 3 * 4") == Infix(Infix(IntLit(2), "*", IntLit(3)), "*", IntLit(4)))
  }

  // ===========================================================
  // Parentheses override precedence
  // ===========================================================

  test("parentheses override precedence") {
    // (1 + 2) * 3  ==  3 * 3
    assert(parse("(1 + 2) * 3") == Infix(Infix(IntLit(1), "+", IntLit(2)), "*", IntLit(3)))
  }

  // ===========================================================
  // Comparison operators
  // ===========================================================

  test("equality") {
    assert(parse("x == y") == Infix(Ident("x"), "==", Ident("y")))
  }

  test("inequality") {
    assert(parse("x != y") == Infix(Ident("x"), "!=", Ident("y")))
  }

  test("less than") {
    assert(parse("a < b") == Infix(Ident("a"), "<", Ident("b")))
  }

  test("less than or equal") {
    assert(parse("a <= b") == Infix(Ident("a"), "<=", Ident("b")))
  }

  test("greater than") {
    assert(parse("a > b") == Infix(Ident("a"), ">", Ident("b")))
  }

  test("greater than or equal") {
    assert(parse("a >= b") == Infix(Ident("a"), ">=", Ident("b")))
  }

  test("arithmetic before comparison") {
    // a + b < c  ==  (a + b) < c
    assert(parse("a + b < c") == Infix(Infix(Ident("a"), "+", Ident("b")), "<", Ident("c")))
  }

  // ===========================================================
  // Logical operators
  // ===========================================================

  test("logical and") {
    assert(parse("a && b") == Infix(Ident("a"), "&&", Ident("b")))
  }

  test("logical or") {
    assert(parse("a || b") == Infix(Ident("a"), "||", Ident("b")))
  }

  test("and before or") {
    // a || b && c  ==  a || (b && c)
    assert(parse("a || b && c") == Infix(Ident("a"), "||", Infix(Ident("b"), "&&", Ident("c"))))
  }

  test("comparison before logical and") {
    // a && b == c  ==  a && (b == c)
    assert(parse("a && b == c") == Infix(Ident("a"), "&&", Infix(Ident("b"), "==", Ident("c"))))
  }

  // ===========================================================
  // Prefix (unary) operators
  // ===========================================================

  test("unary minus") {
    assert(parse("-x") == Prefix("-", Ident("x")))
  }

  test("logical not") {
    assert(parse("!flag") == Prefix("!", Ident("flag")))
  }

  test("unary minus binds tighter than multiplication") {
    // -x * y  ==  (-x) * y
    assert(parse("-x * y") == Infix(Prefix("-", Ident("x")), "*", Ident("y")))
  }

  test("unary minus binds tighter than addition") {
    // -x + y  ==  (-x) + y
    assert(parse("-x + y") == Infix(Prefix("-", Ident("x")), "+", Ident("y")))
  }

  // ===========================================================
  // Field selection  (Scala spec: SimpleExpr '.' id)
  // ===========================================================

  test("field selection") {
    assert(parse("obj.field") == Select(Ident("obj"), "field"))
  }

  test("chained field selection") {
    // a.b.c  ==  (a.b).c  (left-associative)
    assert(parse("a.b.c") == Select(Select(Ident("a"), "b"), "c"))
  }

  test("selection binds tighter than addition") {
    // a.b + c  ==  (a.b) + c
    assert(parse("a.b + c") == Infix(Select(Ident("a"), "b"), "+", Ident("c")))
  }

  test("selection on right-hand side of operator") {
    // a + b.c  ==  a + (b.c)
    assert(parse("a + b.c") == Infix(Ident("a"), "+", Select(Ident("b"), "c")))
  }

  // ===========================================================
  // Function application  (Scala spec: SimpleExpr ArgumentExprs)
  // ===========================================================

  test("function call with no arguments") {
    assert(parse("f()") == Apply(Ident("f"), Nil))
  }

  test("function call with one argument") {
    assert(parse("f(x)") == Apply(Ident("f"), List(Ident("x"))))
  }

  test("function call with multiple arguments") {
    assert(parse("f(x, y, z)") == Apply(Ident("f"), List(Ident("x"), Ident("y"), Ident("z"))))
  }

  test("method call on object") {
    assert(parse("obj.method(x)") == Apply(Select(Ident("obj"), "method"), List(Ident("x"))))
  }

  test("chained method calls") {
    // a.b().c()  ==  ((a.b)()).c()
    assert(parse("a.b().c()") == Apply(Select(Apply(Select(Ident("a"), "b"), Nil), "c"), Nil))
  }

  test("apply binds tighter than addition on left") {
    // f(x) + 1  ==  (f(x)) + 1
    assert(parse("f(x) + 1") == Infix(Apply(Ident("f"), List(Ident("x"))), "+", IntLit(1)))
  }

  test("apply binds tighter than addition on right") {
    // 1 + f(x)  ==  1 + (f(x))
    assert(parse("1 + f(x)") == Infix(IntLit(1), "+", Apply(Ident("f"), List(Ident("x")))))
  }

  test("expression argument in function call") {
    assert(parse("f(a + b)") == Apply(Ident("f"), List(Infix(Ident("a"), "+", Ident("b")))))
  }

  // ===========================================================
  // If-else expression  (Scala spec: Expr1)
  // ===========================================================

  test("if-else expression") {
    assert(parse("if (x) a else b") == If(Ident("x"), Ident("a"), Ident("b")))
  }

  test("if-else with arithmetic condition") {
    assert(parse("if (x > 0) x else -x") == If(Infix(Ident("x"), ">", IntLit(0)), Ident("x"), Prefix("-", Ident("x"))))
  }

  test("else branch is greedy: extends to the right") {
    // if (c) a else b + 1  ==  if (c) a else (b + 1)
    assert(parse("if (c) a else b + 1") == If(Ident("c"), Ident("a"), Infix(Ident("b"), "+", IntLit(1))))
  }

  test("nested if-else: inner else binds to nearest if") {
    // if (c1) if (c2) a else b else c
    // Parsed as: if (c1) (if (c2) a else b) else c
    assert(
      parse("if (c1) if (c2) a else b else c") == If(Ident("c1"), If(Ident("c2"), Ident("a"), Ident("b")), Ident("c")),
    )
  }

  // ===========================================================
  // Block expressions  (Scala spec: BlockExpr / Block)
  // ===========================================================

  test("empty block") {
    assert(parse("{}") == UnitBlock)
  }

  test("block with single expression") {
    assert(parse("{ 42 }") == Block(Nil, IntLit(42)))
  }

  test("block with single val definition") {
    assert(parse("{ val x = 1; x }") == Block(List(ValDef("x", IntLit(1))), Ident("x")))
  }

  test("block with multiple val definitions") {
    assert(
      parse("{ val x = 1; val y = 2; x + y }") == Block(
        List(ValDef("x", IntLit(1)), ValDef("y", IntLit(2))),
        Infix(Ident("x"), "+", Ident("y")),
      ),
    )
  }

  test("block with expression statement") {
    assert(parse("{ x; y }") == Block(List(Ident("x")), Ident("y")))
  }

  test("val definition with complex rhs") {
    assert(
      parse("{ val x = a + b * c; x }") == Block(
        List(ValDef("x", Infix(Ident("a"), "+", Infix(Ident("b"), "*", Ident("c"))))),
        Ident("x"),
      ),
    )
  }

  test("block used inside larger expression") {
    assert(parse("{ val x = 2; x } + 1") == Infix(Block(List(ValDef("x", IntLit(2))), Ident("x")), "+", IntLit(1)))
  }

  // ===========================================================
  // Complex / combined expressions
  // ===========================================================

  test("recursive factorial pattern") {
    // if (n <= 0) 1 else n * f(n - 1)
    assert(
      parse("if (n <= 0) 1 else n * f(n - 1)") == If(
        Infix(Ident("n"), "<=", IntLit(0)),
        IntLit(1),
        Infix(Ident("n"), "*", Apply(Ident("f"), List(Infix(Ident("n"), "-", IntLit(1))))),
      ),
    )
  }

  test("full precedence ladder") {
    // a || b && c == d < e + f * g
    // Parsed as: a || (b && (c == (d < (e + (f * g)))))
    assert(
      parse("a || b && c == d < e + f * g") == Infix(
        Ident("a"),
        "||",
        Infix(
          Ident("b"),
          "&&",
          Infix(
            Ident("c"),
            "==",
            Infix(
              Ident("d"),
              "<",
              Infix(Ident("e"), "+", Infix(Ident("f"), "*", Ident("g"))),
            ),
          ),
        ),
      ),
    )
  }

  test("method chain with arithmetic") {
    // xs.filter(p).length + 1  ==  ((xs.filter(p)).length) + 1
    assert(
      parse("xs.filter(p).length + 1") == Infix(
        Select(Apply(Select(Ident("xs"), "filter"), List(Ident("p"))), "length"),
        "+",
        IntLit(1),
      ),
    )
  }

  // ===========================================================
  // var definitions (Scala 3 spec: VarDef ::= 'var' id '=' Expr)
  // ===========================================================

  test("var definition in block") {
    assert(parse("{ var x = 1; x }") == Block(List(VarDef("x", IntLit(1))), Ident("x")))
  }

  test("val and var mixed in block") {
    assert(
      parse("{ val x = 1; var y = 2; x + y }") == Block(
        List(ValDef("x", IntLit(1)), VarDef("y", IntLit(2))),
        Infix(Ident("x"), "+", Ident("y")),
      ),
    )
  }

  // ===========================================================
  // new expressions (Scala 3 spec: SimpleExpr ::= 'new' ConstrApp; simplified: no args)
  // ===========================================================

  test("new with bare class name") {
    assert(parse("new Foo") == New("Foo", Nil))
  }

  test("new used in expression") {
    assert(parse("new Foo.bar") == Select(New("Foo", Nil), "bar"))
  }

  test("new as val rhs") {
    assert(parse("{ val x = new Foo; x }") == Block(List(ValDef("x", New("Foo", Nil))), Ident("x")))
  }

  // ===========================================================
  // Types (Scala 3 spec: Type, SimpleType — simplified)
  //   Only used via def/class/param positions; no function types.
  // ===========================================================

  test("def with simple return type") {
    // def zero(): Int = 0
    assert(
      parse("{ def zero(): Int = 0; zero() }") == Block(
        List(DefDef("zero", Nil, TypeRef("Int"), IntLit(0))),
        Apply(Ident("zero"), Nil),
      ),
    )
  }

  test("def with single parameter") {
    // def inc(x: Int): Int = x + 1
    assert(
      parse("{ def inc(x: Int): Int = x + 1; inc(41) }") == Block(
        List(
          DefDef(
            "inc",
            List(Param("x", TypeRef("Int"))),
            TypeRef("Int"),
            Infix(Ident("x"), "+", IntLit(1)),
          ),
        ),
        Apply(Ident("inc"), List(IntLit(41))),
      ),
    )
  }

  test("def with multiple parameters") {
    // def add(x: Int, y: Int): Int = x + y
    assert(
      parse("{ def add(x: Int, y: Int): Int = x + y; add(1, 2) }") == Block(
        List(
          DefDef(
            "add",
            List(Param("x", TypeRef("Int")), Param("y", TypeRef("Int"))),
            TypeRef("Int"),
            Infix(Ident("x"), "+", Ident("y")),
          ),
        ),
        Apply(Ident("add"), List(IntLit(1), IntLit(2))),
      ),
    )
  }

  test("def with applied return type") {
    // def empty(): List[Int] = xs
    assert(
      parse("{ def empty(): List[Int] = xs; empty() }") == Block(
        List(DefDef("empty", Nil, AppliedType("List", List(TypeRef("Int"))), Ident("xs"))),
        Apply(Ident("empty"), Nil),
      ),
    )
  }

  test("def with nested type parameter") {
    // def pairs(): Map[String, List[Int]] = xs
    assert(
      parse("{ def pairs(): Map[String, List[Int]] = xs; pairs() }") == Block(
        List(
          DefDef(
            "pairs",
            Nil,
            AppliedType("Map", List(TypeRef("String"), AppliedType("List", List(TypeRef("Int"))))),
            Ident("xs"),
          ),
        ),
        Apply(Ident("pairs"), Nil),
      ),
    )
  }

  test("recursive factorial def") {
    // def fact(n: Int): Int = if (n <= 0) 1 else n * fact(n - 1)
    assert(
      parse("{ def fact(n: Int): Int = if (n <= 0) 1 else n * fact(n - 1); fact(5) }") == Block(
        List(
          DefDef(
            "fact",
            List(Param("n", TypeRef("Int"))),
            TypeRef("Int"),
            If(
              Infix(Ident("n"), "<=", IntLit(0)),
              IntLit(1),
              Infix(
                Ident("n"),
                "*",
                Apply(Ident("fact"), List(Infix(Ident("n"), "-", IntLit(1)))),
              ),
            ),
          ),
        ),
        Apply(Ident("fact"), List(IntLit(5))),
      ),
    )
  }

  // ===========================================================
  // class definitions (Scala 3 spec: ClassDef — simplified; no traits, no type params)
  //   ClassDef ::= 'class' id ['(' Params ')'] ['extends' id] BlockExpr
  // ===========================================================

  test("empty class") {
    assert(parse("{ class Empty {}; null }") == Block(List(ClassDef("Empty", Nil, None, UnitBlock)), NullLit))
  }

  test("class with constructor params") {
    assert(
      parse("{ class Point(x: Int, y: Int) {}; null }") == Block(
        List(
          ClassDef(
            "Point",
            List(Param("x", TypeRef("Int")), Param("y", TypeRef("Int"))),
            None,
            UnitBlock,
          ),
        ),
        NullLit,
      ),
    )
  }

  test("class extending parent") {
    assert(
      parse("{ class Dog extends Animal {}; null }") == Block(
        List(ClassDef("Dog", Nil, Some("Animal"), UnitBlock)),
        NullLit,
      ),
    )
  }

  test("class with params and parent") {
    assert(
      parse("{ class Cat(name: String) extends Animal {}; null }") == Block(
        List(
          ClassDef(
            "Cat",
            List(Param("name", TypeRef("String"))),
            Some("Animal"),
            UnitBlock,
          ),
        ),
        NullLit,
      ),
    )
  }

  test("class with method in body") {
    assert(
      parse("{ class Box(v: Int) { def get(): Int = v; 0 }; null }") == Block(
        List(
          ClassDef(
            "Box",
            List(Param("v", TypeRef("Int"))),
            None,
            Block(
              List(DefDef("get", Nil, TypeRef("Int"), Ident("v"))),
              IntLit(0),
            ),
          ),
        ),
        NullLit,
      ),
    )
  }

  // ===========================================================
  // Partial function literals (Scala 3 spec: BlockExpr ::= '{' CaseClauses '}')
  //
  // These stand in for full match expressions — to simulate
  // `x match { case p => e }`, apply the pfun: `({ case p => e })(x)`.
  // ===========================================================

  test("pfun with wildcard") {
    assert(
      parse("{ case _ => 0 }") == PartialFun(
        List(MatchCase(Wildcard, None, IntLit(0))),
      ),
    )
  }

  test("pfun with variable pattern") {
    assert(
      parse("{ case n => n + 1 }") == PartialFun(
        List(MatchCase(VarPat("n"), None, Infix(Ident("n"), "+", IntLit(1)))),
      ),
    )
  }

  test("pfun with literal patterns") {
    assert(
      parse("""{ case 0 => "zero" case 1 => "one" case _ => "many" }""") == PartialFun(
        List(
          MatchCase(LitPat(IntLit(0)), None, StringLit("zero")),
          MatchCase(LitPat(IntLit(1)), None, StringLit("one")),
          MatchCase(Wildcard, None, StringLit("many")),
        ),
      ),
    )
  }

  test("pfun with guard") {
    assert(
      parse("{ case n if n > 0 => n }") == PartialFun(
        List(MatchCase(VarPat("n"), Some(Infix(Ident("n"), ">", IntLit(0))), Ident("n"))),
      ),
    )
  }

  test("pfun with constructor pattern") {
    assert(
      parse("{ case Some(v) => v case None() => 0 }") == PartialFun(
        List(
          MatchCase(ConstrPat("Some", List(VarPat("v"))), None, Ident("v")),
          MatchCase(ConstrPat("None", Nil), None, IntLit(0)),
        ),
      ),
    )
  }

  test("pfun with nested constructor pattern") {
    assert(
      parse("{ case Some(Some(x)) => x }") == PartialFun(
        List(
          MatchCase(
            ConstrPat("Some", List(ConstrPat("Some", List(VarPat("x"))))),
            None,
            Ident("x"),
          ),
        ),
      ),
    )
  }

  test("pfun with bind pattern") {
    assert(
      parse("{ case all @ Some(v) => all }") == PartialFun(
        List(MatchCase(BindPat("all", ConstrPat("Some", List(VarPat("v")))), None, Ident("all"))),
      ),
    )
  }

  test("pfun applied to argument (simulating match)") {
    // Simulate `x match { case 0 => "zero"; case _ => "other" }` as
    // `({ case 0 => "zero" case _ => "other" })(x)`
    assert(
      parse("""({ case 0 => "zero" case _ => "other" })(x)""") == Apply(
        PartialFun(
          List(
            MatchCase(LitPat(IntLit(0)), None, StringLit("zero")),
            MatchCase(Wildcard, None, StringLit("other")),
          ),
        ),
        List(Ident("x")),
      ),
    )
  }
