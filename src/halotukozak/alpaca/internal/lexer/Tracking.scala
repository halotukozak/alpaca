package halotukozak
package alpaca
package internal
package lexer

import scala.annotation.implicitNotFound
import scala.compiletime.{erasedValue, summonFrom}

/**
 * A per-token update for a single lexer-context field (a "fragment" such as
 * [[Line]] or [[Column]]).
 *
 * [[Tracking.derive]] applies one `Tracking` instance to each case field of
 * the context whose type provides a `given`, threading the result through a
 * functional `copy`; fields whose type has no `Tracking` given are left
 * untouched by the hook (they only change in rule bodies).
 *
 * This is how tracking composes without inheritance: define a distinct field
 * type and a `given Tracking[YourType]` in its companion, then use it as a
 * context field.
 *
 * @tparam F the fragment field type
 */
@implicitNotFound("No Tracking instance for the context fragment ${F}")
trait Tracking[F]:
  /**
   * @param matched the raw text of the token just matched
   * @param field   the fragment's current value
   * @return the fragment's new value
   */
  def apply(matched: String, field: F): F

object Tracking:

  /**
   * Derives the engine hook run after every token match for a context type:
   * one [[Tracking]] update per case field whose type provides a `given`,
   * followed by the fixed step every context needs regardless of what it
   * tracks -- apply the rule body's context changes, advance the cursor,
   * record the lexeme (see [[advance]]).
   */
  inline def derive[Ctx <: LexerCtx: Mirror.ProductOf as m]: (Token[?, Ctx, ?], String, Ctx) => Ctx =
    Derived(fieldSteps[m.MirroredElemTypes](0))

  /**
   * One `(index, update)` pair per case field that has a `given Tracking`;
   * fields with none are skipped entirely, rather than carried along as a
   * no-op, so that [[Derived.apply]] can tell -- without inspecting the
   * context -- whether there is any field update to do at all.
   */
  inline private def fieldSteps[Elems <: Tuple](index: Int): List[(index: Int, update: (String, Any) => Any)] =
    inline erasedValue[Elems] match
      case _: EmptyTuple => Nil
      case _: (h *: t) =>
        val rest = fieldSteps[t](index + 1)
        summonFrom:
          case tracking: Tracking[`h`] =>
            (index = index, update = (matched, v) => tracking(matched, v.asInstanceOf[h])) :: rest
          case _ => rest

  private val fieldNameCache = new java.util.concurrent.ConcurrentHashMap[Class[?], Array[String]]

  /**
   * Applies the rule body's context changes, advances the cursor, and records
   * the lexeme -- the one step every context needs, regardless of what it
   * tracks.
   */
  private def advance(token: Token[?, LexerCtx, ?], raw: String, ctx: LexerCtx): LexerCtx = token match
    case DefinedToken(info, modifyCtx, remapping) =>
      val c = modifyCtx(ctx).carryEngineStateFrom(ctx)
      val fieldNames = fieldNameCache.computeIfAbsent(c.getClass, _ => c.productElementNames.toArray)
      c.lastLexeme = Lexeme(
        name = info.name,
        value = remapping(c),
        text = raw,
        fieldNames = fieldNames,
        fieldValues = c.productIterator.toArray,
      )
      c

    case IgnoredToken(_, modifyCtx) =>
      modifyCtx(ctx).carryEngineStateFrom(ctx)

  /**
   * Named (not anonymous-per-inline-site) so [[derive]] stays cheap to inline.
   * `derive` is itself `inline`, so this gets constructed from wherever `lexer`
   * is ultimately called -- it can't be `private`/`private[alpaca]` (unlike a
   * plain member, `@publicInBinary` isn't allowed on a class), so it's a
   * plain, unqualified class instead; it's still effectively internal since
   * `internal.lexer` is never exported wholesale, only specific symbols are.
   *
   * All tracked fields are folded into a single `productIterator` snapshot,
   * mutated in place, and rebuilt with one `Mirror.fromProduct` -- one
   * allocation and one reflective reconstruction per token match, regardless
   * of how many fields are tracked, rather than one per field. Contexts with
   * no tracked fields (`steps.isEmpty`) skip the snapshot/rebuild entirely.
   */
  final class Derived[Ctx <: LexerCtx: Mirror.ProductOf as m](
    steps: List[(index: Int, update: (String, Any) => Any)],
  ) extends ((Token[?, Ctx, ?], String, Ctx) => Ctx):
    def apply(token: Token[?, Ctx, ?], raw: String, ctx: Ctx): Ctx =
      val afterFields =
        if steps.isEmpty then ctx
        else
          val values = ctx.productIterator.toArray
          steps.foreach(step => values(step.index) = step.update(raw, values(step.index)))
          val updated = m.fromProduct(Tuple.fromArray(values)).asInstanceOf[Ctx]
          updated.asInstanceOf[LexerCtx].carryEngineStateFrom(ctx)
          updated
      advance(token, raw, afterFields).asInstanceOf[Ctx]
