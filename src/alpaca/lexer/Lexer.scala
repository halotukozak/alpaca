package alpaca
package lexer

import alpaca.core.*
import alpaca.lexer.context.AnyGlobalCtx
import alpaca.lexer.context.default.{DefaultGlobalCtx, DefaultLexem}

import scala.NamedTuple.NamedTuple
import scala.annotation.experimental
import scala.quoted.*

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

  type ThisToken = Token[?, Ctx, ?]

  def nameToString[Name <: ValidName: Type]: ValidName =
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str
      case x => raiseShouldNeverBeCalled(x.show)

  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val (tokens, patterns) = cases.foldLeft((tokens = Vector.empty[Expr[ThisToken]], patterns = Vector.empty[String])):
    case ((tokens, patterns), CaseDef(pattern, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
      )

      def extractSimple(ctxManipulation: Expr[CtxManipulation[Ctx]])
        : PartialFunction[Expr[ThisToken], List[(Expr[ThisToken], String)]] =
        case '{ Token.Ignored(using $ctx) } =>
          compileNameAndPattern[Nothing](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ IgnoredToken[name, Ctx]($name, ${ Expr(regex) }, $ctxManipulation) } -> regex

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ DefinedToken[name, Ctx, Unit]($name, ${ Expr(regex) }, $ctxManipulation, _ => ()) } -> regex

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } if value.asTerm.symbol == pattern.symbol =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ DefinedToken[name, Ctx, String]($name, ${ Expr(regex) }, $ctxManipulation, _.text) } -> regex

        case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              // we need to widen here to avoid weird types
              TypeRepr.of[v].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result] { case (methSym, (newCtx: Term) :: Nil) =>
                    replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                  }
                  '{ DefinedToken[name, Ctx, result]($name, ${ Expr(regex) }, $ctxManipulation, $remapping) } -> regex

      extractSimple('{ identity })
        .lift(body.asExprOf[ThisToken])
        .orElse {
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]] { case (methSym, (newCtx: Term) :: Nil) =>
                replaceWithNewCtx(newCtx).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
              }

              extractSimple(ctxManipulation).lift(expr.asExprOf[ThisToken])
        }
        .getOrElse(raiseShouldNeverBeCalled(body.show))
        .foldLeft((tokens, patterns)) { case ((tokens, patterns), (token, pattern)) =>
          (tokens :+ token, patterns :+ pattern)
        }

    case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")

  RegexChecker.checkPatterns(patterns).foreach(report.errorAndAbort)

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?, Ctx, ?]])

  def decls(cls: Symbol): List[Symbol] = {
    val tokenDecls = definedTokens.map { case '{ $token: DefinedToken[name, Ctx, value] } =>
      Symbol.newVal(
        parent = cls,
        name = nameToString[name],
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
      tpe = TypeRepr.of[List[ThisToken]],
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

    tokenDecls.toList ++ List(fieldsDecls, allTokens, byName)
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
      ValDef(cls.fieldMember(nameToString[name]), Some(token.asTerm.changeOwner(cls.fieldMember(nameToString[name]))))
    }

    tokenVals ++ Vector(
      TypeDef(cls.typeMember("Fields")),
      ValDef(
        cls.fieldMember("tokens"),
        Some {
          val declaredTokens = definedTokens.map { case '{ $token: DefinedToken[name, Ctx, ?] } =>
            This(cls).select(cls.fieldMember(nameToString[name])).asExprOf[ThisToken]
          }

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
  }.toList

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
        Refinement(tpe, nameToString[name], token.asTerm.tpe)
      case _ => raiseShouldNeverBeCalled()
    }
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
}
