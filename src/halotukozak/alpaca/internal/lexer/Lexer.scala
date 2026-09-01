package halotukozak
package alpaca
package internal
package lexer

import alpaca.Token as TokenDef
import halotukozak.regex.{Regex, RegexParser, Subset, TokenMatcher}

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.annotation.{publicInBinary, switch}
import scala.reflect.NameTransformer

// $COVERAGE-OFF$
def lexerImpl[Ctx <: LexerCtx: Type, lexemeFields <: AnyNamedTuple: Type](
  rules: Expr[Ctx ?=> LexerDefinition[Ctx]],
  onTokenMatch: Expr[(Token[?, Ctx, ?], String, Ctx) => Ctx],
  errorHandling: Expr[ErrorHandling[Ctx]],
  empty: Expr[Empty[Ctx]],
)(using quotes: Quotes,
): Expr[Tokenization[Ctx] { type LexemeFields = lexemeFields }] =
  import quotes.reflect.*

  type TokenRefn = lexer.Token[?, Ctx, ?] { type LexemeTpe = Lexeme[?, ?] withFields lexemeFields }

  val compileNameAndPattern = new CompileNameAndPattern[quotes.type]
  val createLambda = new CreateLambda[quotes.type]
  val replaceRefs = new ReplaceRefs[quotes.type]
  val rewriteCtxMutations = new RewriteCtxMutations[quotes.type]

  val Lambda(oldCtx :: Nil, Lambda(_, Match(_, cases: List[CaseDef]))) = rules.asTerm.underlying.runtimeChecked

  if cases.isEmpty then report.errorAndAbort("Lexer definition must contain at least one case")

  val (tokens, infos, ignoredFlags) = cases.foldLeft(
    (
      tokens = List.empty[(expr: Expr[lexer.Token[?, Ctx, ?] & TokenRefn], name: ValidName)],
      infos = List.empty[TokenInfo],
      ignored = List.empty[Boolean],
    ),
  ):
    case ((accTokens, accInfos, accIgnored), CaseDef(tree, None, body)) =>
      def replaceWithNewCtx(newCtx: Term) = replaceRefs(
        (find = oldCtx.symbol, replace = newCtx),
        (find = tree.symbol, replace = '{ ${ newCtx.asExprOf[Ctx] }.lastRawMatched }.asTerm),
      )

      def extractSimple(ctxManipulation: Expr[CtxManipulation[Ctx]]): PartialFunction[
        Expr[TokenDef[ValidName, Ctx, Any]],
        List[(info: TokenInfo, ignored: Boolean, expr: Expr[lexer.Token[?, Ctx, ?]])],
      ] =
        case '{ Token.Ignored(using $_) } =>
          compileNameAndPattern[Nothing](tree).map:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (
                info = tokenInfo,
                ignored = true,
                expr = '{ IgnoredToken[name, Ctx](${ Expr(tokenInfo) }, $ctxManipulation) },
              )
            case other =>
              raiseShouldNeverBeCalled(other)

        case '{ type name <: ValidName; Token[name](using $_) } =>
          compileNameAndPattern[name](tree).map:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (
                info = tokenInfo,
                ignored = false,
                expr = '{ DefinedToken[name, Ctx, Unit](${ Expr(tokenInfo) }, $ctxManipulation, _ => ()) },
              )
            case other =>
              raiseShouldNeverBeCalled(other)

        case '{ type name <: ValidName; Token[name]($value: String)(using $_) } if value.asTerm.symbol == tree.symbol =>
          compileNameAndPattern[name](tree).map:
            case ('[type name <: ValidName; name], tokenInfo) =>
              (
                info = tokenInfo,
                ignored = false,
                expr = '{ DefinedToken[name, Ctx, String](${ Expr(tokenInfo) }, $ctxManipulation, _.lastRawMatched) },
              )
            case other =>
              raiseShouldNeverBeCalled(other)

        case '{ type name <: ValidName; Token[name]($value: value)(using $_) } =>
          compileNameAndPattern[name](tree).map:
            case ('[type name <: ValidName; name], tokenInfo) =>
              // we need to widen here to avoid weird types
              TypeRepr.of[value].widen.asType match
                case '[result] =>
                  val remapping = createLambda[Ctx => result]:
                    case (methSym, (newCtx: Term) :: Nil) =>
                      val withNewCtx = replaceWithNewCtx(newCtx).transformTerm(value.asTerm)(methSym)
                      rewriteCtxMutations(newCtx.symbol)(withNewCtx)(methSym)
                  (
                    info = tokenInfo,
                    ignored = false,
                    expr = '{ DefinedToken[name, Ctx, result](${ Expr(tokenInfo) }, $ctxManipulation, $remapping) },
                  )
            case (_, tokenInfo) =>
              raiseShouldNeverBeCalled[(info: TokenInfo, ignored: Boolean, expr: Expr[lexer.Token[?, Ctx, ?]])](
                tokenInfo,
              )

      val triples = extractSimple('{ (c: Ctx) => c })
        .lift(body.asExprOf[TokenDef[ValidName, Ctx, Any]])
        .orElse:
          body match
            case Block(statements, expr) =>
              val ctxManipulation = createLambda[CtxManipulation[Ctx]]:
                case (methSym, (newCtx: Term) :: Nil) =>
                  val ctxVar = Symbol.newVal(methSym, "$ctx", TypeRepr.of[Ctx], Flags.Mutable, Symbol.noSymbol)
                  val withNewCtx = replaceWithNewCtx(Ref(ctxVar)).transformTerm(
                    Block(statements.map(_.changeOwner(methSym)), Literal(UnitConstant())),
                  )(methSym)
                  val rewritten = rewriteCtxMutations(ctxVar)(withNewCtx)(methSym)
                  Block(List(ValDef(ctxVar, Some(newCtx))), Block(List(rewritten), Ref(ctxVar)))

              extractSimple(ctxManipulation).lift(expr.asExprOf[TokenDef[ValidName, Ctx, Any]])
        .getOrElse:
          raiseShouldNeverBeCalled[
            List[(info: TokenInfo, ignored: Boolean, expr: Expr[lexer.Token[?, Ctx, ?]])],
          ](body)

      val infos = triples.map(_.info)
      val ignoredFlags = triples.map(_.ignored)
      val tokens = triples.map(_.expr)

      (
        tokens = accTokens ::: tokens.map:
          case '{ type name <: ValidName; type tokenTpe <: lexer.Token[name, Ctx, ?]; $token: tokenTpe } =>
            (expr = '{ $token.asInstanceOf[tokenTpe & TokenRefn] }, name = ValidName.from[name])
        ,
        infos = accInfos ::: infos,
        ignored = accIgnored ::: ignoredFlags,
      )

    case (_, CaseDef(_, Some(_), body)) => report.errorAndAbort("Guards are not supported yet")

  infos
    .groupBy(_.name)
    .iterator
    .filter(_._2.sizeIs > 1)
    .foreach: (name, duplicates) =>
      report.errorAndAbort(
        show"Token name \"$name\" is defined ${duplicates.size.toString} times. Combine the patterns into a single case using alternatives, e.g.: case x @ (\"pattern1\" | \"pattern2\") => Token[x]",
      )

  val parsedRegexes = infos.map: info =>
    RegexParser.parse(info.pattern) match
      case Right(regex) => regex
      case Left(err) => report.errorAndAbort(err.toString)

  SubsetChecker.checkRegexes(
    for (info, regex) <- infos.zip(parsedRegexes)
    yield (info.pattern, Subset.of(regex).withAnySuffix),
  )

  GrammarExport.maybeWrite(
    // Symbol.spliceOwner is a synthetic "macro" method that dotty introduces to host the
    // transparent inline def's expansion; the val this `lexer{...}` call is actually bound
    // to (e.g. `val CalcLexer = lexer{...}`) is always one owner hop further up. The source
    // file name and line are prefixed too: the same val name (e.g. `CalcLexer`) can recur
    // across multiple test/example files, or even within one file at different scopes (one
    // at class level, another inside a test block), and would otherwise collide in a shared
    // export directory, silently overwriting each other.
    {
      val sourceFileName = Position.ofMacroExpansion.sourceFile.path.split("[/\\\\]").last.stripSuffix(".scala")
      val lexerName = Symbol.spliceOwner.owner.name.stripSuffix("$")
      val line = Position.ofMacroExpansion.startLine + 1
      s"$sourceFileName.$lexerName@L$line"
    },
    infos.zip(ignoredFlags).map((info, ignored) => (name = info.name, pattern = info.pattern, ignored = ignored)),
  )

  val fields = tokens.map((expr, name) => (name, expr.asTerm.tpe))
  val types = Refined(
    TypeTree.of[Any],
    fields.map: (name, tpe) =>
      TypeDef(Symbol.newTypeAlias(Symbol.spliceOwner, name, Flags.EmptyFlags, tpe, Symbol.noSymbol)),
    defn.AnyClass,
  ).tpe

  def selectDynamicImpl(fieldName: Expr[String])(using Quotes) = Match(
    '{ $fieldName: @switch }.asTerm,
    tokens.map: (expr, name) =>
      CaseDef(Literal(StringConstant(NameTransformer.encode(name))), None, expr.asTerm),
  ).asExprOf[lexer.Token[?, Ctx, ?]]

  (refinementTpeFrom(fields).asType, fieldsTpeFrom(fields).asType, types.asType).runtimeChecked match
    case ('[refinedTpe], '[fields], '[types]) =>
      val tokensExpr = Expr.ofList(tokens.map(_.expr))
      val matcherExpr = '{ TokenMatcher.fromRegexes(${ Varargs(parsedRegexes.map(Expr(_))) }*) }

      '{
        {
          new Tokenization[Ctx]($onTokenMatch)(using $errorHandling, $empty):
            @publicInBinary
            override private[alpaca] val tokens: List[lexer.Token[?, Ctx, ?]] = $tokensExpr

            override def selectDynamic(name: String): lexer.Token[?, Ctx, ?] = ${ selectDynamicImpl('{ name }) }

            override protected val matcher: TokenMatcher = $matcherExpr
        }.asInstanceOf[Tokenization[Ctx] { type LexemeFields = lexemeFields; type Fields = fields } & refinedTpe & types]
      }
// $COVERAGE-ON$
