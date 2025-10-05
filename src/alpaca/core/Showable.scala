package alpaca.core

import scala.deriving.Mirror

trait Showable[T]:
  extension (t: T) def show: String

extension (sc: StringContext) def show(args: Showable.Shown*): String = sc.s(args*)

object Showable {
  opaque type Shown = String

  given [T: Showable]: Conversion[T, Shown] = _.show

  given Showable[String] = x => x
  given Showable[Int] = _.toString

  given [C[X] <: Iterable[X], T: Showable]: Showable[C[T]] = _.map(_.show).mkString

  inline def derived[T <: Product](using m: Mirror.ProductOf[T & Product]): Showable[T] = (t: T) =>
    val name = compiletime.constValue[m.MirroredLabel]
    val fields = compiletime.constValueTuple[m.MirroredElemLabels].toList
    val showables =
      compiletime.summonAll[Tuple.Map[m.MirroredElemTypes, Showable]].toList.asInstanceOf[List[Showable[Any]]]
    val values = Tuple.fromProductTyped(t).toList
    val shown = showables.zip(values).map { case (s, v) => s.show(v) }
    s"$name(${fields.zip(shown).map { case (f, v) => s"$f: $v" }.mkString(", ")})"
}
