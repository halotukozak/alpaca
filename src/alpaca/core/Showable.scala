package alpaca.core

import alpaca.core.Showable.*

import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes}
import scala.NamedTuple.NamedTuple

trait Showable[-T]:
  extension (t: T) def show: Shown

extension (sc: StringContext) def show(args: Showable.Shown*): String = sc.s(args*)

object Showable {
  opaque type Shown <: String = String

  given [T: Showable]: Conversion[T, Shown] = _.show

  given Showable[String] = x => x
  given Showable[Int] = _.toString

  // todo: add names
  given [N <: Tuple, V <: Tuple: Showable]: Showable[NamedTuple[N, V]] = _.toTuple.show

  given [T](using quotes: Quotes): Showable[Expr[T]] = expr =>
    import quotes.reflect.*
    expr.asTerm.show(using Printer.TreeShortCode)

  given [A: Showable, B: Showable]: Showable[(A, B)] = (a, b) => show"$a -> $b"

  extension [C[X] <: Iterable[X], T: Showable](c: C[T])
    def mkShow(start: String, sep: String, end: String): Shown =
      c.map(_.show).mkString(start, sep, end)
    def mkShow(sep: String): Shown = mkShow("", sep, "")
    def mkShow: Shown = mkShow("")

  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = (t: T) =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList
    val shown = showables.zip(values).map { case (s, v) => s.show(v) }
    s"$name(${fields.zip(shown).map { case (f, v) => s"$f: $v" }.mkString(", ")})"
}
