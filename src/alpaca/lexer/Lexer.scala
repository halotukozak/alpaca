package alpaca.lexer

import alpaca.*

import scala.annotation.tailrec
import scala.collection.{immutable, SortedSet}
import scala.quoted.*
import scala.util.Random

type LexerDefinition = PartialFunction[String, Token[?]]
type ConstString = String & Singleton

inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenization = ${ lexerImpl('{ rules }) }

private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenization] = {
  import quotes.reflect.*

  final class ReplaceRef(queries: (find: Symbol, replace: Term)*) extends TreeMap {
    // skip NoSymbol
    private val filtered = queries.view.filterNot(_.find.isNoSymbol)

    override def transformTerm(tree: Term)(owner: Symbol): Term =
      filtered
        .collectFirst { case (find, replace) if find == tree.symbol => replace }
        .getOrElse(super.transformTerm(tree)(owner))
  }

  val res = rules.asTerm.underlying match
    case Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) =>
      cases.foldLeft('{ immutable.SortedSet.empty[Token[?]] }) {
        case (tokens, CaseDef(pattern, None, body)) =>
          // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
          def withToken(token: Expr[Token[?]]) = '{ $tokens + $token }

          def compiledPattern(pattern: Tree): Expr[String] = pattern match
            case Bind(_, Literal(StringConstant(str))) => Expr(str)
            case Bind(_, alternatives: Alternatives) => compiledPattern(alternatives)
            case Literal(StringConstant(str)) => Expr(str)
            case Alternatives(alternatives) => Expr(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("|"))
            case x =>
              s"""compiledPattern unsupported ${treeInfo(x)}""".dbg

          def compiledName[T: Type](pattern: Tree): Expr[String] = TypeRepr.of[T] match
            case ConstantType(StringConstant(str)) => Expr(str)
            case _ =>
              def loop(pattern: Tree): Expr[String] = pattern match
                case Bind(_, Literal(StringConstant(str))) => Expr(str)
                case Bind(_, alternatives: Alternatives) => compiledPattern(alternatives)
                case Literal(StringConstant(str)) => Expr(str)
                case Alternatives(alternatives) =>
                  Expr(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("_or_"))
                case x =>
                  s"""compiledPattern unsupported ${treeInfo(x)}""".dbg
              loop(pattern)

          @tailrec def extract(body: Expr[?])(ctxManipulation: Expr[Ctx => Ctx]): Expr[immutable.SortedSet[Token[?]]] =
            body match
              case '{ Token.Ignored.apply(using $ctx) } =>
                val regex = compiledPattern(pattern)
                withToken('{
                  val tempName = TempRandom.alpha().take(8).mkString // todo:  better name required
                  IgnoredToken(tempName, $regex, $ctxManipulation)
                })
              case '{ type t <: ConstString; Token.apply[t](using $ctx: Ctx) } =>
                val name = compiledName[t](pattern)
                val regex = compiledPattern(pattern)
                withToken('{ TokenImpl($name, $regex, $ctxManipulation) })
              case '{ type t <: ConstString; Token.apply[t]($value: v)(using $ctx: Ctx) } =>
                val name = compiledName[t](pattern)
                val regex = compiledPattern(pattern)
                val remapping = Lambda(
                  Symbol.spliceOwner,
                  MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Any]),
                  {
                    case (methSym, (newCtx: Term) :: Nil) =>
                      ReplaceRef(
                        (find = oldCtx.symbol, replace = newCtx),
                        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
                      ).transformTerm(value.asTerm)(methSym)
                    case _ => report.errorAndAbort("Invalid number of parameters in lambda")
                  },
                ).asExprOf[Ctx => Any]
                withToken('{ TokenImpl($name, $regex, $ctxManipulation, Some($remapping)) })
              case complex =>
                complex.asTerm match
                  case Block(statements, expr) =>
                    val ctxManipulation = Lambda(
                      Symbol.spliceOwner,
                      MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Ctx]),
                      {
                        case (methSym, (newCtx: Term) :: Nil) =>
                          ReplaceRef(
                            (find = oldCtx.symbol, replace = newCtx),
                            (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
                          ).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
                        case _ => report.errorAndAbort("Invalid number of parameters in lambda")
                      },
                    ).asExprOf[Ctx => Ctx]

                    extract(expr.asExpr)(ctxManipulation)
                  case x =>
                    s"""Unsupported tree:
                   |${treeInfo(x)}""".dbg

          extract(body.asExpr)('{ _.copy() })
        case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
      }
    case _ =>
      report.errorAndAbort("Lexer definition must be a lambda function")

  '{
    new Tokenization {
      val tokens: SortedSet[Token[?]] = $res
    }
  }
}

//tempporary solution
object TempRandom {
  def alpha(): LazyList[Char] = {
    def nextAlphaNum: Char = {
      val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
      chars.charAt(Random.nextInt(chars.length))
    }

    LazyList.continually(nextAlphaNum)
  }
}
