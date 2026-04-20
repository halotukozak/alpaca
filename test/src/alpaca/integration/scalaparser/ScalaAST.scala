package alpaca
package integration.scalaparser

/**
 * AST for a subset of Scala expressions.
 *
 * Follows the Scala 3 language specification grammar:
 * https://docs.scala-lang.org/scala3/reference/syntax.html
 *
 * Productions covered:
 *   Literal   ::=  integerLiteral | floatingPointLiteral
 *              |   booleanLiteral | characterLiteral | stringLiteral | 'null'
 *   SimpleRef ::=  id
 *   SimpleExpr::=  SimpleRef | Literal | BlockExpr | '(' Expr ')'
 *              |   SimpleExpr '.' id         (Select)
 *              |   SimpleExpr ArgumentExprs  (Apply)
 *   InfixExpr ::=  PrefixExpr | InfixExpr op InfixExpr
 *   PrefixExpr::=  ['-' | '!'] SimpleExpr
 *   Expr1     ::=  'if' '(' Expr ')' Expr 'else' Expr
 *   BlockExpr ::=  '{' '}' | '{' Block '}'
 *   Block     ::=  {BlockStat ';'} Expr
 *   BlockStat ::=  Def | Expr
 *   ValDef    ::=  'val' id '=' Expr
 */
enum ScalaTree:
  // Literals (Scala 3 spec: Literal)
  case IntLit(value: Long)
  case FloatLit(value: Double)
  case BoolLit(value: Boolean)
  case StringLit(value: String)
  case NullLit

  // Simple references (Scala 3 spec: SimpleRef)
  case Ident(name: String)

  // Expressions (Scala 3 spec: InfixExpr, PrefixExpr, SimpleExpr)
  case Select(qual: ScalaTree, name: String) // SimpleExpr '.' id
  case Apply(fun: ScalaTree, args: List[ScalaTree]) // SimpleExpr ArgumentExprs
  case Prefix(op: String, operand: ScalaTree) // PrefixExpr
  case Infix(left: ScalaTree, op: String, right: ScalaTree) // InfixExpr

  // Expr1: if-then-else
  case If(cond: ScalaTree, thenBranch: ScalaTree, elseBranch: ScalaTree)

  // BlockExpr / Block
  case Block(stmts: List[ScalaTree], result: ScalaTree)
  case UnitBlock // empty block: {}

  // Block statement: val definition (Scala 3 spec: ValDef)
  case ValDef(name: String, value: ScalaTree)
