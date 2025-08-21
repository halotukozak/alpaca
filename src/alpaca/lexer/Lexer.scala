package alpaca.lexer

import alpaca.{dbg, treeInfo}

import scala.annotation.{experimental, tailrec}
import scala.quoted.*

type LexerDefinition = PartialFunction[String, Token[?]]
@experimental
transparent inline def lexer(inline rules: Ctx ?=> LexerDefinition): Tokenization = ${ lexerImpl('{ rules }) }

@experimental
private def lexerImpl(rules: Expr[Ctx ?=> LexerDefinition])(using quotes: Quotes): Expr[Tokenization] = {
  import quotes.reflect.*

  final class ReplaceRef(queries: (find: Symbol, replace: Term)*) extends TreeMap {
    override def transformTerm(tree: Term)(owner: Symbol): Term =
      queries.indexWhere(_.find == tree.symbol) match
        case -1 => super.transformTerm(tree)(owner)
        case idx => queries(idx).replace
  }

  def stringToType(str: String): Type[ValidName] =
    ConstantType(StringConstant(str)).asType.asInstanceOf[Type[ValidName]]

  val tokens: List[Expr[Token[?]]] = rules.asTerm.underlying match
    case Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) =>
      cases.foldLeft(List.empty[Expr[Token[?]]]) {
        case (tokens, CaseDef(pattern, None, body)) =>
          // todo: make compile-time check if pattern exists, also for regex overlapping https://github.com/halotukozak/alpaca/issues/41
          def withToken(token: Expr[Token[?]]) = token :: tokens

          def compiledPattern(pattern: Tree): Expr[String] = pattern match
            case Bind(_, Literal(StringConstant(str))) => Expr(str)
            case Bind(_, alternatives: Alternatives) => compiledPattern(alternatives)
            case Literal(StringConstant(str)) => Expr(str)
            case Alternatives(alternatives) => Expr(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("|"))
            case x =>
              s"""compiledPattern unsupported ${treeInfo(x)}""".dbg

          def compiledName[T: Type](pattern: Tree): Type[ValidName] = TypeRepr.of[T] match
            case ConstantType(StringConstant(str)) => stringToType(str)
            case _ =>
              @tailrec def loop(pattern: Tree): Type[ValidName] = pattern match
                case Bind(_, Literal(StringConstant(str))) => stringToType(str)
                case Bind(_, alternatives: Alternatives) => loop(alternatives)
                case Literal(StringConstant(str)) => stringToType(str)
                case Alternatives(alternatives) =>
                  stringToType(alternatives.map(compiledPattern).map(_.valueOrAbort).mkString("_or_"))
                case x =>
                  s"""compiledPattern unsupported ${treeInfo(x)}""".dbg
              loop(pattern)

          @tailrec def extract(body: Expr[?])(ctxManipulation: Expr[(Ctx => Unit) | Null] = '{ null })
            : List[Expr[Token[?]]] =
            body match
              case '{ type t <: ValidName; Token.Ignored[t](using $ctx) } =>
                compiledName[t](pattern) match
                  case '[type name <: ValidName; name] =>
                    val regex = compiledPattern(pattern)
                    withToken('{
                      new IgnoredToken[name](compiletime.constValue[name], $regex, $ctxManipulation)
                    })
              case '{ type t <: ValidName; Token.apply[t](using $ctx) } =>
                compiledName[t](pattern) match
                  case '[type name <: ValidName; name] =>
                    val regex = compiledPattern(pattern)
                    withToken('{ new TokenImpl[name](compiletime.constValue[name], $regex, $ctxManipulation) })
              case '{ type t <: ValidName; Token.apply[t]($value)(using $ctx) } =>
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
                        case _ => report.errorAndAbort("Invalid number of parameters in lambda")
                      },
                    ).asExprOf[Ctx => Any]
                    withToken('{ new TokenImpl[name](compiletime.constValue[name], $regex, $ctxManipulation) })
              case complex =>
                complex.asTerm match
                  case Block(statements, expr) =>
                    val ctxManipulation = Lambda(
                      Symbol.spliceOwner,
                      MethodType("ctx" :: Nil)(_ => TypeRepr.of[Ctx] :: Nil, _ => TypeRepr.of[Unit]),
                      {
                        case (methSym, (newCtx: Term) :: Nil) =>
                          ReplaceRef(
                            (find = oldCtx.symbol, replace = newCtx),
                            (find = pattern.symbol, replace = Select.unique(newCtx, "text")),
                          ).transformTerm(Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())))(
                            methSym,
                          )
                        case _ => report.errorAndAbort("Invalid number of parameters in lambda")
                      },
                    ).asExprOf[Ctx => Unit]
                    extract(expr.asExpr)(ctxManipulation)
                  case x =>
                    s"""Unsupported tree:
                   |${treeInfo(x)}""".dbg

          extract(body.asExpr)()
        case (tokens, CaseDef(pattern, Some(guard), body)) => report.errorAndAbort("Guards are not supported yet")
      }
    case _ =>
      report.errorAndAbort("Lexer definition must be a lambda function")

  val parents = List(TypeTree.of[Object], TypeTree.of[Tokenization])

  val getName: Expr[Token[?]] => ValidName = { case '{ $token: Token[name] } =>
    TypeRepr.of[name] match
      case ConstantType(StringConstant(str)) => str // todo: probably we can make less work to get it right
      case _ => report.errorAndAbort(s"Name is not a constant string. Token: ${token.show}")
  }

  def decls(cls: Symbol): List[Symbol] = tokens.map { case '{ $token: Token[name] } =>
    Symbol.newVal(
      parent = cls,
      name = getName(token),
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

  val body = tokens.map { token =>
    ValDef(cls.fieldMember(getName(token)), Some(token.asTerm))
  } :+ ValDef(
    cls.fieldMember("tokens"),
    Some {
      val params =
        cls.fieldMembers.filter(_.typeRef.widen <:< TypeRepr.of[Token[?]]).map(Select(This(cls), _).asExprOf[Token[?]])

      Expr.ofList(params).asTerm
    },
  )

  val clsDef = ClassDef(cls, parents, body)

  val refinedTpe = tokens.foldLeft(TypeRepr.of[Tokenization]) { case (tpe, token) =>
    val name = getName(token)
    Refinement(tpe, name, TypeRepr.of[Token].appliedTo(TypeRepr.of(using stringToType(name))))
  }
  refinedTpe.asType match
    case '[refinedTpe] =>
      val newCls = Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[refinedTpe])

      Block(clsDef :: Nil, newCls).asExprOf[Tokenization & refinedTpe]
}
