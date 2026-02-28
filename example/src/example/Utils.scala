package example

import scala.compiletime.ops.int.S
import scala.util.control.NoStackTrace

sealed abstract class MatrixException(message: String, line: Int)
  extends Throwable(s"$message at line $line") with NoStackTrace

class MatrixCompilerException(message: String, line: Int) extends MatrixException(message, line)

class MatrixRuntimeException(message: String, line: Int) extends MatrixException(message, line)

type TupleFactory[N <: scala.Int, X] <: Tuple = N match
  case 0 => EmptyTuple
  case S[n] => X *: TupleFactory[n, X]
