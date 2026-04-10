package alpaca
package integration.scalaparser

/**
 * Parser for a subset of Scala expressions.
 *
 * Implements the following productions from the Scala 3 specification
 * (https://docs.scala-lang.org/scala3/reference/syntax.html):
 *
 *   Literal   ::=  intLit | floatLit | 'true' | 'false' | stringLit | 'null'
 *   SimpleRef ::=  id
 *   SimpleExpr::=  SimpleRef | Literal | BlockExpr | '(' Expr ')'
 *              |   SimpleExpr '.' id         (Select)
 *              |   SimpleExpr ArgumentExprs  (Apply with args / Apply empty)
 *   InfixExpr ::=  PrefixExpr | InfixExpr op InfixExpr
 *   PrefixExpr::=  ['-' | '!'] SimpleExpr
 *   Expr1     ::=  'if' '(' Expr ')' Expr 'else' Expr
 *   BlockExpr ::=  '{' '}' | '{' Block '}'
 *   Block     ::=  {BlockStat} Expr
 *   BlockStat ::=  ValDef ';' | Expr ';'
 *   ValDef    ::=  'val' id '=' Expr
 *
 * Operator precedence (from lowest to highest, matching Scala 3 spec):
 *   ||  (logical or)
 *   &&  (logical and)
 *   ==  !=  (equality)
 *   <  >  <=  >=  (relational)
 *   +  -  (additive)
 *   *  /  %  (multiplicative)
 *   -  !  (prefix / unary)
 *   .  ()  (select / apply, highest)
 */
