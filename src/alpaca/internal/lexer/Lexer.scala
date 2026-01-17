package alpaca
package internal
package lexer

import alpaca.Token as TokenDef

import java.util.regex.Pattern
import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.switch
import scala.reflect.NameTransformer

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

  def tokenSymbols(cls: Symbol) = tokens.map:
    case (expr, name) =>
      Symbol.newVal(
        parent = cls,
        name = name,
        tpe = expr.asTerm.tpe,
        flags = Flags.Synthetic,
        privateWithin = Symbol.noSymbol,
      )

  val fields = definedTokens.map((expr, name) => (name, expr.asTerm.tpe))

  def fieldsSymbol(cls: Symbol) = Symbol.newTypeAlias(
    parent = cls,
    name = "Fields",
    tpe = fieldsTpeFrom(fields),
    flags = Flags.Synthetic,
    privateWithin = Symbol.noSymbol,
  )

  def lexemeFieldsSymbol(cls: Symbol) = Symbol.newTypeAlias(
    parent = cls,
    name = "LexemeFields",
    tpe = TypeRepr.of[lexemeFields],
    flags = Flags.Synthetic,
    privateWithin = Symbol.noSymbol,
  )

  def compiledSymbol(cls: Symbol) = Symbol.newVal(
    parent = cls,
    name = "compiled",
    tpe = TypeRepr.of[Pattern],
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

  logger.trace("creating tokenization class symbol")
  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    cls =>
      tokenSymbols(cls) ++ List(
        fieldsSymbol(cls),
        lexemeFieldsSymbol(cls),
        compiledSymbol(cls),
        tokensSymbol(cls),
        selectDynamicSymbol(cls),
      ),
    None,
  )

  logger.trace("creating tokenization class body")
  val body =
    logger.trace("creating defined token vals")
    val tokenVals = tokens.map: (expr, name) =>
      withOverridingSymbol(parent = cls)(_.fieldMember(name)): owner =>
        ValDef(owner, Some(expr.asTerm.changeOwner(owner)))

    tokenVals ++ Vector(
      TypeDef(lexemeFieldsSymbol(cls)),
      TypeDef(fieldsSymbol(cls)),
      withOverridingSymbol(parent = cls)(compiledSymbol): owner =>
        ValDef(
          owner,
          Some {
            val regex = Expr(
              infos
                .map:
                  case TokenInfo(_, regexGroupName, pattern) => show"(?<$regexGroupName>$pattern)"
                .mkString("|")
                .tap(Pattern.compile), // we'd like to compile it here to fail in compile time if regex is invalid
            )

            '{ Pattern.compile($regex) }.asTerm.changeOwner(owner)
          },
        ),
      withOverridingSymbol(parent = cls)(tokensSymbol): owner =>
        ValDef(
          owner,
          Some {
            Expr
              .ofList(tokenSymbols(cls).map(Ref(_).asExprOf[Token[?, Ctx, ?]]))
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
                  tokenVals.collect:
                    case valDef: ValDef if valDef.symbol.typeRef <:< TypeRepr.of[DefinedToken[?, ?, ?]] =>
                      CaseDef(Literal(StringConstant(NameTransformer.encode(valDef.name))), None, Ref(valDef.symbol)),
                ).changeOwner(owner)
            case _ => None
          },
        ),
    )

  logger.trace("creating tokenization class definition")
  val tokenizationConstructor = TypeRepr.of[Tokenization[Ctx]].typeSymbol.primaryConstructor

  logger.trace("creating tokenization class parents")
  val parents =
    New(TypeTree.of[Tokenization[Ctx]])
      .select(tokenizationConstructor)
      .appliedToType(TypeRepr.of[Ctx])
      .appliedToArgs(List(betweenStages.asTerm)) :: Nil

  logger.trace("creating tokenization class definition")
  val clsDef = ClassDef(cls, parents, body)

  logger.trace("creating tokenization class instance")
  refinementTpeFrom(fields).asType match
    case '[refinedTpe] =>
      val newCls = Typed(
        New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone,
        TypeTree.of[Tokenization[Ctx] { type LexemeFields = lexemeFields } & refinedTpe],
      )

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] { type LexemeFields = lexemeFields } & refinedTpe]
