package alpaca.core

import scala.deriving.Mirror

trait Showable[T] {
  def show(t: T): String
}

extension [T](t: T)(using show: Showable[T]) def show: String = show.show(t)
extension (sc: StringContext) def show(args: Showable.Shown*): String = sc.s(args*)

object Showable {
  opaque type Shown = String

  given [T: Showable]: Conversion[T, Shown] = _.show

  given Showable[String] = x => x
  given Showable[Int] = _.toString

  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = (t: T) =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList
    val shown = showables.zip(values).map { case (s, v) => s.show(v) }
    s"$name(${fields.zip(shown).map { case (f, v) => s"$f: $v" }.mkString(", ")})"
}

extension [C[X] <: Iterable[X], T: Showable](coll: C[T]) def mkShow: String = coll.map(_.show).mkString
