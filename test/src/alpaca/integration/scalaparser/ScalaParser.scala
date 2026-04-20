package alpaca
package integration.scalaparser

/**
 * Parser for a subset of Scala expressions.
 *
 * Implements productions from the Scala 3 specification
 * (https://docs.scala-lang.org/scala3/reference/syntax.html) restricted to
 * classic brace syntax (no significant indentation, no indent/outdent). See
 * [[ScalaAST]] for the list of productions covered vs. intentionally dropped.
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
    "plus" { case (Expr(l), ScalaLexer.`\\+`(_), Expr(r)) => ScalaTree.Infix(l, "+", r) },
    "minus" { case (Expr(l), ScalaLexer.`-`(_), Expr(r)) => ScalaTree.Infix(l, "-", r) },
    "times" { case (Expr(l), ScalaLexer.`\\*`(_), Expr(r)) => ScalaTree.Infix(l, "*", r) },
    "divide" { case (Expr(l), ScalaLexer.`/`(_), Expr(r)) => ScalaTree.Infix(l, "/", r) },
    "mod" { case (Expr(l), ScalaLexer.`%`(_), Expr(r)) => ScalaTree.Infix(l, "%", r) },

    // ---- InfixExpr: comparison ----
    "eq" { case (Expr(l), ScalaLexer.eqeq(_), Expr(r)) => ScalaTree.Infix(l, "==", r) },
    "neq" { case (Expr(l), ScalaLexer.neq(_), Expr(r)) => ScalaTree.Infix(l, "!=", r) },
    "lt" { case (Expr(l), ScalaLexer.`<`(_), Expr(r)) => ScalaTree.Infix(l, "<", r) },
    "gt" { case (Expr(l), ScalaLexer.`>`(_), Expr(r)) => ScalaTree.Infix(l, ">", r) },
    "lte" { case (Expr(l), ScalaLexer.lte(_), Expr(r)) => ScalaTree.Infix(l, "<=", r) },
    "gte" { case (Expr(l), ScalaLexer.gte(_), Expr(r)) => ScalaTree.Infix(l, ">=", r) },

    // ---- InfixExpr: logical ----
    "and" { case (Expr(l), ScalaLexer.and(_), Expr(r)) => ScalaTree.Infix(l, "&&", r) },
    "or" { case (Expr(l), ScalaLexer.or(_), Expr(r)) => ScalaTree.Infix(l, "||", r) },

    // ---- PrefixExpr: unary operators ----
    "uminus" { case (ScalaLexer.`-`(_), Expr(e)) => ScalaTree.Prefix("-", e) },
    "unot" { case (ScalaLexer.`!`(_), Expr(e)) => ScalaTree.Prefix("!", e) },

    // ---- SimpleExpr: field selection (Scala spec: SimpleExpr '.' id) ----
    "select" { case (Expr(q), ScalaLexer.`\\.`(_), ScalaLexer.id(n)) =>
      ScalaTree.Select(q, n.value)
    },

    // ---- SimpleExpr: function application (Scala spec: SimpleExpr ArgumentExprs) ----
    "apply" { case (Expr(f), ScalaLexer.`\\(`(_), Args(args), ScalaLexer.`\\)`(_)) =>
      ScalaTree.Apply(f, args)
    },
    "applyEmpty" { case (Expr(f), ScalaLexer.`\\(`(_), ScalaLexer.`\\)`(_)) =>
      ScalaTree.Apply(f, Nil)
    },

    // ---- SimpleExpr: parenthesised expression ----
    { case (ScalaLexer.`\\(`(_), Expr(e), ScalaLexer.`\\)`(_)) => e },

    // ---- SimpleExpr: 'new' id (Scala spec: 'new' ConstrApp, simplified, no args) ----
    { case (ScalaLexer.`new`(_), ScalaLexer.id(n)) => ScalaTree.New(n.value, Nil) },

    // ---- Expr1: if-then-else (Scala spec: 'if' '(' Expr ')' Expr 'else' Expr) ----
    "ifelse" {
      case (
            ScalaLexer.`if`(_),
            ScalaLexer.`\\(`(_),
            Expr(cond),
            ScalaLexer.`\\)`(_),
            Expr(thn),
            ScalaLexer.`else`(_),
            Expr(els),
          ) =>
        ScalaTree.If(cond, thn, els)
    },

    // ---- BlockExpr (Scala spec: BlockExpr ::= '{' (CaseClauses | Block) '}') ----
    { case (ScalaLexer.`\\{`(_), ScalaLexer.`\\}`(_)) => ScalaTree.UnitBlock },
    { case (ScalaLexer.`\\{`(_), BlockStat.List(ss), Expr(e), ScalaLexer.`\\}`(_)) =>
      ScalaTree.Block(ss, e)
    },
    // Partial function literal: `{ case p1 => e1; case p2 => e2 }`
    // Uses CaseClauses (non-empty list) so `{}` stays unambiguously a UnitBlock.
    "pfun" { case (ScalaLexer.`\\{`(_), CaseClauses(cs), ScalaLexer.`\\}`(_)) =>
      ScalaTree.PartialFun(cs)
    },

    // ---- Literals ----
    { case ScalaLexer.intLit(n) => ScalaTree.IntLit(n.value) },
    { case ScalaLexer.floatLit(f) => ScalaTree.FloatLit(f.value) },
    { case ScalaLexer.`true`(_) => ScalaTree.BoolLit(true) },
    { case ScalaLexer.`false`(_) => ScalaTree.BoolLit(false) },
    { case ScalaLexer.`null`(_) => ScalaTree.NullLit },
    { case ScalaLexer.stringLit(s) => ScalaTree.StringLit(s.value) },

    // ---- SimpleRef: identifier ----
    { case ScalaLexer.id(n) => ScalaTree.Ident(n.value) },
  )

  // =========================================================
  // Arguments list for function application
  // =========================================================

  val Args: Rule[List[ScalaTree]] = rule(
    { case Expr(e) => List(e) },
    { case (Args(as), ScalaLexer.`,`(_), Expr(e)) => as :+ e },
  )

  // =========================================================
  // Block statements (Scala spec: BlockStat)
  //
  //   BlockStat ::= Def ';' | Expr ';'
  //   Def       ::= ValDef | VarDef | DefDef | ClassDef
  // =========================================================

  val BlockStat: Rule[ScalaTree] = rule(
    { case (ValDef(v), ScalaLexer.`;`(_)) => v },
    { case (VarDef(v), ScalaLexer.`;`(_)) => v },
    { case (DefDef(d), ScalaLexer.`;`(_)) => d },
    { case (ClassDef(c), ScalaLexer.`;`(_)) => c },
    { case (Expr(e), ScalaLexer.`;`(_)) => e },
  )

  // =========================================================
  // Value and variable definitions
  //
  //   ValDef ::= 'val' id '=' Expr
  //   VarDef ::= 'var' id '=' Expr
  // =========================================================

  val ValDef: Rule[ScalaTree] = rule:
    case (ScalaLexer.`val`(_), ScalaLexer.id(n), ScalaLexer.`=`(_), Expr(v)) =>
      ScalaTree.ValDef(n.value, v)

  val VarDef: Rule[ScalaTree] = rule:
    case (ScalaLexer.`var`(_), ScalaLexer.id(n), ScalaLexer.`=`(_), Expr(v)) =>
      ScalaTree.VarDef(n.value, v)

  // =========================================================
  // Method definition (Scala spec: DefDef, simplified)
  //
  //   DefDef ::= 'def' id '(' [Params] ')' ':' Type '=' Expr
  // =========================================================

  val DefDef: Rule[ScalaTree] = rule(
    {
      case (
            ScalaLexer.`def`(_),
            ScalaLexer.id(n),
            ScalaLexer.`\\(`(_),
            ScalaLexer.`\\)`(_),
            ScalaLexer.`:`(_),
            Type(ret),
            ScalaLexer.`=`(_),
            Expr(body),
          ) =>
        ScalaTree.DefDef(n.value, Nil, ret, body)
    },
    {
      case (
            ScalaLexer.`def`(_),
            ScalaLexer.id(n),
            ScalaLexer.`\\(`(_),
            Params(ps),
            ScalaLexer.`\\)`(_),
            ScalaLexer.`:`(_),
            Type(ret),
            ScalaLexer.`=`(_),
            Expr(body),
          ) =>
        ScalaTree.DefDef(n.value, ps, ret, body)
    },
  )

  // =========================================================
  // Class definition (Scala spec: ClassDef, simplified)
  //
  //   ClassDef ::= 'class' id ['(' Params ')'] ['extends' id] BlockExpr
  //
  // Expanded into four separate productions to avoid optional parts.
  // =========================================================

  val ClassDef: Rule[ScalaTree] = rule(
    "class0" { case (ScalaLexer.`class`(_), ScalaLexer.id(n), Expr(body)) =>
      ScalaTree.ClassDef(n.value, Nil, None, body)
    },
    "class1" {
      case (
            ScalaLexer.`class`(_),
            ScalaLexer.id(n),
            ScalaLexer.`\\(`(_),
            Params(ps),
            ScalaLexer.`\\)`(_),
            Expr(body),
          ) =>
        ScalaTree.ClassDef(n.value, ps, None, body)
    },
    "class2" {
      case (
            ScalaLexer.`class`(_),
            ScalaLexer.id(n),
            ScalaLexer.`extends`(_),
            ScalaLexer.id(p),
            Expr(body),
          ) =>
        ScalaTree.ClassDef(n.value, Nil, Some(p.value), body)
    },
    "class3" {
      case (
            ScalaLexer.`class`(_),
            ScalaLexer.id(n),
            ScalaLexer.`\\(`(_),
            Params(ps),
            ScalaLexer.`\\)`(_),
            ScalaLexer.`extends`(_),
            ScalaLexer.id(p),
            Expr(body),
          ) =>
        ScalaTree.ClassDef(n.value, ps, Some(p.value), body)
    },
  )

  // =========================================================
  // Parameters (Scala spec: Params, Param — simplified, no defaults)
  //
  //   Params ::= Param {',' Param}
  //   Param  ::= id ':' Type
  // =========================================================

  val Params: Rule[List[Param]] = rule(
    { case (ScalaLexer.id(n), ScalaLexer.`:`(_), Type(t)) => List(Param(n.value, t)) },
    { case (Params(ps), ScalaLexer.`,`(_), ScalaLexer.id(n), ScalaLexer.`:`(_), Type(t)) =>
      ps :+ Param(n.value, t)
    },
  )

  // =========================================================
  // Types (Scala spec: SimpleType — simplified)
  //
  //   Type  ::= id | id '[' Types ']'
  //   Types ::= Type {',' Type}
  //
  // Function types (`A => B`) are intentionally dropped — keeping them
  // would introduce an ambiguity with the case-clause arrow in patterns
  // and blow up the LALR state machine.
  // =========================================================

  val Type: Rule[ScalaType] = rule(
    { case ScalaLexer.id(n) => ScalaType.TypeRef(n.value) },
    { case (ScalaLexer.id(n), ScalaLexer.`\\[`(_), Types(ts), ScalaLexer.`\\]`(_)) =>
      ScalaType.AppliedType(n.value, ts)
    },
  )

  val Types: Rule[List[ScalaType]] = rule(
    { case Type(t) => List(t) },
    { case (Types(ts), ScalaLexer.`,`(_), Type(t)) => ts :+ t },
  )

  // =========================================================
  // Case clauses (Scala spec: CaseClauses, CaseClause, Guard)
  //
  //   CaseClauses ::= CaseClause {CaseClause}
  //   CaseClause  ::= 'case' Pattern [Guard] '=>' Expr
  //   Guard       ::= 'if' Expr
  //
  // Simplification vs. spec: case body is a single Expr (not a Block).
  // Wrap in `{}` for multi-statement bodies.
  // =========================================================

  val CaseClause: Rule[MatchCase] = rule(
    { case (ScalaLexer.`case`(_), Pattern(p), ScalaLexer.arrow(_), Expr(body)) =>
      MatchCase(p, None, body)
    },
    {
      case (
            ScalaLexer.`case`(_),
            Pattern(p),
            ScalaLexer.`if`(_),
            Expr(g),
            ScalaLexer.arrow(_),
            Expr(body),
          ) =>
        MatchCase(p, Some(g), body)
    },
  )

  /** Non-empty case-clause list (avoids the empty-block ambiguity with UnitBlock). */
  val CaseClauses: Rule[List[MatchCase]] = rule(
    { case CaseClause(c) => List(c) },
    { case (CaseClauses(cs), CaseClause(c)) => cs :+ c },
  )

  // =========================================================
  // Patterns (Scala spec: Pattern, SimplePattern — simplified)
  //
  //   Pattern       ::= Pattern1 {'|' Pattern1}
  //   Pattern1      ::= Pattern2 | Pattern2 ':' Type
  //   Pattern2      ::= SimplePattern | id '@' SimplePattern
  //   SimplePattern ::= '_' | Literal | id | id '(' Patterns ')'
  //   Patterns      ::= Pattern {',' Pattern}
  // =========================================================

  // Simplified to reduce LALR state-machine size:
  //   - no typed patterns (`p: T`) — would conflict with function-type arrow
  //   - no alt patterns (`a | b`) — saves states; use multiple case clauses
  //   - Pattern1/Pattern2 collapsed into Pattern
  //   - kept: wildcard, literals, variable, constructor, bind

  val Pattern: Rule[ScalaPattern] = rule(
    { case SimplePattern(p) => p },
    "bindpat" { case (ScalaLexer.id(n), ScalaLexer.`@`(_), SimplePattern(p)) =>
      ScalaPattern.BindPat(n.value, p)
    },
  )

  val SimplePattern: Rule[ScalaPattern] = rule(
    { case ScalaLexer.wildcard(_) => ScalaPattern.Wildcard },
    { case ScalaLexer.id(n) => ScalaPattern.VarPat(n.value) },
    { case ScalaLexer.intLit(n) => ScalaPattern.LitPat(ScalaTree.IntLit(n.value)) },
    { case ScalaLexer.floatLit(f) => ScalaPattern.LitPat(ScalaTree.FloatLit(f.value)) },
    { case ScalaLexer.`true`(_) => ScalaPattern.LitPat(ScalaTree.BoolLit(true)) },
    { case ScalaLexer.`false`(_) => ScalaPattern.LitPat(ScalaTree.BoolLit(false)) },
    { case ScalaLexer.`null`(_) => ScalaPattern.LitPat(ScalaTree.NullLit) },
    { case ScalaLexer.stringLit(s) => ScalaPattern.LitPat(ScalaTree.StringLit(s.value)) },
    "constrpat" { case (ScalaLexer.id(n), ScalaLexer.`\\(`(_), Patterns(ps), ScalaLexer.`\\)`(_)) =>
      ScalaPattern.ConstrPat(n.value, ps)
    },
    "constrpatempty" { case (ScalaLexer.id(n), ScalaLexer.`\\(`(_), ScalaLexer.`\\)`(_)) =>
      ScalaPattern.ConstrPat(n.value, Nil)
    },
  )

  val Patterns: Rule[List[ScalaPattern]] = rule(
    { case Pattern(p) => List(p) },
    { case (Patterns(ps), ScalaLexer.`,`(_), Pattern(p)) => ps :+ p },
  )

  // =========================================================
  // Top-level rule
  // =========================================================

  val root: Rule[ScalaTree] = rule:
    case Expr(e) => e

  // =========================================================
  // Conflict resolutions for operator precedence
  //
  // `X.after(Y)` means "don't reduce X when seeing Y" (Y binds tighter).
  // `X.before(Y)` means "reduce X before shifting Y" (X binds tighter).
  // =========================================================

  override val resolutions: Set[ConflictResolution] = Set(
    // Select '.' and Apply '(' bind tightest
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

    // Unary '-' and '!' bind tighter than binary operators
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

    // '*' '/' '%' higher precedence than '+' '-'
    production.plus.after(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.minus.after(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.plus.before(ScalaLexer.`\\+`, ScalaLexer.`-`),
    production.minus.before(ScalaLexer.`\\+`, ScalaLexer.`-`),
    production.times.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.divide.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.mod.before(ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),

    // '+' '-' higher precedence than '<' '>' '<=' '>='
    production.lt.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.gt.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.lte.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.gte.after(ScalaLexer.`\\+`, ScalaLexer.`-`, ScalaLexer.`\\*`, ScalaLexer.`/`, ScalaLexer.`%`),
    production.lt.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.gt.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.lte.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.gte.before(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),

    // '<' '>' '<=' '>=' higher precedence than '==' '!='
    production.eq.after(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.neq.after(ScalaLexer.`<`, ScalaLexer.`>`, ScalaLexer.lte, ScalaLexer.gte),
    production.eq.before(ScalaLexer.eqeq, ScalaLexer.neq),
    production.neq.before(ScalaLexer.eqeq, ScalaLexer.neq),

    // '==' '!=' higher precedence than '&&'
    production.and.after(ScalaLexer.eqeq, ScalaLexer.neq),
    production.and.before(ScalaLexer.and),

    // '&&' higher precedence than '||'
    production.or.after(ScalaLexer.and),
    production.or.before(ScalaLexer.or),

    // if-else: else-branch is greedy
    production.ifelse.after(
      ScalaLexer.`\\+`,
      ScalaLexer.`-`,
      ScalaLexer.`\\*`,
      ScalaLexer.`/`,
      ScalaLexer.`%`,
      ScalaLexer.`<`,
      ScalaLexer.`>`,
      ScalaLexer.lte,
      ScalaLexer.gte,
      ScalaLexer.eqeq,
      ScalaLexer.neq,
      ScalaLexer.and,
      ScalaLexer.or,
    ),
  )
