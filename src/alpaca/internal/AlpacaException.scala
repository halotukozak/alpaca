package alpaca
package internal

import scala.annotation.constructorOnly
import scala.util.control.NoStackTrace

/**
 * Base exception class for all Alpaca-specific errors.
 *
 * This abstract class extends Exception and adds NoStackTrace to improve
 * error reporting. All Alpaca-specific exceptions should extend this class.
 *
 * @param message the error message
 */
private[alpaca] abstract class AlpacaException(message: Shown) extends Exception(message) with NoStackTrace

/**
 * Exception thrown when Alpaca macro compilation takes too long.
 *
 * This exception is raised when the compilation timeout configured in
 * DebugSettings is exceeded during macro expansion.
 */
private[alpaca] class AlpacaTimeoutException()(using @constructorOnly log: Log)
  extends AlpacaException("Alpaca compilation timeout")
