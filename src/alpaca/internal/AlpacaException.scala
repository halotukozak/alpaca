package alpaca
package internal

import scala.util.control.NoStackTrace

import alpaca.internal.Showable.given_Conversion_String_Shown

/**
 * Base exception class for all Alpaca-specific errors.
 *
 * This abstract class extends Exception and adds NoStackTrace to improve
 * error reporting. All Alpaca-specific exceptions should extend this class.
 *
 * @param message the error message
 */
abstract class AlpacaException(message: Shown) extends Exception(message) with NoStackTrace

/**
 * Exception thrown when Alpaca macro compilation takes too long.
 *
 * This exception is raised when the compilation timeout configured in
 * DebugSettings is exceeded during macro expansion.
 */
object AlpacaTimeoutException extends AlpacaException("Alpaca compilation timeout")
