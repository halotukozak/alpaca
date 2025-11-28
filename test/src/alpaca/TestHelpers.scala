package alpaca

import alpaca.internal.lexer.LazyReader

import java.nio.file.Files
import scala.util.Using

inline def withLazyReader[A](fileContent: String)(inline action: LazyReader => A): A =
  val tempFile = Files.createTempFile("test", ".txt")
  try
    Files.write(tempFile, fileContent.getBytes)
    Using.resource(LazyReader.from(tempFile))(action)
  finally Files.deleteIfExists(tempFile)
