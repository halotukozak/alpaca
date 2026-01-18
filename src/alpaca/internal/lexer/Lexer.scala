package alpaca
package internal
package lexer

import alpaca.Token as TokenDef

import java.util.regex.Pattern
import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.switch
import scala.reflect.NameTransformer
import ox.*

def lexerImpl[Ctx <: LexerCtx: Type, lexemeFields <: AnyNamedTuple: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeFields = lexemeFields }] = withTimeout:
  import quotes.reflect.*
  type TokenRefn = Token[?, Ctx, ?] { type LexemeTpe = Lexeme[?, ?] withFields lexemeFields }

  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val withOverridingSymbol = new WithOverridingSymbol[quotes.type]
  val replaceRefs = new ReplaceRefs[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val (tokens, infos) = cases.foldLeft(
    (
      tokens = List.empty[(expr: Expr[Token[?, Ctx, ?] & TokenRefn], name: ValidName)],
      infos = List.empty[TokenInfo],
    ),
  ):
    case ((accTokens, accInfos), CaseDef(tree, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = replaceRefs(
        (find = oldCtx.symbol, replace = newCtx),
        (find = tree.symbol, replace = Select.unique(newCtx, "lastRawMatched")),
      )

      def extractSimple(ctxManipulation: Expr[CtxManipulation[Ctx]])
        : PartialFunction[Expr[TokenDef[ValidName, Ctx, Any]], List[(TokenInfo, Expr[Token[?, Ctx, ?]])]] =
        case '{ Token.Ignored(using $_) } =>
          logger.trace("extractSimple(1)")
          compileNameAndPattern[Nothing](tree).unsafeMap:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (tokenInfo, '{ IgnoredToken[name, Ctx](${ Expr(tokenInfo) }, $ctxManipulation) })

        case '{ type name <: ValidName; Token[name](using $_) } =>
          logger.trace("extractSimple(2)")
          compileNameAndPattern[name](tree).unsafeMap:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (tokenInfo, '{ DefinedToken[name, Ctx, Unit](${ Expr(tokenInfo) }, $ctxManipulation, _ => ()) })

        case '{ type name <: ValidName; Token[name]($value: String)(using $_) } if value.asTerm.symbol == tree.symbol =>
          logger.trace("extractSimple(3)")
          compileNameAndPattern[name](tree).unsafeMap:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (tokenInfo, '{ DefinedToken[name, Ctx, String](${ Expr(tokenInfo) }, $ctxManipulation, _.lastRawMatched) })

        case '{ type name <: ValidName; Token[name]($value: value)(using $_) } =>
          logger.trace("extractSimple(4)")
          compileNameAndPattern[name](tree).unsafeMap:
            case ('[type name <: ValidName; name], tokenInfo) =>
              // we need to widen here to avoid weird types
              TypeRepr.of[value].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  (tokenInfo, '{ DefinedToken[name, Ctx, result](${ Expr(tokenInfo) }, $ctxManipulation, $remapping) })

      logger.trace("extracting tokens from body")
      val (infos, tokens) = extractSimple('{ _ => () })
        .lift(body.asExprOf[TokenDef[ValidName, Ctx, Any]])
        .orElse:
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(
                    Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                  )(methSym)

              extractSimple(ctxManipulation).lift(expr.asExprOf[TokenDef[ValidName, Ctx, Any]])
        .getOrElse(raiseShouldNeverBeCalled[List[(TokenInfo, Expr[Token[?, Ctx, ?]])]](body))
        .unzip

      val patterns = infos.map(_.pattern)
      RegexChecker.checkPatterns(patterns).foreach(report.errorAndAbort)
      RegexChecker.checkPatterns(patterns.reverse).foreach(report.errorAndAbort)
      par(RegexChecker.checkPatterns(patterns), RegexChecker.checkPatterns(patterns.reverse))

      (
        tokens = accTokens ::: tokens.map:
          case '{ type name <: ValidName; type tokenTpe <: Token[name, Ctx, ?]; $token: tokenTpe } =>
            (expr = '{ $token.asInstanceOf[tokenTpe & TokenRefn] }, name = ValidName.from[name]),
        infos = accInfos ::: infos,
      )

    case (_, CaseDef(_, Some(_), body)) => report.errorAndAbort("Guards are not supported yet")

  logger.trace("partitioning defined and ignored tokens")
  val (definedTokens, ignoredTokens) = tokens.partition(_.expr.isExprOf[DefinedToken[?, Ctx, ?]])

  logger.trace("checking regex patterns")
  RegexChecker.checkPatterns(infos.map(_.pattern)).foreach(report.errorAndAbort)

  val fields = definedTokens.map((expr, name) => (name, expr.asTerm.tpe))

  val selectDynamicLambda = createLambda[String => DefinedToken[?, Ctx, ?]]:
    case (methSym, List(fieldName: Term)) =>
      Match(
        Typed(
          fieldName,
          Annotated(
            TypeTree.ref(fieldName.tpe.typeSymbol),
            '{ new annotation.switch }.asTerm.changeOwner(methSym),
          ),
        ),
        definedTokens.collect:
          case (expr, name) if expr.asTerm.tpe <:< TypeRepr.of[DefinedToken[?, ?, ?]] =>
            CaseDef(Literal(StringConstant(NameTransformer.encode(name))), None, expr.asTerm),
      ).changeOwner(methSym)

  logger.trace("creating tokenization class instance")
  (refinementTpeFrom(fields).asType, fieldsTpeFrom(fields).asType).runtimeChecked match
    case ('[refinedTpe], '[fields]) =>

      val tokensExpr = Expr.ofList(tokens.map(_.expr))

      val regex = Expr(
        infos
          .map:
            case TokenInfo(_, regexGroupName, pattern) => show"(?<$regexGroupName>$pattern)"
          .mkString("|")
          .tap(Pattern.compile), // we'd like to compile it here to fail in compile time if regex is invalid
      )

      '{
        {
          new Tokenization[Ctx](using $betweenStages):
            override val tokens: List[Token[?, Ctx, ?]] = $tokensExpr

            override def selectDynamic(name: String): DefinedToken[?, Ctx, ?] = $selectDynamicLambda(name)

            override protected val compiled: java.util.regex.Pattern = Pattern.compile($regex)
        }.asInstanceOf[Tokenization[Ctx] { type LexemeFields = lexemeFields; type Fields = fields } & refinedTpe]
      }
