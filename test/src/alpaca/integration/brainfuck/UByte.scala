package alpaca.integration.brainfuck

import scala.reflect.ClassTag

opaque type UByte = Byte

object UByte:
  given ClassTag[UByte] = ClassTag.Byte.asInstanceOf[ClassTag[UByte]]

  def apply(value: Byte): UByte = value & 0xff

  extension (self: UByte)
    def toInt: Int = self
    def toChar: Char = self.toChar
    def +(other: Byte): UByte = (self + other) & 0xff
    def -(other: Byte): UByte = (self - other) & 0xff
