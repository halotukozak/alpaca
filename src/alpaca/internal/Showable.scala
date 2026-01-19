package alpaca
package internal

import scala.NamedTuple.NamedTuple
import scala.annotation.targetName

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
  extension (t: T)(using Log) def show: Shown

/** String interpolator for values that have Showable instances. */
extension (sc: StringContext) private[internal] def show(args: Shown*): Shown = sc.s(args*)

/**
 * An opaque type representing a string that has been shown.
 *
 * Used to ensure type safety in string interpolation.
 */
opaque into private[internal] type Shown <: String = String

object Shown:

  /**
   * Implicit conversion from any Showable type to Shown.
   *
   * @tparam T the type with a Showable instance
   */
  given [T: Showable] => Log => Conversion[T, Shown] = _.show

private[internal] object Showable:
  def apply[T](func: Log ?=> T => Shown): Showable[T] = new:
    extension (t: T)(using Log) override def show: Shown = func(t)

  given Conversion[String, Shown] = _.asInstanceOf[Shown]

  /** Showable instance for String (identity). */
  given Showable[String] = Showable(_.asInstanceOf[Shown])

  /** Showable instance for Int. */
  given Showable[Int] = fromToString

  /** Showable instance for Long. */
  given Showable[Long] = fromToString

  /** Showable instance for Double. */
  given Showable[Double] = fromToString

  /** Showable instance for Float. */
  given Showable[Float] = fromToString

  /** Showable instance for Boolean. */

  given Showable[Boolean] = fromToString

  /** Showable instance for Char. */
  given Showable[Char] = fromToString

  def fromToString[T]: Showable[T] = Showable(_.toString)

  // todo: add names
  given [N <: Tuple, V <: Tuple: Showable] => Showable[NamedTuple[N, V]] = Showable(_.toTuple.show)

  given [T] => (quotes: Quotes) => Showable[Expr[T]] = Showable:
    import quotes.reflect.*
    expr => expr.asTerm.show

  given [T] => (quotes: Quotes) => Showable[quotes.reflect.TypeRepr] = Showable: tpe =>
    val short = show"[${quotes.reflect.Printer.TypeReprShortCode.show(tpe)}]"
    if summon[Log].debugSettings.enableVerboseNames then
      show"$short(${quotes.reflect.Printer.TypeReprStructure.show(tpe)})"
    else short

  given [T] => (quotes: Quotes) => Showable[Type[T]] = Showable: tpe =>
    import quotes.reflect.*
    TypeRepr.of(using tpe).show

  @targetName("given_Type_bounds")
  given [T] => (quotes: Quotes) => Showable[Type[? <: T]] = Showable: tpe =>
    import quotes.reflect.*
    TypeRepr.of(using tpe).show

  given (quotes: Quotes) => Showable[quotes.reflect.Tree] = Showable:
    quotes.reflect.Printer.TreeShortCode.show(_)

  given (quotes: Quotes) => Showable[quotes.reflect.Symbol] = Showable: symbol =>
    symbol.name

  given [A: Showable, B: Showable] => Showable[(A, B)] = Showable: (a, b) =>
    show"$a : $b"

  /**
   * Automatically derives a Showable instance for Product types (case classes).
   *
   * Creates a representation in the form: `ClassName(field1: value1, field2: value2)`.
   *
   * @tparam T the Product type to derive Showable for
   * @param m the Mirror.ProductOf for type T
   * @return a Showable instance
   */
  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = Showable: t =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList.asFlow
    val shown = showables.asFlow.zip(values).map(_.show(_))
    show"$name(${fields.asFlow.zip(shown).map((f, v) => s"$f: $v").mkShow(", ")})"

extension [C[X] <: Iterable[X], T: Showable](c: C[T])(using Log)

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

extension [T: Showable](it: Iterator[T])(using Log)
  private[internal] def mkShow(start: String, sep: String, end: String): Shown = it.map(_.show).mkString(start, sep, end)
  private[internal] def mkShow(sep: String): Shown = mkShow("", sep, "")
  private[internal] def mkShow: Shown = mkShow("")

extension [T: Showable](it: Flow[T])(using Log)
  private[internal] def mkShow(start: String, sep: String, end: String): Shown = it.map(_.show).mkString(start, sep, end)
  private[internal] def mkShow(sep: String): Shown = mkShow("", sep, "")
  private[internal] def mkShow: Shown = mkShow("")
