package example.runtime

import Numeric.Implicits.infixNumericOps

import scala.reflect.TypeTest

opaque type Vector = Array[Number]

object Vector:
  def apply(source: Array[Number]): Vector = source

  def apply(source: Iterable[Number]): Vector = source.toArray

  given TypeTest[Any, Vector]:
    def unapply(x: Any): Option[x.type & Vector] = x match
      case vector: Array[?] => Some(vector.asInstanceOf[x.type & Vector])
      case _ => None

  extension (source: Vector)
    def update(index: Int, value: Number): Unit =
      source(index) = value

    def apply(index: Int): Number =
      source(index)

    def `.+`(other: Vector): Vector =
      for (v, w) <- source zip other yield v + w

    def `.-`(other: Vector): Vector =
      for (v, w) <- source zip other yield v - w

    def `.*`(other: Vector): Vector =
      for (v, w) <- source zip other yield v * w

    def `./`(other: Vector): Vector =
      for (v, w) <- source zip other yield v / w

    def unary_- : Vector =
      for n <- source yield -n

    inline def toArray: Array[Number] = source
