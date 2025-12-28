package alpaca
package internal
package lexer

import scala.deriving.Mirror
import NamedTuple.AnyNamedTuple

trait LexerRefinement:
  type Self <: LexerCtx
  type Lexeme <: alpaca.internal.lexer.Lexeme
  final type Token = alpaca.internal.lexer.Token[Self] { type LexemeTpe = Lexeme }

object LexerRefinement:
  transparent inline given derived[Ctx <: LexerCtx & Product: Mirror.Of as m]: (Ctx has LexerRefinement) =
    ${ derivedImpl[Ctx, m.MirroredElemLabels, m.MirroredElemTypes] }

  def derivedImpl[Ctx <: LexerCtx: Type, Labels <: Tuple: Type, Types <: Tuple: Type](using quotes: Quotes)
    : Expr[Ctx has LexerRefinement] =
    import quotes.reflect.*
    def extractAll(tup: Type[? <: Tuple]): List[TypeRepr] = tup match
      case '[h *: t] => TypeRepr.of[h] :: extractAll(Type.of[t])
      case '[EmptyTuple] => Nil

    val labels = extractAll(Type.of[Labels])
    val types = extractAll(Type.of[Types])

    labels
      .zip(types)
      .unsafeFoldLeft(TypeRepr.of[Lexeme]):
        case (acc, (ConstantType(StringConstant(label)), tpe)) => Refinement(acc, label, tpe)
      .asType
      .match
        case '[refinementTpe] =>
          '{
            dummy[
              LexerRefinement {
                type Self = Ctx
                type Lexeme = { type Fields = Tuple.Zip[Labels, Types] } & refinementTpe
              },
            ]
          }
