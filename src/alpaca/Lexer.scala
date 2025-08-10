package alpaca

import scala.annotation.{compileTimeOnly, tailrec}
import scala.collection.SortedSet
import scala.quoted.*

class Lexem[Name <: String](
  using Ctx
)(
  val tpe: Name | Null,
  val value: Any | Null,
  val lineno: Int = ctx.lineno,
  val index: Int = ctx.index,
  val end: Int = ctx.index,
)
sealed trait Token[Name <: String](val pattern: String, val ctxManipulation: Ctx ?=> Unit | Null)

private class TokenImpl[Name <: String](
  val tpe: Name | Null,
  pattern: String,
  ctxManipulation: Ctx ?=> Unit | Null = null,
  val remapping: Expr[String => Any] | Null = null,
) extends Token[Name](pattern, ctxManipulation) {
  val index: Int = TokenImpl.nextIndex()
}

private object TokenImpl {
  private var index = 0

  private def nextIndex(): Int = {
    index += 1
    index
  }
}

class IgnoredToken(pattern: String, ctxManipulation: Ctx ?=> Unit | Null = null)
  extends Token[Nothing](pattern, ctxManipulation)

given Ordering[Token[?]] = {
  case (x: IgnoredToken, y: TokenImpl[?]) => -1 // Ignored tokens are always less than any other token
  case (x: TokenImpl[?], y: IgnoredToken) => 1
  case (x: TokenImpl[?], y: TokenImpl[?]) => x.index.compareTo(y.index)
  case (x: IgnoredToken, y: IgnoredToken) => x.pattern.compareTo(y.pattern)
}

object Token {
  @compileTimeOnly("Should never be called outside the lexer definition")
  val Ignored: Ctx ?=> Token[?] = ???
  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ConstString](using Ctx): Token[Name] = ???

  @compileTimeOnly("Should never be called outside the lexer definition")
  def apply[Name <: ConstString](value: Any)(using Ctx): Token[Name] = ???
}

case class TokenizationResult(lexems: List[Lexem[?]], errors: List[String])(using val ctx: Ctx) {
  export ctx.*
}

trait Tokenize {
  def tokenize(s: String): TokenizationResult = ???
}

type LexerDefinition = PartialFunction[String, Token[?]]

type ConstString = String & Singleton
inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenize = ${ lexerImpl('{ rules }) }

private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenize] = {
  import quotes.reflect.*
//  return '{???}
  rules.asTerm.underlying match
    case Lambda(_ctx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) =>
      cases.foldLeft(SortedSet.empty[Token[?]]) { case (tokens, CaseDef(pattern, guard, body)) =>
        def treeToPattern(tree: Tree): String = tree match
          case Bind(_, Literal(StringConstant(str))) => str
          case Bind(_, alternatives: Alternatives) => treeToPattern(alternatives)
          case Literal(StringConstant(str)) => str
          case Alternatives(alternatives) => alternatives.map(treeToPattern).mkString("|")
          case x => treeInfo(x).dbg

        def withToken(token: Token[?]) =
          if tokens contains token // should be better check (for regex overlapping)
          then report.errorAndAbort(s"Duplicate token type: $token")
          else tokens + token

        @tailrec def extract(body: Expr[?]): SortedSet[Token[?]] = body match
          case '{ Token.Ignored.apply(using $ctx) } =>
            withToken(new IgnoredToken(treeToPattern(pattern)))
          case '{ type t <: ConstString; Token.apply[t](using $ctx: Ctx) } =>
            withToken(new TokenImpl(Type.show[t], treeToPattern(pattern)))
          case '{ type t <: ConstString; Token.apply[t]($value: v)(using $ctx: Ctx) } =>
            // todo remapping
            withToken(new TokenImpl(Type.show[t], treeToPattern(pattern)))
          case ExprBlock(statements, expr) =>
            statements.map(_.asTerm).map {
              case Block(ValDef(_, _, _) :: Nil, Closure(meth, _)) =>
                ???
              case x =>
                treeInfo(x).dbg
            }
            extract(expr)
          case x =>
            s"""Unsupported tree:
               |treeInfo(x.asTerm)""".dbg

        extract(body.asExpr)
      }
    case _ =>
      report.errorAndAbort("Lexer definition must be a lambda function")

  '{
    new Tokenize {}
  }
}

trait Ctx {
  var text: String
  var index: Int
  var lineno: Int
}

inline given ctx: Ctx = compiletime.summonInline[Ctx]
