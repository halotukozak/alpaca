package alpaca
package internal

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
private[internal] trait Showable[-T]:
  /**
   * Extension method to convert a value to its string representation.
   *
   * @return the string representation of the value
   */
  extension (t: T) def show: Shown

/** String interpolator for values that have Showable instances. */
extension (sc: StringContext) private[internal] def show(args: Shown*): Shown = sc.s(args*)

/**
 * An opaque type representing a string that has been shown.
 *
 * Used to ensure type safety in string interpolation.
 */
opaque private[internal] type Shown <: String = String

object Shown {

  /**
   * Implicit conversion from any Showable type to Shown.
   *
   * @tparam T the type with a Showable instance
   */
  given [T: Showable]: Conversion[T, Shown] = _.show
}

private[internal] object Showable {

  def fromToString[T]: Showable[T] = _.toString

  /** Showable instance for String (identity). */
  given Showable[String] = _.asInstanceOf[Shown]

  /** Showable instance for Int. */
  given Showable[Int] = fromToString

  /** Showable instance for nullable types. */
  given [T: Showable as showable]: Showable[T | Null] =
    case null => ""
    case value: T => showable.show(value)

  // todo: add names
  given [N <: Tuple, V <: Tuple: Showable]: Showable[NamedTuple[N, V]] = _.toTuple.show

  given [T](using quotes: Quotes): Showable[Expr[T]] =
    import quotes.reflect.*
    expr => expr.asTerm.show

  given (using quotes: Quotes): Showable[quotes.reflect.Tree] = quotes.reflect.Printer.TreeShortCode.show(_)

  given [A: Showable, B: Showable]: Showable[(A, B)] = (a, b) => show"$a : $b"

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
    val shown = showables.zip(values).map(_.show(_))
    s"$name(${fields.zip(shown).map((f, v) => s"$f: $v").mkString(", ")})"
}

extension [C[X] <: Iterable[X], T: Showable](c: C[T])

  /**
   * Creates a string representation with custom start, separator, and end strings.
   *
   * @param start the string to prepend
   * @param sep   the separator between elements
   * @param end   the string to append
   * @return the formatted string
   */
  private[internal] def mkShow(start: String, sep: String, end: String): Shown =
    c.map(_.show).mkString(start, sep, end)

  /**
   * Creates a string representation with a custom separator.
   *
   * @param sep the separator between elements
   * @return the formatted string
   */
  private[internal] def mkShow(sep: String): Shown = mkShow("", sep, "")

  /**
   * Creates a string representation with elements concatenated.
   *
   * @return the concatenated string
   */
  private[internal] def mkShow: Shown = mkShow("")