object ScalaParser extends Parser:

  // =========================================================
  // Main expression rule
  // =========================================================

  val Expr: Rule[ScalaTree] = rule(

    // ---- InfixExpr: arithmetic ----
    "plus"   { case (Expr(l), ScalaLexer.`\\+`(_), Expr(r)) => ScalaTree.Infix(l, "+",  r) },
    "minus"  { case (Expr(l), ScalaLexer.`-`(_),   Expr(r)) => ScalaTree.Infix(l, "-",  r) },
    "times"  { case (Expr(l), ScalaLexer.`\\*`(_), Expr(r)) => ScalaTree.Infix(l, "*",  r) },
    "divide" { case (Expr(l), ScalaLexer.`/`(_),   Expr(r)) => ScalaTree.Infix(l, "/",  r) },
    "mod"    { case (Expr(l), ScalaLexer.`%`(_),   Expr(r)) => ScalaTree.Infix(l, "%",  r) },

    // ---- InfixExpr: comparison ----
    "eq"     { case (Expr(l), ScalaLexer.eqeq(_),  Expr(r)) => ScalaTree.Infix(l, "==", r) },
    "neq"    { case (Expr(l), ScalaLexer.neq(_),   Expr(r)) => ScalaTree.Infix(l, "!=", r) },
    "lt"     { case (Expr(l), ScalaLexer.`<`(_),   Expr(r)) => ScalaTree.Infix(l, "<",  r) },
    "gt"     { case (Expr(l), ScalaLexer.`>`(_),   Expr(r)) => ScalaTree.Infix(l, ">",  r) },
    "lte"    { case (Expr(l), ScalaLexer.lte(_),   Expr(r)) => ScalaTree.Infix(l, "<=", r) },
    "gte"    { case (Expr(l), ScalaLexer.gte(_),   Expr(r)) => ScalaTree.Infix(l, ">=", r) },

    // ---- InfixExpr: logical ----
    "and"    { case (Expr(l), ScalaLexer.and(_),   Expr(r)) => ScalaTree.Infix(l, "&&", r) },
    "or"     { case (Expr(l), ScalaLexer.or(_),    Expr(r)) => ScalaTree.Infix(l, "||", r) },

    // ---- PrefixExpr: unary operators ----
    "uminus" { case (ScalaLexer.`-`(_), Expr(e)) => ScalaTree.Prefix("-", e) },
    "unot"   { case (ScalaLexer.`!`(_), Expr(e)) => ScalaTree.Prefix("!", e) },

    // ---- SimpleExpr: field selection (Scala spec: SimpleExpr '.' id) ----
    "select" { case (Expr(q), ScalaLexer.`\\.`(_), ScalaLexer.id(n)) =>
      ScalaTree.Select(q, n.value)
    },

    // ---- SimpleExpr: function application (Scala spec: SimpleExpr ArgumentExprs) ----
    "apply"      { case (Expr(f), ScalaLexer.`\\(`(_), Args(args), ScalaLexer.`\\)`(_)) =>
      ScalaTree.Apply(f, args)
    },
    "applyEmpty" { case (Expr(f), ScalaLexer.`\\(`(_), ScalaLexer.`\\)`(_)) =>
      ScalaTree.Apply(f, Nil)
    },

    // ---- SimpleExpr: parenthesised expression ----
    { case (ScalaLexer.`\\(`(_), Expr(e), ScalaLexer.`\\)`(_)) => e },

    // ---- Expr1: if-then-else (Scala spec: 'if' '(' Expr ')' Expr 'else' Expr) ----
    "ifelse" { case (ScalaLexer.`if`(_), ScalaLexer.`\\(`(_), Expr(cond),
                     ScalaLexer.`\\)`(_), Expr(thn), ScalaLexer.`else`(_), Expr(els)) =>
      ScalaTree.If(cond, thn, els)
    },

    // ---- BlockExpr ----
    { case (ScalaLexer.`\\{`(_), ScalaLexer.`\\}`(_)) => ScalaTree.UnitBlock },
    { case (ScalaLexer.`\\{`(_), BlockStat.List(ss), Expr(e), ScalaLexer.`\\}`(_)) =>
      ScalaTree.Block(ss, e)
    },

    // ---- Literals ----
    { case ScalaLexer.intLit(n)    => ScalaTree.IntLit(n.value)  },
    { case ScalaLexer.floatLit(f)  => ScalaTree.FloatLit(f.value) },
    { case ScalaLexer.`true`(_)    => ScalaTree.BoolLit(true)  },
    { case ScalaLexer.`false`(_)   => ScalaTree.BoolLit(false) },
    { case ScalaLexer.`null`(_)    => ScalaTree.NullLit },
    { case ScalaLexer.stringLit(s) => ScalaTree.StringLit(s.value) },

    // ---- SimpleRef: identifier ----
    { case ScalaLexer.id(n) => ScalaTree.Ident(n.value) },
  )

  // =========================================================
  // Arguments list for function application
  // =========================================================

  val Args: Rule[List[ScalaTree]] = rule(
    { case Expr(e)                                   => List(e)       },
    { case (Args(as), ScalaLexer.`,`(_), Expr(e))   => as :+ e       },
  )

  // =========================================================
  // Block statement  (Scala spec: ValDef ';' | Expr ';')
  // =========================================================

  val BlockStat: Rule[ScalaTree] = rule(
    { case (ValDef(v), ScalaLexer.`;`(_)) => v },
    { case (Expr(e),   ScalaLexer.`;`(_)) => e },
  )

  // =========================================================
  // Value definition  (Scala spec: 'val' id '=' Expr)
  // =========================================================

  val ValDef: Rule[ScalaTree] = rule:
    case (ScalaLexer.`val`(_), ScalaLexer.id(n), ScalaLexer.`=`(_), Expr(v)) =>
      ScalaTree.ValDef(n.value, v)

  // =========================================================
  // Top-level rule
  // =========================================================

  val root: Rule[ScalaTree] = rule:
    case Expr(e) => e

  // =========================================================
  // Conflict resolutions for operator precedence
  //
  // Scala 3 spec precedence (lowest → highest):
  //   ||  →  &&  →  == !=  →  < > <= >=  →  + -  →  * / %  →  unary  →  . ()
  //
  // Each "X.after(Y)" means "don't reduce X when seeing Y" (Y binds tighter).
  // Each "X.before(Y)" means "reduce X before shifting Y" (X binds tighter).
  // =========================================================

  override val resolutions: Set[ConflictResolution] = Set(

    // ------------------------------------------------------------------
    // Select '.' and Apply '(' bind tightest: delay reducing everything
    // else when the next token is '.' or '('
    // ------------------------------------------------------------------
    production.plus.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.minus.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.times.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.divide.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.mod.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.eq.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.neq.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.lt.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.gt.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.lte.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.gte.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.and.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.or.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.uminus.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.unot.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),
    production.ifelse.after(ScalaLexer.`\\.`, ScalaLexer.`\\(`),

    // ------------------------------------------------------------------
    // Unary '-' and '!' bind tighter than all binary operators
    // ------------------------------------------------------------------
    production.uminus.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.uminus.before(ScalaLexer.`\\+`, ScalaLexer.`-`),
    production.uminus.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.uminus.before(ScalaLexer.eqeq, ScalaLexer.neq),
    production.uminus.before(ScalaLexer.and),
    production.uminus.before(ScalaLexer.or),

    production.unot.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.unot.before(ScalaLexer.`\\+`, ScalaLexer.`-`),
    production.unot.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.unot.before(ScalaLexer.eqeq, ScalaLexer.neq),
    production.unot.before(ScalaLexer.and),
    production.unot.before(ScalaLexer.or),

    // ------------------------------------------------------------------
    // '*' '/' '%' have higher precedence than '+' '-'
    // ------------------------------------------------------------------
    production.plus.after(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.minus.after(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),

    // Left-associativity of '+' and '-'
    production.plus.before(ScalaLexer.`\\+`, ScalaLexer.`-`),
    production.minus.before(ScalaLexer.`\\+`, ScalaLexer.`-`),

    // Left-associativity of '*' '/' '%'
    production.times.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.divide.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.mod.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),

    // ------------------------------------------------------------------
    // '+' '-' have higher precedence than '<' '>' '<=' '>='
    // ------------------------------------------------------------------
    production.lt.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.gt.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.lte.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.gte.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),

    // Left-associativity of '<' '>' '<=' '>='
    production.lt.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.gt.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.lte.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.gte.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),

    // ------------------------------------------------------------------
    // '<' '>' '<=' '>=' have higher precedence than '==' '!='
    // ------------------------------------------------------------------
    production.eq.after(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.neq.after(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),

    // Left-associativity of '==' '!='
    production.eq.before(ScalaLexer.eqeq, ScalaLexer.neq),
    production.neq.before(ScalaLexer.eqeq, ScalaLexer.neq),

    // ------------------------------------------------------------------
    // '==' '!=' have higher precedence than '&&'
    // ------------------------------------------------------------------
    production.and.after(ScalaLexer.eqeq, ScalaLexer.neq),

    // Left-associativity of '&&'
    production.and.before(ScalaLexer.and),

    // ------------------------------------------------------------------
    // '&&' has higher precedence than '||'
    // ------------------------------------------------------------------
    production.or.after(ScalaLexer.and),

    // Left-associativity of '||'
    production.or.before(ScalaLexer.or),

    // ------------------------------------------------------------------
    // 'if-else' extends as far right as possible:
    // the else-branch is greedy and captures following binary operators
    // ------------------------------------------------------------------
    production.ifelse.after(
      ScalaLexer.`\\+`, ScalaLexer.`-`,
      ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`,
      ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte,
      ScalaLexer.eqeq, ScalaLexer.neq,
      ScalaLexer.and, ScalaLexer.or,
    ),
  )
