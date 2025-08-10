package alpaca

import scala.annotation.tailrec
import scala.collection.mutable
import scala.quoted.*

class Token[Name <: String](
  using Ctx
)(
  val value: Any | Null,
  val tpe: Name | Null,
  val lineno: Int = ctx.lineno,
  val index: Int = ctx.index,
  val end: Int = ctx.index,
)

object Token {
  val Ignored: Ctx ?=> Token[?] = new Token[Null](null, null)

  inline def apply[Name <: String: ValueOf](using Ctx): Token[Name] = new Token[Name](null, valueOf[Name])
  inline def apply[Name <: String: ValueOf](value: Any)(using Ctx): Token[Name] = new Token[Name](value, valueOf[Name])
}

case class TokenizationResult(tokens: List[Token[?]], errors: List[String])(using val ctx: Ctx) {
  export ctx.*
}

trait Tokenize {
  val ignorePatterns: Set[String]
  val patterns: Map[String, String] // should be some lambda
  def tokenize(s: String): TokenizationResult = ???
}

type LexerDefinition = PartialFunction[String, Token[?]]
inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenize = ${ lexerImpl('{ rules }) }

private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenize] = {
  import quotes.reflect.*

  type Result = (ignorePatterns: Set[String], mapping: mutable.SortedMap[String, String])

  val (ignoredPatterns, mapping) = rules.asTerm.underlying match
    case Lambda(_ctx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) =>
      cases.foldLeft((Set.empty[String], mutable.SortedMap.empty[String, String])) {
        case ((ignores, mapping), CaseDef(pattern, guard, body)) =>
          def withPattern(name: String, value: String): Result =
            if mapping contains name
            then report.errorAndAbort(s"Duplicate token type: $name")
            else (ignores, mapping + (name -> value))

          @tailrec def extract(body: Term): Result = body.asExpr match
            case '{ Token.Ignored.apply(using $ctx) } =>
              (ignores + pattern.show, mapping)
            case '{
                  type t <: String
                  Token.apply[t](using $valueOf: ValueOf[t], $ctx: Ctx)
                } =>
              withPattern(Type.show[t], pattern.show) // should be some lambda
            case '{
                  type t <: String
                  Token.apply[t]($value: v)(using $valueOf: ValueOf[t], $ctx: Ctx)
                } =>
              withPattern(Type.show[t], value.show)// should be some lambda
            case _ =>
              body match
                case Block(statements, expr) =>
                  //do not forget about putting statements in remapping
                  extract(expr)
                case x =>
                  treeInfo(x).dbg

          extract(body)
      }
    case _ =>
      report.errorAndAbort("Lexer definition must be a lambda function")

  '{
    new Tokenize {
      val ignorePatterns: Set[String] = ${ Expr(ignoredPatterns) }
      val patterns: Map[String, String] = ${ Expr(mapping.toMap) }
    }
  }
}

trait Ctx {
  var text: String
  var index: Int
  var lineno: Int
}

inline given ctx: Ctx = compiletime.summonInline[Ctx]
