package example

type Numerical = Int | Float
import example.Matrix.*
import example.Vector.*

import scala.math.Ordered.orderingToOrdered
import scala.reflect.TypeTest

extension (x: Numerical)
  infix def plus(y: Numerical): Numerical =
    (x, y) match
      case (a: Int, b: Int) => a + b
      case (a: Float, b: Float) => a + b
      case (a: Int, b: Float) => a + b
      case (a: Float, b: Int) => a + b

  infix def minus(y: Numerical): Numerical =
    (x, y) match
      case (a: Int, b: Int) => a - b
      case (a: Float, b: Float) => a - b
      case (a: Int, b: Float) => a - b
      case (a: Float, b: Int) => a - b

  infix def times(y: Numerical): Numerical =
    (x, y) match
      case (a: Int, b: Int) => a * b
      case (a: Float, b: Float) => a * b
      case (a: Int, b: Float) => a * b
      case (a: Float, b: Int) => a * b

  infix def div(y: Numerical): Numerical =
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

    // def iterator: Iterator[Int | Float] =
    //   source.iterator

    def dotAdd(other: Vector): Vector =
      for (v, w) <- source zip source yield v.plus(w)

    def dotSub(other: Vector): Vector =
      for (v, w) <- source zip source yield v.minus(w)

    def dotMul(other: Vector): Vector =
      for (v, w) <- source zip source yield v.times(w)

    def dotDiv(other: Vector): Vector =
      for (v, w) <- source zip source yield v.div(w)

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
      Matrix(for (v: Vector, w: Vector) <- source zip source yield v.dotAdd(w))

    def -(other: Matrix): Matrix =
      Matrix(for (v: Vector, w: Vector) <- source zip source yield v.dotSub(w))

    def *(other: Matrix): Matrix =
      Matrix(for (v: Vector, w: Vector) <- source zip source yield v.dotMul(w))

    def /(other: Matrix): Matrix =
      Matrix(for (v: Vector, w: Vector) <- source zip source yield v.dotDiv(w))

type DynamicFunction = PartialFunction[Seq[?], ?]

val globalFunctions = Map[String, DynamicFunction](
  "+" -> { case Seq(a: Numerical, b: Numerical) => a.plus(b) },
  "-" -> { case Seq(a: Numerical, b: Numerical) => a.minus(b) },
  "*" -> { case Seq(a: Numerical, b: Numerical) => a.times(b) },
  "/" -> { case Seq(a: Numerical, b: Numerical) => a.div(b) },
  "PRINT" -> { args => println(args.mkString(" ")) },
  "ZEROS" -> { case Seq(n: Int) => Matrix(Array.fill(n)(Vector(Array.fill(n)(0)))) },
  "ONES" -> { case Seq(n: Int) => Matrix(Array.fill(n)(Vector(Array.fill(n)(1)))) },
  "EYE" -> { case Seq(n: Int) => Matrix.tabulate(n, n)((i, j) => if i == j then 1 else 0) },
  "INIT" -> { args =>
    args.head match
      case n: Numerical => Vector(args.asInstanceOf[Iterable[Numerical]])
      case _ => Matrix(args.asInstanceOf[Iterable[Vector]])
  },
  "==" -> { case Seq(a: Numerical, b: Numerical) => a == b },
  "!=" -> { case Seq(a: Numerical, b: Numerical) => a != b },
  "<=" -> { case Seq(a: Numerical, b: Numerical) => a <= b },
  ">=" -> { case Seq(a: Numerical, b: Numerical) => a >= b },
  "<" -> { case Seq(a: Numerical, b: Numerical) => a < b },
  ">" -> { case Seq(a: Numerical, b: Numerical) => a > b },
  "DOTADD" -> { case Seq(a: Matrix, b: Matrix) => a + b },
  "DOTSUBB" -> { case Seq(a: Matrix, b: Matrix) => a - b },
  "DOTMUL" -> { case Seq(a: Matrix, b: Matrix) => a * b },
  "DOTDIV" -> { case Seq(a: Matrix, b: Matrix) => a / b },
  "TRANSPOSE" -> { case Seq(m: Matrix) =>
    val rows = m.length
    val cols = m.head.length
    Matrix.tabulate(cols, rows)((i, j) => m(j)(i))
  },
)
