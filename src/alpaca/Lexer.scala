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
sealed trait Token[Name <: String](val pattern: String, val ctxManipulation: (Ctx => Unit) | Null)

private class TokenImpl[Name <: String](
  val tpe: Name | Null,
  pattern: String,
  ctxManipulation: (Ctx => Unit) | Null = null,
  val remapping: (String => Any) | Null = null,
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

class IgnoredToken(pattern: String, ctxManipulation: (Ctx => Unit) | Null = null)
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
  val tokens: SortedSet[Token[?]]
  def tokenize(s: String): TokenizationResult = ???
}

type LexerDefinition = PartialFunction[String, Token[?]]

type ConstString = String & Singleton
inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenize = ${ lexerImpl('{ rules }) }

private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenize] = {
  import quotes.reflect.*
//  return '{???}
  def treeToPattern(tree: Tree): Expr[String] = tree match
    case Bind(_, Literal(StringConstant(str))) => Expr(str)
    case Bind(_, alternatives: Alternatives) => treeToPattern(alternatives)
    case Literal(StringConstant(str)) => Expr(str)
    case Alternatives(alternatives) => Expr(alternatives.map(treeToPattern).mkString("|"))
    case x =>
      s"""treeToPattern unsupported ${treeInfo(x)}""".dbg

  final class ReplaceRef(queries: (find: Term, replace: Term)*) extends TreeMap {
    override def transformTerm(tree: Term)(owner: Symbol): Term =
      queries.indexWhere(_.find.symbol == tree.symbol) match
        case -1 => super.transformTerm(tree)(owner)
        case idx => queries(idx).replace
  }

  val res = rules.asTerm.underlying match
    case Lambda(ctx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) =>
      cases.foldLeft('{ SortedSet.empty[Token[?]] }) { case (tokens, CaseDef(pattern, guard, body)) =>
        treeInfo(pattern).dbg

        def withToken(token: Expr[Token[?]]) = '{
          if $tokens contains $token
            // todo: make it compile-time error
            // should be better check (for regex overlapping)
          then throw Exception(s"Duplicate token type: $$token")
          else $tokens + $token
        }

        @tailrec def extract(
          body: Expr[?]
        )(
          ctxManipulation: Expr[(Ctx => Unit) | Null] = '{ null }
        ): Expr[SortedSet[Token[?]]] = body match
          case '{ Token.Ignored.apply(using $ctx) } =>
            val regex = treeToPattern(pattern)
            withToken('{ new IgnoredToken($regex, $ctxManipulation) })
          case '{ type t <: ConstString; Token.apply[t](using $ctx: Ctx) } =>
            val name = Expr(Type.show[t])
            val regex = treeToPattern(pattern)
            withToken('{ new TokenImpl($name, $regex, $ctxManipulation) })
          case '{ type t <: ConstString; Token.apply[t]($value: v)(using $ctx: Ctx) } =>
//            ReplaceRef(find = pattern, replace = Ref(ctx.symbol)).transformTree(value.asTerm)(Symbol.spliceOwner)
            val name = Expr(Type.show[t])
            val regex = treeToPattern(pattern)
            withToken('{ new TokenImpl($name, $regex, $ctxManipulation) })
          case ExprBlock(statements, expr) =>
            val ctxManipulation = Lambda(
              Symbol.spliceOwner,
              MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Unit]),
              {
                case (methSym, ctx :: Nil) =>
                  Block(
                    statements.map(_.asTerm).map {
                      case Block(ValDef(_, _, Some(rhs)) :: Nil, x) if rhs.symbol == ctx.symbol =>
                        ReplaceRef(
                          (find = rhs, replace = Ref(ctx.symbol)),
                          // todo replace x also
                        ).transformTerm(x)(Symbol.spliceOwner)
                      case x => x
                    },
                    Literal(UnitConstant()),
                  )
                case _ => report.errorAndAbort("Invalid number of parameters in lambda")
              },
            ).asExprOf[Ctx => Unit]
            extract(expr)(ctxManipulation)
          case x =>
            s"""Unsupported tree:
               |treeInfo(x.asTerm)""".dbg

        extract(body.asExpr)()
      }
    case _ =>
      report.errorAndAbort("Lexer definition must be a lambda function")

  '{
    new Tokenize {
      val tokens = $res
    }
  }
}

trait Ctx {
  var text: String
  var index: Int
  var lineno: Int
}

inline given ctx: Ctx = compiletime.summonInline[Ctx]
