package example.runtime

import scala.reflect.TypeTest

opaque type Number = Int | Double

object Number:
  def apply(value: Int | Double): Number = value

  given TypeTest[Any, Number]:
    def unapply(x: Any): Option[x.type & Number] = x match
      case number: Int => Some(number.asInstanceOf[x.type & Number])
      case number: Double => Some(number.asInstanceOf[x.type & Number])
      case _ => None

  private val extractDouble: PartialFunction[Number, Double] =
    case x: Int => x.toDouble
    case x: Double => x

  given Numeric[Number]:
    override def plus(x: Number, y: Number): Number = (x, y).runtimeChecked match
      case (a: Int, b: Int) => a + b
      case (extractDouble(a), extractDouble(b)) => a + b

    override def minus(x: Number, y: Number): Number = (x, y).runtimeChecked match
      case (a: Int, b: Int) => a - b
      case (extractDouble(a), extractDouble(b)) => a - b

    override def times(x: Number, y: Number): Number = (x, y).runtimeChecked match
      case (a: Int, b: Int) => a * b
      case (extractDouble(a), extractDouble(b)) => a * b

    override def negate(x: Number): Number = x match
      case a: Int => -a
      case a: Double => -a

    override def fromInt(x: Int): Number = x

    override def parseString(str: String): Option[Number] =
      str.toIntOption.orElse(str.toDoubleOption)

    override def toInt(x: Number): Int = x match
      case a: Int => a
      case a: Double => a.toInt

    override def toLong(x: Number): Long = x match
      case a: Int => a.toLong
      case a: Double => a.toLong

    override def toFloat(x: Number): Float = x match
      case a: Int => a.toFloat
      case a: Double => a.toFloat

    override def toDouble(x: Number): Double = x match
      case a: Int => a.toDouble
      case a: Double => a

    override def compare(x: Number, y: Number): Int = (x, y).runtimeChecked match
      case (a: Int, b: Int) => a.compareTo(b)
      case (extractDouble(a), extractDouble(b)) => a.compareTo(b)

  extension (x: Number)
    def /(y: Number): Number = (x, y).runtimeChecked match
      case (a: Int, b: Int) => a / b
      case (extractDouble(a), extractDouble(b)) => a / b
