package alpaca.lexer

import alpaca.*
import alpaca.core.*

import scala.annotation.experimental
import scala.quoted.*

type LexerDefinition[Ctx <: EmptyCtx] = PartialFunction[String, Token[?, Ctx]]

transparent inline private def lexer[Ctx <: EmptyCtx & Product](
  using Ctx := NoCtx,
)(
  inline rules: Ctx ?=> LexerDefinition[Ctx],
)(using copy: Copyable[Ctx],
): Tokenization[Ctx] =
  ${ lexerImpl[Ctx]('{ rules }, '{ copy }) }

//todo: ctxManipulation should work
//todo: more complex expressions should be supported in remaping
private def lexerImpl[Ctx <: EmptyCtx: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  copy: Expr[Copyable[Ctx]],
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
  val tokens: List[Expr[Token[?, Ctx]]] = cases.foldLeft(List.empty[Expr[Token[?, Ctx]]]) {
    case (tokens, CaseDef(pattern, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
      )

      def extractSimple(body: Expr[Token[?, Ctx]], ctxManipulation: Expr[CtxManipulation[Ctx]]) = body.matchOption:
        case '{ type t <: ValidName; Token.Ignored[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new IgnoredToken[name, Ctx]($name, $regex, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new DefinedToken[name, Ctx]($name, $regex, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t]($value)(using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              val remapping = createLambda[Remapping[Ctx]] { case (methSym, (newCtx: Term) :: Nil) =>
                replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
              }
              '{ new DefinedToken[name, Ctx]($name, $regex, $ctxManipulation, $remapping) }

      val newTokens =
        extractSimple(body.asExprOf[Token[?, Ctx]], '{ CtxManipulation.empty })
          .orElse {
            body match
              case Block(statements, expr) =>
                val ctxManipulation = createLambda[CtxManipulation[Ctx]] { case (methSym, (newCtx: Term) :: Nil) =>
                  replaceWithNewCtx(newCtx).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
                }
                extractSimple(expr.asExprOf[Token[?, Ctx]], ctxManipulation)
              case x => raiseShouldNeverBeCalled(x.show)
          }
          .getOrElse(raiseShouldNeverBeCalled(body.show))

      // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
      newTokens ::: tokens
    case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
  }

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?, Ctx]]).runtimeChecked

  def decls(cls: Symbol): List[Symbol] = definedTokens.map { case '{ $token: DefinedToken[name, Ctx] } =>
    Symbol.newVal(
      parent = cls,
      name = typeToString[name],
      tpe = TypeRepr.of[Token[name, Ctx]],
      flags = Flags.Synthetic,
      privateWithin = Symbol.noSymbol,
    )
  } :+ Symbol.newVal(
    parent = cls,
    name = "tokens",
    tpe = TypeRepr.of[List[Token[?, Ctx]]],
    flags = Flags.Synthetic | Flags.Override,
    privateWithin = Symbol.noSymbol,
  )

  val cls = Symbol.newClass(
    Symbol.spliceOwner,
    Symbol.freshName("$anon"),
    List(TypeRepr.of[Tokenization[Ctx]]),
    decls,
    None,
  )

  val body = definedTokens.collect { case '{ $token: DefinedToken[name, Ctx] } =>
    ValDef(cls.fieldMember(typeToString[name]), Some(token.asTerm.changeOwner(cls.fieldMember(typeToString[name]))))
  } :+ ValDef(
    cls.fieldMember("tokens"),
    Some {
      val declaredTokens =
        cls.fieldMembers
          .filter(_.typeRef.widen <:< TypeRepr.of[Token[?, Ctx]])
          .map(Select(This(cls), _).asExprOf[Token[?, Ctx]])

      Expr.ofList(ignoredTokens ++ declaredTokens).asTerm.changeOwner(cls.fieldMember("tokens"))
    },
  )

  val tokenizationConstructor = TypeRepr.of[Tokenization[Ctx]].typeSymbol.primaryConstructor

  val parents =
    Select(New(TypeTree.of[Tokenization[Ctx]]), tokenizationConstructor)
      .appliedToType(TypeRepr.of[Ctx])
      .appliedTo(copy.asTerm) :: Nil

  val clsDef = ClassDef(cls, parents, body)

  definedTokens
    .foldLeft(TypeRepr.of[Tokenization[Ctx]]) {
      case (tpe, '{ $token: DefinedToken[name, Ctx] }) =>
        Refinement(tpe, typeToString[name], TypeRepr.of[Token[name, Ctx]])
      case _ => raiseShouldNeverBeCalled()
    }
    .asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
}
