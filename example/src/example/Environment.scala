package example

import example.Matrix.*
import example.Numerical.{*, given}

import scala.math.Ordered.orderingToOrdered

type Numerical = Int | Double

object Numerical:
  extension (x: Numerical)
    def +(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a + b
        case (a: Double, b: Double) => a + b
        case (a: Int, b: Double) => a + b
        case (a: Double, b: Int) => a + b

    def -(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a - b
        case (a: Double, b: Double) => a - b
        case (a: Int, b: Double) => a - b
        case (a: Double, b: Int) => a - b

    def *(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a * b
        case (a: Double, b: Double) => a * b
        case (a: Int, b: Double) => a * b
        case (a: Double, b: Int) => a * b

    def /(y: Numerical): Numerical =
      (x, y) match
        case (a: Int, b: Int) => a / b
        case (a: Double, b: Double) => a / b
        case (a: Int, b: Double) => a / b
        case (a: Double, b: Int) => a / b

  given Ordering[Numerical] with
    def compare(x: Numerical, y: Numerical): Int =
      (x, y) match
        case (a: Int, b: Int) => a.compareTo(b)
        case (a: Double, b: Double) => a.compareTo(b)
        case (a: Int, b: Double) => a.toDouble.compareTo(b)
        case (a: Double, b: Int) => a.compareTo(b.toDouble)

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
      for (v, w) <- source zip other yield Numerical.+(v)(w)

    def -(other: Vector): Vector =
      for (v, w) <- source zip other yield Numerical.-(v)(w)

    def *(other: Vector): Vector =
      for (v, w) <- source zip other yield Numerical.*(v)(w)

    def /(other: Vector): Vector =
      for (v, w) <- source zip other yield Numerical./(v)(w)

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
      Matrix(for (v, w) <- source zip other yield Vector.+(v)(w))

    def -(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip other yield Vector.-(v)(w))

    def *(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip other yield Vector.*(v)(w))

    def /(other: Matrix): Matrix =
      Matrix(for (v, w) <- source zip other yield Vector./(v)(w))

type DynamicFunction = PartialFunction[Tuple, ?]

val globalFunctions = Map[String, DynamicFunction](
  "+" -> { case (a: Numerical, b: Numerical) => a + b },
  "-" -> { case (a: Numerical, b: Numerical) => a - b },
  "*" -> {
    case (a: Numerical, b: Numerical) => a * b
    case (a: Matrix, b: Numerical) => a.map(_.map(_ * b))
    case (a: Vector, b: Numerical) => a.map(_ * b)
  },
  "/" -> { case (a: Numerical, b: Numerical) => a / b },
  "PRINT" -> { args =>
    println(
      args.toList
        .map:
          case m: Matrix => m.map(_.mkString("[", ", ", "]")).mkString("[", ", ", "]")
          case v: Vector => v.mkString("[", ", ", "]")
          case x => x
        .mkString(" "),
    )
  },
  "zeros" -> { case (n: Int) *: EmptyTuple => Matrix(Array.fill(n)(Vector(Array.fill(n)(0)))) },
  "ones" -> { case (n: Int) *: EmptyTuple => Matrix(Array.fill(n)(Vector(Array.fill(n)(1)))) },
  "eye" -> { case (n: Int) *: EmptyTuple => Matrix.tabulate(n, n)((i, j) => if i == j then 1 else 0) },
  "INIT" -> {
    case args @ (_: Numerical) *: _ => Vector(args.toList.asInstanceOf[List[Numerical]])
    case args @ (_: Vector) *: _ => Matrix(args.toList.asInstanceOf[List[Vector]])
  },
  "==" -> { case (a: Numerical, b: Numerical) => a == b },
  "!=" -> { case (a: Numerical, b: Numerical) => a != b },
  "<=" -> { case (a: Numerical, b: Numerical) => a <= b },
  ">=" -> { case (a: Numerical, b: Numerical) => a >= b },
  "<" -> { case (a: Numerical, b: Numerical) => a < b },
  ">" -> { case (a: Numerical, b: Numerical) => a > b },
  "DOTADD" -> { case (a: Matrix, b: Matrix) => a + b },
  "DOTSUB" -> { case (a: Matrix, b: Matrix) => a - b },
  "DOTMUL" -> { case (a: Matrix, b: Matrix) => a * b },
  "DOTDIV" -> { case (a: Matrix, b: Matrix) => a / b },
  "TRANSPOSE" -> { case (m: Matrix) *: EmptyTuple =>
    val rows = m.length
    val cols = m.head.length
    Matrix.tabulate(cols, rows)((i, j) => m(j)(i))
  },
)
