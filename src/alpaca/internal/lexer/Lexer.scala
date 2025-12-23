package alpaca
package internal
package lexer

import scala.NamedTuple.NamedTuple
import scala.util.matching.Regex
import NamedTuple.AnyNamedTuple

//todo: private[alpaca]
def lexerImpl[Ctx <: LexerCtx: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
)(using debugSettings: Expr[DebugSettings],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx]] = runWithTimeout {
  import quotes.reflect.*
  // val lexerName = Symbol.spliceOwner.owner.name.stripSuffix("$") //todo: parser debug
  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val (tokens, infos) = cases.foldLeft((List.empty[Expr[Token[?, Ctx, ?]]], List.empty[TokenInfo[?]])):
    case ((accTokens, accInfos), CaseDef(tree, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = tree.symbol, replace = Select.unique(newCtx, "lastRawMatched")),
      )

      def extractSimple(
        ctxManipulation: Expr[CtxManipulation[Ctx]],
      ): PartialFunction[Expr[TokenDefinition[ValidNameLike, Ctx, Any]], List[Expr[Token[?, Ctx, ?]]]] =
        case '{ Token.Ignored(using $ctx) } =>
          compileNameAndPattern[Nothing](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } => '{ IgnoredToken[name, Ctx]($tokenInfo, $ctxManipulation) }

        case '{ type name <: ValidNameLike; Token[name](using $ctx) } =>
          compileNameAndPattern[name](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              '{ DefinedToken[name, Ctx, Unit]($tokenInfo, $ctxManipulation, _ => ()) }

        case '{ type name <: ValidNameLike; Token[name]($value: String | Char)(using $ctx) }
            if value.asTerm.symbol == tree.symbol =>
          compileNameAndPattern[name](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              '{ DefinedToken[name, Ctx, String | Char]($tokenInfo, $ctxManipulation, _.lastRawMatched) }

        case '{ type name <: ValidNameLike; Token[name]($value: value)(using $ctx) } =>
          compileNameAndPattern[name](tree).map:
            case '{ $tokenInfo: TokenInfo[name] } =>
              // we need to widen here to avoid weird types
              TypeRepr.of[value].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  '{ DefinedToken[name, Ctx, result]($tokenInfo, $ctxManipulation, $remapping) }

      val tokens = extractSimple('{ _ => () })
        .lift(body.asExprOf[TokenDefinition[ValidNameLike, Ctx, Any]])
        .orElse:
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(
                    Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                  )(methSym)

              extractSimple(ctxManipulation).lift(expr.asExprOf[TokenDefinition[ValidNameLike, Ctx, Any]])
        .getOrElse(raiseShouldNeverBeCalled(body))

      val infos = tokens.unsafeMap:
        case '{ type name <: ValidName; DefinedToken[name, Ctx, value]($tokenInfo, $ctxManipulation, $remapping) } =>
          tokenInfo.valueOrAbort
        case '{ type name <: ValidName; IgnoredToken[name, Ctx]($tokenInfo, $ctxManipulation) } =>
          tokenInfo.valueOrAbort

      RegexChecker.checkInfos(infos)
      RegexChecker.checkInfos(infos.reverse)
      (accTokens ::: tokens, accInfos ::: infos)

    case (tokens, CaseDef(tree, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?, Ctx, ?]])

  RegexChecker.checkInfos(infos)

  def decls(cls: Symbol): List[Symbol] = {
    val definedTokenDecls = definedTokens.map:
      case '{ $token: DefinedToken[name, Ctx, value] } =>
        Symbol.newVal(
          parent = cls,
          name = ValidName.from[name],
          tpe = token.asTerm.tpe,
          flags = Flags.Synthetic,
          privateWithin = Symbol.noSymbol,
        )

    val ignoredTokenDecls = ignoredTokens.map:
      case '{ $token: IgnoredToken[name, Ctx] } =>
        Symbol.newVal(
          parent = cls,
          name = ValidName.from[name],
          tpe = token.asTerm.tpe,
          flags = Flags.Synthetic | Flags.Private,
          privateWithin = Symbol.noSymbol,
        )

    val fieldTpe = definedTokens
      .unsafeFoldLeft[(Type[? <: Tuple], Type[? <: Tuple])]((Type.of[EmptyTuple], Type.of[EmptyTuple])):
        case (
              ('[type names <: Tuple; names], '[type types <: Tuple; types]),
              '{ $token: DefinedToken[name, Ctx, value] },
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
      tpe = TypeRepr.of[List[Token[?, Ctx, ?]]],
      flags = Flags.Synthetic | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val byName = Symbol.newVal(
      parent = cls,
      name = "byName",
      tpe = TypeRepr.of[Map[String, DefinedToken[?, Ctx, ?]]],
      flags = Flags.Protected | Flags.Synthetic | Flags.Lazy | Flags.Override,
      privateWithin = Symbol.noSymbol,
    )

    val byLiteral = Symbol.newVal(
      parent = cls,
      name = "byLiteral",
      tpe = TypeRepr.of[Map[Char, Token[?, Ctx, ?]]],
      flags = Flags.Synthetic | Flags.Lazy | Flags.Override, // todo: reconsider lazy //todo: protected
      privateWithin = Symbol.noSymbol,
    )

    definedTokenDecls ++ ignoredTokenDecls ++ List(fieldsDecls, compiled, allTokens, byName, byLiteral)
  }

  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    decls,
    None,
  )

  val body = {
    val definedTokenVals = definedTokens.collect:
      case '{ $token: DefinedToken[name, Ctx, value] } =>
        ValDef(
          cls.fieldMember(ValidName.from[name]),
          Some(token.asTerm.changeOwner(cls.fieldMember(ValidName.from[name]))),
        )

    val ignoredTokenVals = ignoredTokens.collect:
      case '{ $token: IgnoredToken[name, Ctx] } =>
        ValDef(
          cls.fieldMember(ValidName.from[name]),
          Some(token.asTerm.changeOwner(cls.fieldMember(ValidName.from[name]))),
        )

    definedTokenVals ++ ignoredTokenVals ++ Vector(
      TypeDef(cls.typeMember("Fields")),
      ValDef(
        cls.fieldMember("compiled"),
        Some {
          val regex = Expr(
            infos
              .map: tokenInfo =>
                val regex = tokenInfo.toEscapedRegex // todo: literals should be handled separately
                s"(?<${tokenInfo.regexGroupName}>$regex)"
              .mkString("|")
              .tap(_.soft)
              .r
              .regex, // we'd like to compile it here to fail in compile time if regex is invalid
          )

          '{ Regex($regex) }.asTerm.changeOwner(cls.fieldMember("compiled"))
        },
      ),
      ValDef(
        cls.fieldMember("tokens"),
        Some {
          val tokens = infos.map:
            case TokenInfo(name, _, _) =>
              This(cls).select(cls.fieldMember(name)).asExprOf[Token[?, Ctx, ?]]

          Expr.ofList(tokens).asTerm.changeOwner(cls.fieldMember("tokens"))
        },
      ),
      ValDef(
        cls.fieldMember("byName"),
        Some {
          val all = Expr.ofSeq {
            definedTokenVals.map(valDef =>
              Expr.ofTuple((Expr(valDef.name), Ref(valDef.symbol).asExprOf[DefinedToken[?, Ctx, ?]])),
            )
          }

          '{ Map($all*) }.asTerm.changeOwner(cls.fieldMember("byName"))
        },
      ),
      ValDef(
        cls.fieldMember("byLiteral"),
        Some {
          val all = Expr.ofSeq {
            (definedTokenVals ++ ignoredTokenVals).collect:
              case valDef if valDef.name.length == 1 =>
                Expr.ofTuple((Expr(valDef.name.head), Ref(valDef.symbol).asExprOf[DefinedToken[?, Ctx, ?]]))
          }
          '{ Map($all*) }.asTerm.changeOwner(cls.fieldMember("byLiteral"))
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
    .unsafeFoldLeft(TypeRepr.of[Tokenization[Ctx]]):
      case (tpe, '{ $token: DefinedToken[name, Ctx, value] }) =>
        Refinement(tpe, ValidName.from[name], token.asTerm.tpe)
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
}
