package alpaca
package internal
package lexer

import scala.NamedTuple.NamedTuple
import scala.util.matching.Regex
import java.util.regex.Pattern

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
def lexerImpl[Ctx <: LexerCtx: Type, LexemeRefn: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
)(using debugSettings: Expr[DebugSettings],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeRefinement = LexemeRefn }] = {
  import quotes.reflect.*
  type ThisToken = Token[?, Ctx, ?]
  type TokenRefn = ThisToken { type LexemeTpe = LexemeRefn }

  val lexerName = Symbol.spliceOwner.owner.name.stripSuffix("$") // todo: debug lexer
  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val (tokens, infos) = cases.foldLeft((List.empty[Expr[ThisToken]], List.empty[TokenInfo[?]])):
    case ((accTokens, accInfos), CaseDef(tree, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = tree.symbol, replace = Select.unique(newCtx, "lastRawMatched")),
      )

      def extractSimple(
        ctxManipulation: Expr[CtxManipulation[Ctx]],
      ): PartialFunction[Expr[ThisToken], List[Expr[ThisToken]]] =
        case '{ Token.Ignored(using $ctx) } =>
          compileNameAndPattern[Nothing](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } => '{ IgnoredToken[name, Ctx]($tokenInfo, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              '{ DefinedToken[name, Ctx, Unit]($tokenInfo, $ctxManipulation, _ => ()) }

        case '{ type t <: ValidName; Token.apply[t]($value: String)(using $ctx) }
            if value.asTerm.symbol == tree.symbol =>

          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              '{ DefinedToken[name, Ctx, String]($tokenInfo, $ctxManipulation, _.lastRawMatched) }

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } =>
          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              // we need to widen here to avoid weird types
              TypeRepr.of[v].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  '{ DefinedToken[name, Ctx, result]($tokenInfo, $ctxManipulation, $remapping) }

      val tokens = extractSimple('{ _ => () })
        .lift(body.asExprOf[ThisToken])
        .orElse:
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(
                    Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                  )(methSym)

              extractSimple(ctxManipulation).lift(expr.asExprOf[ThisToken])
        .getOrElse(raiseShouldNeverBeCalled(body))

      val infos = tokens.unsafeMap:
        case '{ type name <: ValidName; DefinedToken[name, Ctx, value]($tokenInfo, $ctxManipulation, $remapping) } =>
          tokenInfo.valueOrAbort
        case '{ type name <: ValidName; IgnoredToken[name, Ctx]($tokenInfo, $ctxManipulation) } =>
          tokenInfo.valueOrAbort

      val patterns = infos.map(_.pattern)
      RegexChecker.checkPatterns(patterns).foreach(report.errorAndAbort)
      RegexChecker.checkPatterns(patterns.reverse).foreach(report.errorAndAbort)
      (accTokens ::: tokens, accInfos ::: infos)

    case (tokens, CaseDef(tree, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?, Ctx, ?]])

  RegexChecker.checkPatterns(infos.map(_.pattern)).foreach(report.errorAndAbort)

  def decls(cls: Symbol): List[Symbol] = {
    val tokenDecls = definedTokens.map:
      case '{ $token: DefinedToken[name, Ctx, value] } =>
        Symbol.newVal(
          parent = cls,
          name = ValidName.from[name],
          tpe = token.asTerm.tpe,
          flags = Flags.Synthetic,
          privateWithin = Symbol.noSymbol,
        )

    val fieldTpe = definedTokens
      .unsafeFoldLeft[(Type[? <: Tuple], Type[? <: Tuple])]((Type.of[EmptyTuple], Type.of[EmptyTuple])):
        case (
              ('[type names <: Tuple; names], '[type types <: Tuple; types]),
              '{ $token: Token[name, Ctx, value] },
            ) =>
          (Type.of[name *: names], Type.of[Token[name, Ctx, value] *: types])
      .runtimeChecked
      .match
        case ('[type names <: Tuple; names], '[type types <: Tuple; types]) => TypeRepr.of[NamedTuple[names, types]]

    val fieldsDecls = Symbol.newTypeAlias(
      parent = cls,
      name = "Fields",
      tpe = fieldTpe,
      flags = Flags.Synthetic,
      privateWithin = Symbol.noSymbol,
    )

    val lexemeRefinement = Symbol.newTypeAlias(
      parent = cls,
      name = "LexemeRefinement",
      tpe = TypeRepr.of[LexemeRefn],
      flags = Flags.Synthetic,
      privateWithin = Symbol.noSymbol,
    )

    val compiled = Symbol.newVal(
      parent = cls,
      name = "compiled",
      tpe = TypeRepr.of[Pattern],
      flags = Flags.Protected | Flags.Synthetic | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val allTokens = Symbol.newVal(
      parent = cls,
      name = "tokens",
      tpe = TypeRepr.of[List[ThisToken]],
      flags = Flags.Synthetic | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val byName = Symbol.newVal(
      parent = cls,
      name = "byName",
      tpe = TypeRepr.of[Map[String, DefinedToken[?, Ctx, ?] & TokenRefn]],
      flags = Flags.Synthetic | Flags.Override | Flags.Lazy, // todo: reconsider lazy
      privateWithin = Symbol.noSymbol,
    )

    tokenDecls ++ List(fieldsDecls, lexemeRefinement, compiled, allTokens, byName)
  }

  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    decls,
    None,
  )

  val body = {
    val tokenVals = definedTokens.map:
      case '{ $token: Token[name, Ctx, value] } =>
        ValDef(
          cls.fieldMember(ValidName.from[name]),
          Some(token.asTerm.changeOwner(cls.fieldMember(ValidName.from[name]))),
        )

    tokenVals ++ Vector(
      TypeDef(cls.typeMember("LexemeRefinement")),
      TypeDef(cls.typeMember("Fields")),
      ValDef(
        cls.fieldMember("compiled"),
        Some {
          val regex = Expr(
            infos
              .map:
                case TokenInfo(_, regexGroupName, pattern) => s"(?<$regexGroupName>$pattern)"
              .mkString("|")
              .tap(Pattern.compile), // we'd like to compile it here to fail in compile time if regex is invalid
          )

          '{ Pattern.compile($regex) }.asTerm.changeOwner(cls.fieldMember("compiled"))
        },
      ),
      ValDef(
        cls.fieldMember("tokens"),
        Some {
          val declaredTokens = definedTokens.map:
            case '{ $token: Token[name, Ctx, value] } =>
              This(cls).select(cls.fieldMember(ValidName.from[name])).asExprOf[ThisToken]

          Expr.ofList(ignoredTokens ++ declaredTokens).asTerm.changeOwner(cls.fieldMember("tokens"))
        },
      ),
      ValDef(
        cls.fieldMember("byName"),
        Some {
          val all = Expr.ofSeq {
            tokenVals.map: valDef =>
              Expr.ofTuple((Expr(valDef.name), Ref(valDef.symbol).asExprOf[DefinedToken[?, Ctx, ?]]))
          }
          // it's simpler to add cast here than e.g. bypass apply of DefinedToken
          '{ Map($all*).asInstanceOf[Map[String, DefinedToken[?, Ctx, ?] & TokenRefn]] }.asTerm
        },
      ),
    )
  }

  val tokenizationConstructor = TypeRepr.of[Tokenization[Ctx]].typeSymbol.primaryConstructor

  val parents =
    New(TypeTree.of[Tokenization[Ctx]])
      .select(tokenizationConstructor)
      .appliedToType(TypeRepr.of[Ctx])
      .appliedToArgs(List(copy.asTerm, betweenStages.asTerm)) :: Nil

  val clsDef = ClassDef(cls, parents, body)

  definedTokens
    .unsafeFoldLeft(TypeRepr.of[Tokenization[Ctx] { type LexemeRefinement = LexemeRefn }]):
      case (tpe, '{ $token: Token[name, Ctx, value] }) =>
        Refinement(
          tpe,
          ValidName.from[name],
          TypeRepr.of[DefinedToken[name, Ctx, value] & TokenRefn],
        )
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] { type LexemeRefinement = LexemeRefn } & refinedTpe]
}
