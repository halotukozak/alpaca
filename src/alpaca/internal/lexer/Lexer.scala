package alpaca
package internal
package lexer

import alpaca.Token as TokenDef

import java.util.regex.{Pattern, PatternSyntaxException}
import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.switch
import scala.reflect.{NameTransformer, Typeable}
import scala.util.matching.Regex

/**
 * Type alias for lexer rule definitions.
 *
 * A lexer definition is a partial function that maps string patterns
 * (as regex literals) to token definitions.
 *
 * @tparam Ctx the global context type
 */
private[alpaca] type LexerDefinition[Ctx <: LexerCtx] = PartialFunction[String, Token[?, Ctx, ?]]

//todo: private[alpaca]
def lexerImpl[Ctx <: LexerCtx: Type, LexemeRefn: Type, lexemeFields <: AnyNamedTuple: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
  errorHandling: Expr[ErrorHandling[Ctx]],
  empty: Expr[Empty[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeRefinement = LexemeRefn; type LexemeFields = lexemeFields }] = withDebugSettings:
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
  errorHandling: Expr[ErrorHandling[Ctx]],
  empty: Expr[Empty[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeFields = lexemeFields }] = withLog:
  timeoutOnTooLongCompilation()

  import quotes.reflect.*
type ThisToken = Token[?, Ctx, ?]
type TokenRefn = ThisToken { type LexemeTpe = LexemeRefn }

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

def extractSimple(
  ctxManipulation: Expr[CtxManipulation[Ctx]],
): PartialFunction[Expr[TokenDefinition[ValidName, Ctx, Any]], List[(TokenInfo, Expr[Token[?, Ctx, ?]])]] =
  case '{ Token.Ignored(using $ctx) } =>
    compileNameAndPattern[Nothing](tree).unsafeMap:
      case ('[type name <: ValidName; name], tokenInfo) =>
        (tokenInfo, '{ IgnoredToken[name, Ctx](${ Expr(tokenInfo) }, $ctxManipulation) })

case '{ type name <: ValidName; Token.apply[name](using $ctx) } =>
  compileNameAndPattern[name](tree).map:
    case '{ $tokenInfo: TokenInfo[name] } =>
      '{ DefinedToken[name, Ctx, Unit]($tokenInfo, $ctxManipulation, _ => ()) }

case '{ type name <: ValidName; Token.apply[name]($value: String)(using $ctx) }
    if value.asTerm.symbol == tree.symbol =>
  compileNameAndPattern[name](tree).unsafeMap:
    case ('[type name <: ValidName; name], tokenInfo) =>
      (tokenInfo, '{ DefinedToken[name, Ctx, String](${ Expr(tokenInfo) }, $ctxManipulation, _.lastRawMatched) })
case '{ type name <: ValidName; Token.apply[name]($value: value)(using $ctx) } =>
  compileNameAndPattern[name](tree).map:
    case ('[type name <: ValidName; name], tokenInfo) =>
      TypeRepr.of[value].widen.asType match
        case '[result] =>
          val remapping = createLambda[Ctx => result]:
            case (methSym, (newCtx: Term) :: Nil) =>
              replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
          (tokenInfo, '{ DefinedToken[name, Ctx, result](${ Expr(tokenInfo) }, $ctxManipulation, $remapping) })
              // we need to widen here to avoid weird types
              TypeRepr.of[value].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  (tokenInfo, '{ DefinedToken[name, Ctx, result](${ Expr(tokenInfo) }, $ctxManipulation, $remapping) })
            case (_, tokenInfo) =>
              raiseShouldNeverBeCalled[(TokenInfo, Expr[Token[?, Ctx, ?]])](tokenInfo)

val tokens = extractSimple('{ _ => () })
  .lift(body.asExprOf[TokenDefinition[ValidName, Ctx, Any]])
        .orElse:
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(
                    Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                  )(methSym)

extractSimple(ctxManipulation)
  .lift(expr.asExprOf[TokenDefinition[ValidName, Ctx, Any]])
.getOrElse(raiseShouldNeverBeCalled[List[(TokenInfo, Expr[Token[?, Ctx, ?]])]](body))
.unzip

      val patterns = infos.map(_.pattern)
      RegexChecker.checkPatterns(patterns)
      RegexChecker.checkPatterns(patterns.reverse)

val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?, Ctx, ?]])

RegexChecker.checkPatterns(infos.map(_.pattern)).foreach(report.errorAndAbort)

logger.trace("checking for duplicate token names")
infos
  .groupBy(_.name)
  .iterator
  .filter(_._2.sizeIs > 1)
  .foreach: (name, duplicates) =>
    report.errorAndAbort(
      show"Token name \"$name\" is defined ${duplicates.size.toString} times. Combine the patterns into a single case using alternatives, e.g.: case x @ (\"pattern1\" | \"pattern2\") => Token[x]",
    )

    case (_, CaseDef(_, Some(_), body)) => report.errorAndAbort("Guards are not supported yet")

def compiledSymbol(cls: Symbol) = Symbol.newVal(
  parent = cls,
  name = "compiled",
  tpe = TypeRepr.of[Regex],
  flags = Flags.Protected | Flags.Synthetic | Flags.Override,
  privateWithin = Symbol.noSymbol,
)

def tokensSymbol(cls: Symbol) = Symbol.newVal(
  parent = cls,
  name = "tokens",
  tpe = TypeRepr.of[List[Token[?, Ctx, ?]]],
  flags = Flags.Synthetic | Flags.Override,
  privateWithin = Symbol.noSymbol,
)

def selectDynamicSymbol(cls: Symbol) = Symbol.newMethod(
  parent = cls,
  name = "selectDynamic",
  tpe = MethodType(List("fieldName"))(
    _ => List(TypeRepr.of[String]),
    _ => TypeRepr.of[DefinedToken[?, Ctx, ?] & TokenRefn],
  ),
  flags = Flags.Synthetic | Flags.Override,
  privateWithin = Symbol.noSymbol,
)

val cls = Symbol.newClass(
  Symbol.spliceOwner,
  Symbol.freshName("$anon"),
  List(TypeRepr.of[Tokenization[Ctx]]),
  cls =>
    definedTokenSymbols(cls) ++ ignoredTokenSymbols(cls) ++ List(
      fieldsSymbol(cls),
      lexemeRefinementSymbol(cls),
      compiledSymbol(cls),
      tokensSymbol(cls),
      selectDynamicSymbol(cls),
    ),
  None,
)

val body =
  val definedTokenVals = definedTokens.map:
    case '{ $token: DefinedToken[name, Ctx, value] } =>
      withOverridingSymbol(parent = cls)(_.fieldMember(ValidName.from[name])): owner =>
        ValDef(
          owner,
          Some('{ $token.asInstanceOf[DefinedToken[name, Ctx, value] & TokenRefn] }.asTerm.changeOwner(owner)),
        )

  val ignoredTokenVals = ignoredTokens.map:
    case '{ $token: IgnoredToken[name, Ctx] } =>
      withOverridingSymbol(parent = cls)(_.fieldMember(ValidName.from[name])): owner =>
        ValDef(
          owner,
          Some('{ $token.asInstanceOf[IgnoredToken[name, Ctx] & TokenRefn] }.asTerm.changeOwner(owner)),
        )

  definedTokenVals ++ ignoredTokenVals ++ Vector(
    TypeDef(lexemeRefinementSymbol(cls)),
    TypeDef(fieldsSymbol(cls)),
    withOverridingSymbol(parent = cls)(compiledSymbol): owner =>
      ValDef(
        owner,
        Some {
          val regex = Expr(
            infos
              .map:
                case TokenInfo(_, regexGroupName, pattern) => s"(?<$regexGroupName>$pattern)"
              .mkString("|")
              .r
              .regex, // we'd like to compile it here to fail in compile time if regex is invalid
          )

          '{ Regex($regex) }.asTerm.changeOwner(owner)
        },
      ),
    withOverridingSymbol(parent = cls)(tokensSymbol): owner =>
      ValDef(
        owner,
        Some {
          Expr
            .ofList(tokenSymbols(cls).map(Ref(_).asExprOf[ThisToken]))
            .asTerm
            .changeOwner(owner)
        },
      ),
    withOverridingSymbol(parent = cls)(selectDynamicSymbol): owner =>
      DefDef(
        owner,
        {
          case List(List(fieldName: Term)) =>
            Some:
              Match(
                Typed(
                  fieldName,
                  Annotated(
                    TypeTree.ref(fieldName.tpe.typeSymbol),
                    '{ new annotation.switch }.asTerm.changeOwner(owner),
                  ),
                ),
                definedTokenVals.map: valDef =>
                  CaseDef(Literal(StringConstant(NameTransformer.encode(valDef.name))), None, Ref(valDef.symbol)),
              ).changeOwner(owner)
          case _ => None
        },
      ),
  )

  logger.trace("checking regex patterns")
  RegexChecker.checkPatterns(infos.map(_.pattern))

  val fields = tokens.map((expr, name) => (name, expr.asTerm.tpe))

  val selectDynamicLambda = createLambda[String => Token[?, Ctx, ?]]:
    case (methSym, List(fieldName: Term)) =>
      Match(
        Typed(
          fieldName,
          Annotated(
            TypeTree.ref(fieldName.tpe.typeSymbol),
            '{ new annotation.switch }.asTerm.changeOwner(methSym),
          ),
        ),
        tokens.map: (expr, name) =>
          CaseDef(Literal(StringConstant(NameTransformer.encode(name))), None, expr.asTerm),
      ).changeOwner(methSym)

  logger.trace("creating tokenization class instance")
  (refinementTpeFrom(fields).asType, fieldsTpeFrom(fields).asType).runtimeChecked match
    case ('[refinedTpe], '[fields]) =>
      val tokensExpr = Expr.ofList(tokens.map(_.expr))
      infos.iterator.foreach: info =>
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
            override val tokens: List[Token[?, Ctx, ?]] = $tokensExpr

            override def selectDynamic(name: String): Token[?, Ctx, ?] = $selectDynamicLambda(name)

            override protected val compiled: java.util.regex.Pattern = Pattern.compile($regex)
        }.asInstanceOf[Tokenization[Ctx] { type LexemeFields = lexemeFields; type Fields = fields } & refinedTpe]
      }
// $COVERAGE-ON$
