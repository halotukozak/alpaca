package alpaca.lexer

import alpaca.*

import scala.annotation.{experimental, tailrec}
import scala.quoted.*

type LexerDefinition = PartialFunction[String, Token[?]]
@experimental
transparent inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenization = ${ lexerImpl('{ rules }) }

@experimental
private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenization] = {
  import quotes.reflect.*

  def raiseShouldNeverBeCalled(): Nothing = report.errorAndAbort(s"It should never happen")

  final class ReplaceRef(queries: (find: Symbol, replace: Term)*) extends TreeMap {
    // skip NoSymbol
    private val filtered = queries.view.filterNot(_.find.isNoSymbol)

    override def transformTerm(tree: Term)(owner: Symbol): Term =
      filtered
        .collectFirst { case (find, replace) if find == tree.symbol => replace }
        .getOrElse(super.transformTerm(tree)(owner))
  }

  def stringToType(str: String): Type[ValidName] =
    ConstantType(StringConstant(str)).asType.asInstanceOf[Type[ValidName]]

  def typeToString[Name <: ValidName: Type]: ValidName =
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str // todo: probably we can make less work to get it right
      case _ => raiseShouldNeverBeCalled()

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked
  val tokens: List[Expr[Token[?]]] = cases.foldLeft(List.empty[Expr[Token[?]]]) {
    case (tokens, CaseDef(pattern, None, body)) =>
      // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
      def withToken(token: Expr[Token[?]]) = token :: tokens

      def compiledPattern(pattern: Tree): Expr[String] = pattern match
        case Bind(_, Literal(StringConstant(str))) => Expr(str)
        case Bind(_, alternatives: Alternatives) => compiledPattern(alternatives)
        case Literal(StringConstant(str)) => Expr(str)
        case Alternatives(alternatives) => Expr(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("|"))
        case _ => raiseShouldNeverBeCalled()

      def compiledName[T: Type](pattern: Tree): Type[ValidName] = TypeRepr.of[T] match
        case ConstantType(StringConstant(str)) => stringToType(str)
        case _ =>
          @tailrec def loop(pattern: Tree): Type[ValidName] = pattern match
            case Bind(_, Literal(StringConstant(str))) => stringToType(str)
            case Bind(_, alternatives: Alternatives) => loop(alternatives)
            case Literal(StringConstant(str)) => stringToType(str)
            case Alternatives(alternatives) =>
              stringToType(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("_or_"))
            case _ => raiseShouldNeverBeCalled()
          loop(pattern)

      @tailrec def extract(body: Expr[?])(ctxManipulation: Expr[CtxManipulation]): List[Expr[Token[?]]] =
        body match
          case '{ type t <: ValidName; Token.Ignored[t](using $ctx) } =>
            compiledName[t](pattern) match
              case '[type name <: ValidName; name] =>
                val regex = compiledPattern(pattern)
                withToken('{ new IgnoredToken[name](compiletime.constValue[name], $regex, $ctxManipulation) })
          case '{ type t <: ValidName; Token.apply[t](using $ctx: Ctx) } =>
            compiledName[t](pattern) match
              case '[type name <: ValidName; name] =>
                val regex = compiledPattern(pattern)
                withToken('{ new TokenImpl[name](compiletime.constValue[name], $regex, $ctxManipulation) })
          case '{ type t <: ValidName; Token.apply[t]($value: v)(using $ctx: Ctx) } =>
            compiledName[t](pattern) match
              case '[type name <: ValidName; name] =>
                val regex = compiledPattern(pattern)
                val remapping = Lambda(
                  Symbol.spliceOwner,
                  MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Any]),
                  {
                    case (methSym, (newCtx: Term) :: Nil) =>
                      ReplaceRef(
                        (find = oldCtx.symbol, replace = newCtx),
                        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
                      ).transformTerm(value.asTerm)(methSym)
                    case _ => raiseShouldNeverBeCalled()
                  },
                ).asExprOf[Ctx => Any]

                withToken('{ new TokenImpl[name](compiletime.constValue[name], $regex, $ctxManipulation) })
          case complex =>
            complex.asTerm match
              case Block(statements, expr) =>
                val ctxManipulation = Lambda(
                  Symbol.spliceOwner,
                  MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Ctx]),
                  {
                    case (methSym, (newCtx: Term) :: Nil) =>
                      ReplaceRef(
                        (find = oldCtx.symbol, replace = newCtx),
                        (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
                      ).transformTerm(Block(statements.map(_.changeOwner(methSym)), newCtx))(methSym)
                    case _ => raiseShouldNeverBeCalled()
                  },
                ).asExprOf[CtxManipulation]

                extract(expr.asExpr)(ctxManipulation)
              case _ => raiseShouldNeverBeCalled()

      extract(body.asExpr)('{ _.copy() })
    case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
  }

  val nonIgnoredToken: List[Expr[TokenImpl[?]]] = tokens.collect { case '{ $token: TokenImpl[name] } => token }

  val parents = List(TypeTree.of[Object], TypeTree.of[Tokenization])

  def decls(cls: Symbol): List[Symbol] = nonIgnoredToken.map { case '{ $token: TokenImpl[name] } =>
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

  val body = nonIgnoredToken.collect { case '{ $token: TokenImpl[name] } =>
    ValDef(cls.fieldMember(typeToString[name]), Some(token.asTerm.changeOwner(cls.fieldMember(typeToString[name]))))
  } :+ ValDef(
    cls.fieldMember("tokens"),
    Some {
      val params =
        cls.fieldMembers.filter(_.typeRef.widen <:< TypeRepr.of[Token[?]]).map(Select(This(cls), _).asExprOf[Token[?]])

      Expr.ofList(params).asTerm
    },
  )

  val clsDef = ClassDef(cls, parents, body)

  val refinedTpe = nonIgnoredToken.foldLeft(TypeRepr.of[Tokenization]) {
    case (tpe, '{ $token: TokenImpl[name] }) => Refinement(tpe, typeToString[name], TypeRepr.of[Token[name]])
    case _ => raiseShouldNeverBeCalled()
  }
  refinedTpe.asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization & refinedTpe]
}
