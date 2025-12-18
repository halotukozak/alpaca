package alpaca
package internal
package lexer

import scala.util.matching.Regex
import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.unchecked.uncheckedVariance as uv

/**
 * Type alias for lexer rule definitions.
 *
 * A lexer definition is a partial function that maps string patterns
 * (as regex literals) to token definitions.
 *
 * @tparam Ctx the global context type
 */
private[alpaca] type LexerDefinition[Ctx <: LexerCtx] = PartialFunction[String, Token[Ctx]]

//todo: private[alpaca]
def lexerImpl[Ctx <: LexerCtx: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Ctx is Copyable],
  empty: Expr[Ctx has Empty],
  betweenStages: Expr[Ctx has BetweenStages],
)(using debugSettings: Expr[DebugSettings],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx]] = {
  import quotes.reflect.*
  val lexerName = Symbol.spliceOwner.owner.name.stripSuffix("$")
  val compileNameAndPattern = new CompileNameAndPattern
  val createLambda = new CreateLambda
  val replaceRefs = new ReplaceRefs

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val (tokens, infos) = cases.foldLeft((List.empty[Expr[Token[Ctx]]], List.empty[TokenInfo])):
    case ((accTokens, accInfos), CaseDef(tree, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = replaceRefs(
        (find = oldCtx.symbol, replace = newCtx),
        (find = tree.symbol, replace = Select.unique(newCtx, "lastRawMatched")),
      )

      def extractSimple(
        ctxManipulation: Expr[CtxManipulation[Ctx]],
      ): PartialFunction[Expr[Token[Ctx]], List[Expr[Token[Ctx]]]] =
        case '{ Token.Ignored(using $ctx) } =>
          compileNameAndPattern[Nothing](tree).map: tokenInfo =>
            '{ IgnoredToken[Ctx]($tokenInfo, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo.AUX[name] } =>
              '{ DefinedToken[Ctx, Unit, name]($tokenInfo, $ctxManipulation, _ => ()) }

        case '{ type t <: ValidName; Token.apply[t]($value: String)(using $ctx) }
            if value.asTerm.symbol == tree.symbol =>

          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo.AUX[name] } =>
              '{ DefinedToken[Ctx, String, name]($tokenInfo, $ctxManipulation, _.lastRawMatched) }

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } =>
          compileNameAndPattern[t](tree).map:
            case '{ $tokenInfo: TokenInfo.AUX[name] } =>
              // we need to widen here to avoid weird types
              TypeRepr.of[v].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  '{ DefinedToken[Ctx, result, name]($tokenInfo, $ctxManipulation, $remapping) }

      val tokens: List[Expr[Token[Ctx]]] =
        extractSimple('{ _ => () })
          .lift(body.asExprOf[Token[Ctx]])
          .orElse:
            body match
              case Block(statements, expr) =>
                val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                  case (methSym, (newCtx: Term) :: Nil) =>
                    replaceWithNewCtx(newCtx).transformTerm(
                      Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                    )(methSym)

                extractSimple(ctxManipulation).lift(expr.asExprOf[Token[Ctx]])
          .getOrElse(raiseShouldNeverBeCalled(body))

      val infos = tokens.unsafeMap:
        case '{
              type name <: ValidName;
              DefinedToken.apply[Ctx, value, name]($tokenInfo, $ctxManipulation, $remapping)
            } =>
          tokenInfo.valueOrAbort
        case '{ IgnoredToken.apply[Ctx]($tokenInfo, $ctxManipulation) } =>
          tokenInfo.valueOrAbort

      val patterns = infos.map(_.pattern)
      RegexChecker.checkPatterns(patterns).foreach(report.errorAndAbort)
      RegexChecker.checkPatterns(patterns.reverse).foreach(report.errorAndAbort)
      (accTokens ::: tokens, accInfos ::: infos)

    case (tokens, CaseDef(tree, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[Ctx]])

  RegexChecker.checkPatterns(infos.map(_.pattern)).foreach(report.errorAndAbort)

  def decls(cls: Symbol): List[Symbol] = {
    val tokenDecls = definedTokens.map:
      case '{ $token: Token[Ctx] & NamedToken[name] } =>
        Symbol.newVal(
          parent = cls,
          name = ValidName.from[name],
          tpe = token.asTerm.tpe,
          flags = Flags.Synthetic,
          privateWithin = Symbol.noSymbol,
        )

    val fieldTpe = definedTokens
      .unsafeFoldLeft[(Type[? <: Tuple], Type[? <: Tuple])]((Type.of[EmptyTuple], Type.of[EmptyTuple])):
        case (('[type names <: Tuple; names], '[type types <: Tuple; types]), '{ $token: NamedToken[name] }) =>
          (Type.of[name *: names], Type.of[(Token[Ctx] & NamedToken[name]) *: types])
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

    val compiled = Symbol.newVal(
      parent = cls,
      name = "compiled",
      tpe = TypeRepr.of[Regex],
      flags = Flags.Protected | Flags.Synthetic | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val allTokens = Symbol.newVal(
      parent = cls,
      name = "tokens",
      tpe = TypeRepr.of[List[Token[Ctx]]],
      flags = Flags.Synthetic | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val byName = Symbol.newVal(
      parent = cls,
      name = "byName",
      tpe = TypeRepr.of[Map[String, DefinedToken[Ctx]]],
      flags = Flags.Synthetic | Flags.Lazy, // todo: reconsider lazy
      privateWithin = Symbol.noSymbol,
    )

    tokenDecls ++ List(fieldsDecls, compiled, allTokens, byName)
  }

  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    decls,
    None,
  )

  val body = {
    val tokenVals = definedTokens.collect:
      case '{ $token: NamedToken[name] } =>
        ValDef(
          cls.fieldMember(ValidName.from[name]),
          Some(token.asTerm.changeOwner(cls.fieldMember(ValidName.from[name]))),
        )

    tokenVals ++ Vector(
      TypeDef(cls.typeMember("Fields")),
      ValDef(
        cls.fieldMember("compiled"),
        Some {
          val regex = Expr(
            infos
              .map:
                case TokenInfo(_, regexGroupName, pattern) => s"(?<$regexGroupName>$pattern)"
              .mkString("|")
              .r
              .regex, // we'd like to compile it here to fail in compile time if regex is invalid
          )

          '{ Regex($regex) }.asTerm.changeOwner(cls.fieldMember("compiled"))
        },
      ),
      ValDef(
        cls.fieldMember("tokens"),
        Some {
          val declaredTokens = definedTokens.map:
            case '{ $token: NamedToken[name] } =>
              This(cls).select(cls.fieldMember(ValidName.from[name])).asExprOf[NamedToken[name] & Token[Ctx]]

          Expr.ofList(ignoredTokens ++ declaredTokens).asTerm.changeOwner(cls.fieldMember("tokens"))
        },
      ),
      ValDef(
        cls.fieldMember("byName"),
        Some {
          val all = Expr.ofSeq {
            tokenVals.map(valDef => Expr.ofTuple((Expr(valDef.name), Ref(valDef.symbol).asExprOf[DefinedToken[Ctx]])))
          }

          '{ Map($all*) }.asTerm
        },
      ),
    )
  }

  val tokenizationConstructor = TypeRepr.of[Tokenization[Ctx]].typeSymbol.primaryConstructor

  val parents =
    New(TypeTree.of[Tokenization[Ctx]])
      .select(tokenizationConstructor)
      .appliedToType(TypeRepr.of[Ctx])
      .appliedToArgs(List(copy.asTerm, empty.asTerm, betweenStages.asTerm)) :: Nil

  val clsDef = ClassDef(cls, parents, body)

  definedTokens
    .unsafeFoldLeft(TypeRepr.of[Tokenization[Ctx]]):
      case (tpe, '{ $token: NamedToken[name] }) =>
        Refinement(tpe, ValidName.from[name], token.asTerm.tpe)
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
}
