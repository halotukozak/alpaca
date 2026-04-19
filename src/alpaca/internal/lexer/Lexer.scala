package alpaca
package internal
package lexer

import alpaca.Token as TokenDef

import java.util.regex.{Pattern, PatternSyntaxException}
import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.{publicInBinary, switch}
import scala.reflect.NameTransformer

// $COVERAGE-OFF$
def lexerImpl[Ctx <: LexerCtx: Type, lexemeFields <: AnyNamedTuple: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  betweenStages: Expr[OnTokenMatch[Ctx]],
  errorHandling: Expr[ErrorHandling[Ctx]],
  empty: Expr[Empty[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeFields = lexemeFields }] = withLog:
  timeoutOnTooLongCompilation()

  import quotes.reflect.*

  type TokenRefn = Token[?, Ctx, ?] { type LexemeTpe = Lexeme[?, ?] withFields lexemeFields }

  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val replaceRefs = new ReplaceRefs[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked

  if cases.isEmpty then report.errorAndAbort("Lexer definition must contain at least one case")

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
          compileNameAndPattern[name](tree).map:
            case ('[type name <: ValidName; name], tokenInfo) =>
              // we need to widen here to avoid weird types
              TypeRepr.of[value].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  (tokenInfo, '{ DefinedToken[name, Ctx, result](${ Expr(tokenInfo) }, $ctxManipulation, $remapping) })
            case (_, tokenInfo) =>
              raiseShouldNeverBeCalled[(TokenInfo, Expr[Token[?, Ctx, ?]])](tokenInfo)

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
      RegexChecker.checkPatterns(patterns)
      RegexChecker.checkPatterns(patterns.reverse)

      (
        tokens = accTokens ::: tokens.map:
          case '{ type name <: ValidName; type tokenTpe <: Token[name, Ctx, ?]; $token: tokenTpe } =>
            (expr = '{ $token.asInstanceOf[tokenTpe & TokenRefn] }, name = ValidName.from[name])
        ,
        infos = accInfos ::: infos,
      )

    case (_, CaseDef(_, Some(_), body)) => report.errorAndAbort("Guards are not supported yet")

  logger.trace("checking for duplicate token names")
  infos
    .groupBy(_.name)
    .iterator
    .filter(_._2.sizeIs > 1)
    .foreach: (name, duplicates) =>
      report.errorAndAbort(
        show"Token name \"$name\" is defined ${duplicates.size.toString} times. Combine the patterns into a single case using alternatives, e.g.: case x @ (\"pattern1\" | \"pattern2\") => Token[x]",
      )

  logger.trace("checking regex patterns")
  RegexChecker.checkPatterns(infos.map(_.pattern))

  val fields = tokens.map((expr, name) => (name, expr.asTerm.tpe))
  val types = Refined(
    TypeTree.of[Any],
    fields.map: (name, tpe) =>
      TypeDef(Symbol.newTypeAlias(Symbol.spliceOwner, name, Flags.EmptyFlags, tpe, Symbol.noSymbol)),
    defn.AnyClass,
  ).tpe

  def selectDynamicImpl(fieldName: Expr[String])(using Quotes) = Match(
    '{ $fieldName: @switch }.asTerm,
    tokens.map: (expr, name) =>
      CaseDef(Literal(StringConstant(NameTransformer.encode(name))), None, expr.asTerm),
  ).asExprOf[Token[?, Ctx, ?]]

  logger.trace("creating tokenization class instance")
  (refinementTpeFrom(fields).asType, fieldsTpeFrom(fields).asType, types.asType).runtimeChecked match
    case ('[refinedTpe], '[fields], '[types]) =>
      val tokensExpr = Expr.ofList(tokens.map(_.expr))
      infos.foreach: info =>
        try Pattern.compile(info.pattern)
        catch
          case e: PatternSyntaxException =>
            report.errorAndAbort(
              show"Invalid regex pattern \"${info.pattern}\" for token \"${info.name}\": ${e.getDescription}. If you meant to match a literal character, escape it with a backslash (e.g., \"\\\\+\" instead of \"+\")",
            )

      val regex = Expr:
        infos
          .map:
            case TokenInfo(_, regexGroupName, pattern) => show"(?<$regexGroupName>$pattern)"
          .mkString("|")
          .tap(Pattern.compile) // we'd like to compile it here to fail in compile time if regex is invalid

      '{
        {
          new Tokenization[Ctx](using $betweenStages, $errorHandling, $empty):
            @publicInBinary
            override private[alpaca] val tokens: List[Token[?, Ctx, ?]] = $tokensExpr

            override def selectDynamic(name: String): Token[?, Ctx, ?] = ${ selectDynamicImpl('{ name }) }

            override protected val compiled: java.util.regex.Pattern = Pattern.compile($regex)
        }.asInstanceOf[Tokenization[Ctx] { type LexemeFields = lexemeFields; type Fields = fields } & refinedTpe & types]
      }
// $COVERAGE-ON$
