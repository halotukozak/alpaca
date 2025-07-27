package alpaca

import scala.deriving.Mirror
import scala.util.NotGiven

trait Showable[T] {
  def show(t: T): String
}

extension [T](t: T)(using show: Showable[T]) def show: String = show.show(t)
extension (sc: StringContext) def show(args: Showable.Shown*): String = sc.s(args*)

object Showable {
  opaque type Shown = String

  given [T: Showable]: Conversion[T, Shown] = _.show

  given Showable[String] = Showable.derived
  given Showable[Int] = Showable.derived

  def derived[T](using NotGiven[Mirror.Of[T]]): Showable[T] = _.toString

  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = (t: T) =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList
    val shown = showables.zip(values).map { case (s, v) => s.show(v) }
    s"$name(${(fields zip shown).map { case (f, v) => s"$f: $v" }.mkString(", ")})"
}

extension [C[X] <: Iterable[X], T: Showable](coll: C[T])
  def mkShow(start: String = "", sep: String = "", end: String = ""): String = coll.map(_.show).mkString(start, sep, end)
