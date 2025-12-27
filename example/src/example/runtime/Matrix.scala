package example.runtime

import scala.reflect.TypeTest

opaque type Matrix = Array[Vector]

object Matrix:
  def apply(source: Array[Vector]): Matrix = source

  given TypeTest[Any, Matrix]:
    def unapply(x: Any): Option[x.type & Matrix] = x match
      case matrix: Array[?] => Some(matrix.asInstanceOf[x.type & Matrix])
      case _ => None

  inline def tabulate(rows: Int, cols: Int)(inline f: (Int, Int) => Number): Matrix =
    Array.tabulate(rows, cols)(f).map(row => Vector(row))

  def apply(source: Iterable[Vector]): Matrix = source.toArray

  extension (source: Matrix)
    def update(row: Int, value: Vector): Unit =
      source(row) = value

    def apply(row: Int): Vector =
      source(row)

    def +(other: Matrix): Matrix =
      for (v, w) <- source zip other yield v + w

    def -(other: Matrix): Matrix =
      for (v, w) <- source zip other yield v - w

    def *(other: Matrix): Matrix =
      for (v, w) <- source zip other yield v * w

    def /(other: Matrix): Matrix =
      for (v, w) <- source zip other yield v / w

    def unary_- : Matrix =
      for vector <- source yield -vector

    def toArray: Array[Vector] = source
