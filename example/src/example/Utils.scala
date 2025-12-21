package example

import scala.util.control.NoStackTrace

sealed abstract class MatrixException(message: String, line: Int)
  extends Throwable(s"$message at line $line") with NoStackTrace

class MatrixCompilerException(message: String, line: Int) extends MatrixException(message, line)
class MatrixRuntimeException(message: String, line: Int) extends MatrixException(message, line)
