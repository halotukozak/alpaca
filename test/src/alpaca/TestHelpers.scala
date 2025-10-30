package alpaca

import alpaca.lexer.LazyReader
import java.nio.file.Files
import scala.util.Using

object TestHelpers:
  def withTempFile[A](fileContent: String)(action: LazyReader => A): A =
    val tempFile = Files.createTempFile("test", ".txt")
    try {
      Files.write(tempFile, fileContent.getBytes)
      Using(LazyReader.from(tempFile)) { input =>
        action(input)
      }.get
    } finally {
      Files.deleteIfExists(tempFile)
    }
