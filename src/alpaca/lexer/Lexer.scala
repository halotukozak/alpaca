package alpaca
package lexer

import alpaca.core.*
import alpaca.lexer.context.AnyGlobalCtx
import alpaca.lexer.context.default.{DefaultGlobalCtx, DefaultLexem}

import scala.NamedTuple.NamedTuple
import scala.annotation.experimental
import scala.quoted.*

transparent inline given ctx(using c: AnyGlobalCtx): c.type = c

type LexerDefinition[Ctx <: AnyGlobalCtx] = PartialFunction[String, Token[?, Ctx, ?]]

@experimental //for IJ  :/
transparent inline def lexer[Ctx <: AnyGlobalCtx & Product](
  using Ctx WithDefault DefaultGlobalCtx[DefaultLexem[?, ?]],
)(
  inline rules: Ctx ?=> LexerDefinition[Ctx],
)(using
  copy: Copyable[Ctx],
  betweenStages: BetweenStages[Ctx],
): Tokenization[Ctx] =
  ${ lexerImpl[Ctx]('{ rules }, '{ summon }, '{ summon }) }

//todo: ctxManipulation should work
//todo: more complex expressions should be supported in remaping
@experimental //for IJ  :/
private def lexerImpl[Ctx <: AnyGlobalCtx: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
  betweenStages: Expr[BetweenStages[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx]] = {
  import quotes.reflect.*

//todo: only for debugging, remove in real world
  def stringToType(str: String): Type[ValidName] =
    ConstantType(StringConstant(str)).asType.asInstanceOf[Type[ValidName]]

//todo: only for debugging, remove in real world
  def typeToString[Name <: ValidName: Type]: ValidName =
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str // todo: probably we can make less work to get it right
      case x => raiseShouldNeverBeCalled(x.show)

  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val tokens: List[(token: Expr[Token[?, Ctx, ?]], pattern: Expr[String])] = cases.foldLeft(List.empty[(Expr[Token[?, Ctx, ?]], Expr[String])]) {
    case (tokens, CaseDef(pattern, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
      )

      def extractSimple(body: Expr[Token[?, Ctx, ?]], ctxManipulation: Expr[CtxManipulation[Ctx]]) = body.matchOption:
        case '{ type t <: ValidName; Token.Ignored[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new IgnoredToken[name, Ctx]($name, $regex, $ctxManipulation) } -> regex

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new DefinedToken[name, Ctx, Unit]($name, $regex, $ctxManipulation, _ => ()) } -> regex

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } if value.asTerm.symbol == pattern.symbol =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new DefinedToken[name, Ctx, String]($name, $regex, $ctxManipulation, _.text) } -> regex

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              val remapping = createLambda[Ctx => v] { case (methSym, (newCtx: Term) :: Nil) =>
                replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
              }
              '{ new DefinedToken[name, Ctx, v]($name, $regex, $ctxManipulation, $remapping) } -> regex

      val newTokens =
        extractSimple(body.asExprOf[Token[?, Ctx, ?]], '{ identity })
          .orElse {
            body match
              case Block(statements, expr) =>
                val ctxManipulation = createLambda[CtxManipulation[Ctx]] { case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
                }
                extractSimple(expr.asExprOf[Token[?, Ctx, ?]], ctxManipulation)
              case x => raiseShouldNeverBeCalled(x.show)
          }
          .getOrElse(raiseShouldNeverBeCalled(body.show))

      // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
      newTokens ::: tokens
    case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
  }

  RegexChecker.checkPatterns(tokens.map(_.pattern.valueOrAbort))

  val (definedTokens, ignoredTokens) = tokens.map(_.token).partition(_.isExprOf[DefinedToken[?, Ctx, ?]]).runtimeChecked

  def decls(cls: Symbol): List[Symbol] = {
    val tokenDecls = definedTokens
      .map { case '{ $token: DefinedToken[name, Ctx, value] } =>
        Symbol.newVal(
          parent = cls,
          name = typeToString[name],
          tpe = token.asTerm.tpe,
          flags = Flags.Synthetic,
          privateWithin = Symbol.noSymbol,
        )
      }

    val fieldTpe = definedTokens
      .foldLeft[(Type[? <: Tuple], Type[? <: Tuple])]((Type.of[EmptyTuple], Type.of[EmptyTuple])) {
        case (
              ('[type names <: Tuple; names], '[type types <: Tuple; types]),
              '{ $token: DefinedToken[name, Ctx, value] },
            ) =>
          (Type.of[name *: names], Type.of[Token[name, Ctx, value] *: types])
        case _ => raiseShouldNeverBeCalled()
      }
      .runtimeChecked match
      case ('[type names <: Tuple; names], '[type types <: Tuple; types]) => TypeRepr.of[NamedTuple[names, types]]

    val fieldsDecls = Symbol.newTypeAlias(
      parent = cls,
      name = "Fields",
      tpe = fieldTpe,
      flags = Flags.Synthetic,
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
      flags = Flags.Synthetic | Flags.Lazy, // todo: reconsider lazy
      privateWithin = Symbol.noSymbol,
    )

    tokenDecls ++ List(fieldsDecls, allTokens, byName)
  }

  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    decls,
    None,
  )

  val body = {
    val tokenVals = definedTokens.collect { case '{ $token: DefinedToken[name, Ctx, value] } =>
      ValDef(cls.fieldMember(typeToString[name]), Some(token.asTerm.changeOwner(cls.fieldMember(typeToString[name]))))
    }
    tokenVals ++ List(
      TypeDef(cls.typeMember("Fields")),
      ValDef(
        cls.fieldMember("tokens"),
        Some {
          val declaredTokens =
            cls.fieldMembers
              .filter(_.typeRef.widen <:< TypeRepr.of[Token[?, Ctx, ?]])
              .map(This(cls).select(_).asExprOf[Token[?, Ctx, ?]])

          Expr.ofList(ignoredTokens ++ declaredTokens).asTerm.changeOwner(cls.fieldMember("tokens"))
        },
      ),
      ValDef(
        cls.fieldMember("byName"),
        Some {
          val all = Expr.ofSeq {
            tokenVals.map(valDef => Expr.ofTuple((Expr(valDef.name), Ref(valDef.symbol).asExprOf[DefinedToken[?, Ctx, ?]])))
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
      .appliedToArgs(List(copy.asTerm, betweenStages.asTerm)) :: Nil

  val clsDef = ClassDef(cls, parents, body)

  definedTokens
    .foldLeft(TypeRepr.of[Tokenization[Ctx]]) {
      case (tpe, '{ $token: DefinedToken[name, Ctx, value] }) =>
        Refinement(tpe, typeToString[name], token.asTerm.tpe)
      case _ => raiseShouldNeverBeCalled()
    }
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
}
