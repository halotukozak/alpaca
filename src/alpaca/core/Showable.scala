package alpaca.core

import alpaca.core.Showable.*

import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes}
import scala.NamedTuple.NamedTuple

/**
 * A type class for converting values to their string representation.
 *
 * This trait provides a `show` method that can be used to display values
 * in a human-readable format. It's similar to `toString` but more controlled
 * and composable.
 *
 * @tparam T the type to show
 */
private[alpaca] trait Showable[-T]:
  /**
   * Extension method to convert a value to its string representation.
   *
   * @param t the value to show
   * @return the string representation of the value
   */
  extension (t: T) def show: Shown

/** String interpolator for values that have Showable instances. */
extension (sc: StringContext) private[alpaca] def show(args: Showable.Shown*): Showable.Shown = sc.s(args*)

private[alpaca] object Showable {

  /**
   * An opaque type representing a string that has been shown.
   *
   * Used to ensure type safety in string interpolation.
   */
  opaque type Shown <: String = String

  /**
   * Implicit conversion from any Showable type to Shown.
   *
   * @tparam T the type with a Showable instance
   */
  given [T: Showable]: Conversion[T, Shown] = _.show

  /** Showable instance for String (identity). */
  given Showable[String] = x => x

  /** Showable instance for Int. */
  given Showable[Int] = _.toString

  /** Showable instance for nullable types. */
  given [T: Showable]: Showable[T | Null] =
    case null => ""
    case value => show"$value"

  // todo: add names
  given [N <: Tuple, V <: Tuple: Showable]: Showable[NamedTuple[N, V]] = _.toTuple.show

  given [T](using quotes: Quotes): Showable[Expr[T]] = expr =>
    import quotes.reflect.*
    expr.asTerm.show

  given (using quotes: Quotes): Showable[quotes.reflect.Tree] = quotes.reflect.Printer.TreeShortCode.show(_)

  given [A: Showable, B: Showable]: Showable[(A, B)] = (a, b) => show"$a : $b"

  inline def mkShowTuple[T <: Tuple]: Tuple = inline compiletime.erasedValue[T] match
    case _: EmptyTuple => EmptyTuple
    case _: (h *: t) =>
      compiletime.summonInline[Showable[h]].show(compiletime.constValue[h]) *: mkShowTuple[t]

  extension [C[X] <: Iterable[X], T: Showable](c: C[T])

    /**
     * Creates a string representation with custom start, separator, and end strings.
     *
     * @param start the string to prepend
     * @param sep the separator between elements
     * @param end the string to append
     * @return the formatted string
     */
    def mkShow(start: String, sep: String, end: String): Shown =
      c.map(_.show).mkString(start, sep, end)

    /**
     * Creates a string representation with a custom separator.
     *
     * @param sep the separator between elements
     * @return the formatted string
     */
    def mkShow(sep: String): Shown = mkShow("", sep, "")

    /**
     * Creates a string representation with elements concatenated.
     *
     * @return the concatenated string
     */
    def mkShow: Shown = mkShow("")

  /**
   * Automatically derives a Showable instance for Product types (case classes).
   *
   * Creates a representation in the form: `ClassName(field1: value1, field2: value2)`.
   *
   * @tparam T the Product type to derive Showable for
   * @param m the Mirror.ProductOf for type T
   * @return a Showable instance
   */
  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = (t: T) =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList
    val shown = showables.zip(values).map { case (s, v) => s.show(v) }
    s"$name(${fields.zip(shown).map { case (f, v) => s"$f: $v" }.mkString(", ")})"
}
