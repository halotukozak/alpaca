package alpaca.lexer

import alpaca.*

import scala.annotation.{experimental, tailrec}
import scala.quoted.*
import alpaca.core.ReplaceRefs
import alpaca.core.CreateLambda
import alpaca.core.raiseShouldNeverBeCalled
import alpaca.core.given
import scala.util.TupledFunction
import alpaca.core.matchOption

type LexerDefinition = PartialFunction[String, Token[?]]
transparent inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenization = ${ lexerImpl('{ rules }) }

//todo: ctxManipulation should be generic
//todo: ctxManipulation should work
//todo: more complex expressions should be supported in remaping
private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenization] = {
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
  val tokens: List[Expr[Token[?]]] = cases.foldLeft(List.empty[Expr[Token[?]]]) {
    case (tokens, CaseDef(pattern, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = new ReplaceRefs[quotes.type].apply(
        (find = oldCtx.symbol, replace = newCtx),
        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
      )

      def extractSimple(body: Expr[Token[?]], ctxManipulation: Expr[CtxManipulation]) = body matchOption:
        case '{ type t <: ValidName; Token.Ignored[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new IgnoredToken[name]($name, $regex, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              '{ new DefinedToken[name]($name, $regex, $ctxManipulation) }

        case '{ type t <: ValidName; Token.apply[t]($value)(using $ctx) } =>
          compileNameAndPattern[t](pattern).map:
            case ('{ type name <: ValidName; $name: name }, regex) =>
              val remapping = createLambda[Remapping] { case (methSym, (newCtx: Term) :: Nil) =>
                replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
              }
              '{ new DefinedToken[name]($name, $regex, $ctxManipulation, $remapping) }

      val newTokens = extractSimple(body.asExprOf[Token[?]], '{ CtxManipulation.empty }).orElse{
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation] { case (methSym, (newCtx: Term) :: Nil) =>
                replaceWithNewCtx(newCtx).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
              }
              extractSimple(expr.asExprOf[Token[?]], ctxManipulation)
            case x => raiseShouldNeverBeCalled(x.show)
      }.get

      // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
      newTokens ::: tokens
    case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
  }

  val (definedTokens, ignoredTokens) = tokens.partition(_.isExprOf[DefinedToken[?]]).runtimeChecked

  val parents = List(TypeTree.of[Object], TypeTree.of[Tokenization])

  def decls(cls: Symbol): List[Symbol] = definedTokens.map { case '{ $token: DefinedToken[name] } =>
    Symbol.newVal(
      parent = cls,
      name = typeToString[name],
      tpe = TypeRepr.of[Token[name]],
      flags = Flags.Synthetic,
      privateWithin = Symbol.noSymbol,
    )
  } :+ Symbol.newVal(
    parent = cls,
    name = "tokens",
    tpe = TypeRepr.of[List[Token[?]]],
    flags = Flags.Synthetic | Flags.Override,
    privateWithin = Symbol.noSymbol,
  )

  val cls = Symbol.newClass(Symbol.spliceOwner, Symbol.freshName("$anon"), parents.map(_.tpe), decls, None)

  val body = definedTokens.collect { case '{ $token: DefinedToken[name] } =>
    ValDef(cls.fieldMember(typeToString[name]), Some(token.asTerm.changeOwner(cls.fieldMember(typeToString[name]))))
  } :+ ValDef(
    cls.fieldMember("tokens"),
    Some {
      val declaredTokens =
        cls.fieldMembers.filter(_.typeRef.widen <:< TypeRepr.of[Token[?]]).map(Select(This(cls), _).asExprOf[Token[?]])

      Expr.ofList(ignoredTokens ++ declaredTokens).asTerm.changeOwner(cls.fieldMember("tokens"))
    },
  )

  val clsDef = ClassDef(cls, parents, body)

  val refinedTpe = definedTokens.foldLeft(TypeRepr.of[Tokenization]) {
    case (tpe, '{ $token: DefinedToken[name] }) => Refinement(tpe, typeToString[name], TypeRepr.of[Token[name]])
    case _ => raiseShouldNeverBeCalled()
  }
  refinedTpe.asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization & refinedTpe]
}
