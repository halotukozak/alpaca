package example

import example.Numerical.{*, given}
import example.Matrix.*
import example.Vector.*

import scala.math.Ordered.orderingToOrdered
import scala.reflect.TypeTest

type Numerical = Int | Float

object Numerical:
  extension (x: Numerical)
    def +(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a + b
        case (a: Float, b: Float) => a + b
        case (a: Int, b: Float) => a + b
        case (a: Float, b: Int) => a + b

    def -(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a - b
        case (a: Float, b: Float) => a - b
        case (a: Int, b: Float) => a - b
        case (a: Float, b: Int) => a - b

    def *(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a * b
        case (a: Float, b: Float) => a * b
        case (a: Int, b: Float) => a * b
        case (a: Float, b: Int) => a * b

    def /(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a / b
        case (a: Float, b: Float) => a / b
        case (a: Int, b: Float) => a / b
        case (a: Float, b: Int) => a / b

  given Ordering[Numerical] with
    def compare(x: Numerical, y: Numerical): Int =
      (x, y) match
        case (a: Int, b: Int) => a.compareTo(b)
        case (a: Float, b: Float) => a.compareTo(b)
        case (a: Int, b: Float) => a.toFloat.compareTo(b)
        case (a: Float, b: Int) => a.compareTo(b.toFloat)

opaque type Vector = Array[Numerical]

object Vector:
  def apply(source: Array[Numerical]): Vector =
    source

  def apply(source: Iterable[Numerical]): Vector =
    source.toArray

  extension (source: Vector)
    def update(index: Int, value: Numerical): Unit =
      source(index) = value

    def apply(index: Int): Numerical =
      source(index)

    def +(other: Vector): Vector =
      for (v, w) <- source zip source yield Numerical.+(v)(w)

    def -(other: Vector): Vector =
      for (v, w) <- source zip source yield Numerical.-(v)(w)

    def *(other: Vector): Vector =
      for (v, w) <- source zip source yield Numerical.*(v)(w)

    def /(other: Vector): Vector =
      for (v, w) <- source zip source yield Numerical./(v)(w)

opaque type Matrix = Array[Vector]

object Matrix:
  def apply(source: Array[Vector]): Matrix = source

  def tabulate(rows: Int, cols: Int)(f: (Int, Int) => Numerical): Matrix =
    Array.tabulate(rows, cols)(f).map(row => Vector(row))

  def apply(source: Iterable[Vector]): Matrix = source.toArray

  extension (source: Matrix)
    def update(row: Int, value: Vector): Unit =
      source(row) = value

    def apply(row: Int): Vector =
      source(row)

    def +(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip source yield Vector.+(v)(w))

    def -(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip source yield Vector.-(v)(w))

    def *(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip source yield Vector.*(v)(w))

    def /(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip source yield Vector./(v)(w))

type DynamicFunction = PartialFunction[Tuple, ?]

val globalFunctions = Map[String, DynamicFunction](
  "+" -> { case (a: Numerical, b: Numerical) => a + b },
  "-" -> { case (a: Numerical, b: Numerical) => a - b },
  "*" -> { case (a: Numerical, b: Numerical) => a * b },
  "/" -> { case (a: Numerical, b: Numerical) => a / b },
  "PRINT" -> { args => println(args.toList.mkString(" ")) },
  "ZEROS" -> { case (n: Int) *: EmptyTuple => Matrix(Array.fill(n)(Vector(Array.fill(n)(0)))) },
  "ONES" -> { case (n: Int) *: EmptyTuple => Matrix(Array.fill(n)(Vector(Array.fill(n)(1)))) },
  "EYE" -> { case (n: Int) *: EmptyTuple => Matrix.tabulate(n, n)((i, j) => if i == j then 1 else 0) },
  "INIT" -> { args =>
    args.head match
      case n: Numerical => Vector(args.asInstanceOf[Iterable[Numerical]])
      case _ => Matrix(args.asInstanceOf[Iterable[Vector]])
  },
  "==" -> { case (a: Numerical, b: Numerical) => a == b },
  "!=" -> { case (a: Numerical, b: Numerical) => a != b },
  "<=" -> { case (a: Numerical, b: Numerical) => a <= b },
  ">=" -> { case (a: Numerical, b: Numerical) => a >= b },
  "<" -> { case (a: Numerical, b: Numerical) => a < b },
  ">" -> { case (a: Numerical, b: Numerical) => a > b },
  "DOTADD" -> { case (a: Matrix, b: Matrix) => a + b },
  "DOTSUBB" -> { case (a: Matrix, b: Matrix) => a - b },
  "DOTMUL" -> { case (a: Matrix, b: Matrix) => a * b },
  "DOTDIV" -> { case (a: Matrix, b: Matrix) => a / b },
  "TRANSPOSE" -> { case (m: Matrix) *: EmptyTuple =>
    val rows = m.length
    val cols = m.head.length
    Matrix.tabulate(cols, rows)((i, j) => m(j)(i))
  },
)
