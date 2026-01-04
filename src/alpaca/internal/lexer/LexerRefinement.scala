package alpaca
package internal
package lexer

import scala.NamedTuple.AnyNamedTuple
import scala.annotation.implicitNotFound
import scala.deriving.Mirror

@implicitNotFound("Cannot derive LexerRefinement for ${Ctx}. Ensure it is a case class.")
trait LexerRefinement[Ctx <: LexerCtx]:
  type Lexeme <: alpaca.internal.lexer.Lexeme[?, ?]
  final type Token = alpaca.internal.lexer.Token[?, ?, Ctx] { type LexemeTpe = Lexeme }

object LexerRefinement:
  transparent inline given derived[Ctx <: LexerCtx & Product: Mirror.Of as m]: LexerRefinement[Ctx] =
    ${ derivedImpl[Ctx, m.MirroredElemLabels, m.MirroredElemTypes] }

  def derivedImpl[Ctx <: LexerCtx: Type, Labels <: Tuple: Type, Types <: Tuple: Type](using quotes: Quotes)
    : Expr[LexerRefinement[Ctx]] = withTimeout:
    import quotes.reflect.*
    logger.trace(show"deriving LexerRefinement for ${Type.of[Ctx]}")

    def extractAll(tup: Type[? <: Tuple]): List[TypeRepr] = tup match
      case '[h *: t] => TypeRepr.of[h] :: extractAll(Type.of[t])
      case '[EmptyTuple] => Nil

    val labels = extractAll(Type.of[Labels])
    val types = extractAll(Type.of[Types])

    logger.trace(show"extracting ${labels.size} fields for Lexeme refinement")

    labels
      .zip(types)
      .unsafeFoldLeft(TypeRepr.of[Lexeme[?, ?]]):
        case (acc, (ConstantType(StringConstant(label)), tpe)) =>
          logger.trace(show"refining Lexeme with field $label: $tpe")
          Refinement(acc, label, tpe)
      .asType
      .match
        case '[refinementTpe] =>
          '{
            dummy[
              LexerRefinement[Ctx] {
                type Lexeme = { type Fields = Tuple.Zip[Labels, Types] } & refinementTpe
              },
            ]
          }
